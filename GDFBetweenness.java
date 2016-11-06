/*
 * Code prompts user for GDF file and coverts that to a JGraphT object
 * then runs a Johnson clustering algorithm on it. The algorithm only
 * works on sparse graphs with non-negative edge weights. The Johnson
 * algorithm consists of running Djikstra's on every node, and therefore
 * is parallelizable. The code will prompt for the number of processes
 * (which don't have to all be on the same computer), and for the respective
 * one to run. For example, if there are 128 nodes, 8 processes, and
 * you tell the program to run the 3rd process, it will run Djikstra's
 * on nodes 32-47 (zero-based numbering). The Graph library used doesn't allow
 * edges to have the same source and destination node, so centralities are
 * calculated as if these don't exist
 */
package gdfbetweenness;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import static org.jgrapht.Graphs.getOppositeVertex;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 *
 * @author nightduck
 */
public class GDFBetweenness {

    public static class AttribNode implements Comparable {

        long name;
        AttribNode parent;
        double distance;
        int centrality;

//        double x;  //EDITABLE add variables here if you need attributes in your calculations
//        double y;
        String[] attrib;

        //EDITABLE: Feel free to modify the constructor, but leave the attrib variable
        public AttribNode(long name, String[] attrib) {
            this.name = name;
            this.attrib = attrib;
            this.centrality = 0;
        }

        //This is needed for the priority queue that picks the node closest to
        //the source when running Djikstra's
        public int compareTo(Object n) {
            return Double.compare(this.distance, ((AttribNode) n).distance);
        }
    }

    public static class AttribEdge extends DefaultWeightedEdge {

        String line;

        public AttribEdge() {
            super();
        }

        public AttribEdge(String line) {
            super();
            this.line = line;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.print("Enter gdf filename: ");
        String filename = scan.nextLine();
        System.out.print("How many processes are used? ");
        int procTotal = scan.nextInt();
        System.out.print("And which am I (starting from 0)? ");
        int procNum = scan.nextInt();
        scan.nextLine();
        System.out.print("Specify output file: ");
        String outfilename = scan.nextLine();
        //NOTE: If you want each instance of the program to launch it's own threads
        //  ask for another variable here

        File file = new File(filename);

        SimpleWeightedGraph<AttribNode, AttribEdge> g = importGDF(file);

        johnsons(g, procNum, procTotal);

        try {
            outputGDF(file, outfilename, g); //Format output file in manner of input file
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("IOException-filename: " + outfilename);
            ex.printStackTrace();
        }
    }

    /*
        NOTE: This method is designed for a GDF file using specific
        node and edge templates. Edit the marked lines to adjust to your own
        format. Apologies for the lack of plug and play. Ctrl+F for EDITABLE to find
        editable lines to adjust to your format
     */
    public static SimpleWeightedGraph<AttribNode, AttribEdge> importGDF(File f) {
        BufferedReader reader = null;
        SimpleWeightedGraph<AttribNode, AttribEdge> g = new SimpleWeightedGraph(new ClassBasedEdgeFactory(AttribEdge.class));
        HashMap<Long, AttribNode> dict = new HashMap();

        try {
            reader = new BufferedReader(new FileReader(f));
            //Reader first line and look for weight and directed values to 
            //determine what to assign g to.
            String[] nodeTemplate = reader.readLine().substring(8).split(","); //
            String line = reader.readLine();
            while (line.substring(0, 8).compareTo("edgedef>") != 0) {
                //System.out.println(line);
                String[] values = line.split(",");

                //EDITABLE: Create a node using the first value in the list as a name, and store the others values
                AttribNode node = new AttribNode(Long.parseLong(values[0]), values);
                line = reader.readLine();

                //EDITABLE: You can change 0 to an appropriate number, but the dictionary is essential
                dict.put(Long.parseLong(values[0]), node);
                g.addVertex(node);
            }

            String[] edgeTemplate = line.substring(8).split(",");

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");

                //EDITABLE: In my format, the id of node1 is the first in the list. You can change accordingly
                AttribNode n1 = dict.get(Long.parseLong(values[0]));
                //EDITABLE: In my format, the id of node2 is the second in the list. You can change accordingly
                AttribNode n2 = dict.get(Long.parseLong(values[1]));
                //EDITABLE: In my format, the weight of the edge is the third in the list. You can change accordingly
                double weight = Float.parseFloat(values[2]);

                //Pass it to the graph and let it do its thang
                AttribEdge e = new AttribEdge(line);
                try {
                    g.addEdge(n1, n2, e);
                } catch (IllegalArgumentException err) {
                    if (n1 != n2) {
                        System.out.println("n1: " + n1.name);
                        System.out.println("n2: " + n2.name);
                        err.printStackTrace();
                        System.exit(1);
                    }
                }

                //Passing the values reference again along with the weight and the JGraphT API will add that in
                g.setEdgeWeight(e, weight);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return g;
    }

    static void johnsons(SimpleWeightedGraph g, int proc, int total) {
        int i = 0;
        for (Object node : g.vertexSet()) {
            //If there's 32 processes and this is the 7th process, perform
            //Djisktra's on every 32nd node starting with the 7th
            //NOTE: If you want to add threads, here's the place to do it.
            if (i == proc) {
                //Djikstra's on node n

                PriorityQueue<AttribNode> q = new PriorityQueue();

                //Initialization
                for (Object n : g.vertexSet()) {
                    ((AttribNode) n).distance = Double.POSITIVE_INFINITY;
                    ((AttribNode) n).parent = null;
                }

                //Add root node
                ((AttribNode) node).distance = 0;
                q.add((AttribNode) node);

                while (q.size() > 0) {
                    AttribNode u = q.remove();

                    for (Object e : g.edgesOf(u)) {
                        AttribEdge edge = (AttribEdge) e;
                        AttribNode neighbor = getOppositeVertex(g, edge, u);
                        
                        double alt = g.getEdgeWeight(e) + u.distance;

                        if (alt < neighbor.distance) {
                            neighbor.distance = alt;
                            neighbor.parent = u;
                            q.add(neighbor);
                        }
                    }

                }

                //Traverse each path and add 1 to centrality of every node passed through
                for (Object v : g.vertexSet()) {
                    AttribNode x = (AttribNode) v;
                    if (x.parent != null) {
                        x.centrality++;
                    }
                    while (x.parent != null) {
                        x = x.parent;
                        x.centrality++;
                    }
                }
            }

            i = (i + 1) % total;
        }
    }

    static void outputGDF(File infile, String outname, SimpleWeightedGraph g) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        File fout = new File(outname);
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(fout));
        Writer out = new BufferedWriter(osw);
//        PrintWriter out = new PrintWriter(outname, "UTF-8");
        BufferedReader in = new BufferedReader(new FileReader(infile));

        for (Object n : g.vertexSet()) {
            out.write(((AttribNode) n).attrib[0] + ",");
            out.write(Integer.toString(((AttribNode) n).centrality));
            out.write("\n");
        }
        out.close();
    }
}

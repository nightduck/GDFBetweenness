/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package consolidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author nightduck
 */
public class Consolidation {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        Scanner scan = new Scanner(System.in);
        System.out.print("Enter original file: ");
        String filename = scan.nextLine();
        
        System.out.print("How many .cen files? ");
        int count = scan.nextInt();
        scan.nextLine();
        
        
        String[] cenfiles = new String[count];
        System.out.print("Read the default 0.cen, 1.cen, etc? (y)");
        if(scan.next().charAt(0) != 'y') {
            //Have the user specify the .cen files
            for(int i = 0; i < count; i++) {
                System.out.print(".cen file " + i + ": ");
                cenfiles[i] = scan.nextLine();
            }
        } else {
            for(int i = 0; i < count; i++) {
                //Use deafults
                cenfiles[i] = i + ".cen";
            }
        }
        
        BufferedReader fin = new BufferedReader(new FileReader(new File(filename)));
        int vertexCount = 0;
        fin.readLine();   //Read in the node template
        String line = fin.readLine();
        HashMap<Long, Long> dict = new HashMap();
        
        while(!line.substring(0, 8).equals("edgedef>")) {
            //Add each node to dictionary referenced by name at beginning of line
            dict.put(Long.parseLong(line.split(",")[0]), (long)0);
            vertexCount++;
            line = fin.readLine();
        }
        
        for(String file : cenfiles) {
            //For each .cen file
            BufferedReader cen = new BufferedReader(new FileReader(new File(file)));
            while((line = cen.readLine()) != null) {
                //Each line. Add to appropriate dicitonary entry
                long name = Long.parseLong(line.split(",")[0]);
                long cenCount = Long.parseLong(line.split(",")[1]);
                
                //Increment centrality and put it back in the dictionary
                dict.put(name, dict.get(name) + cenCount);
            }
        }
        
        fin.close();
        
        System.out.print("Specify output file: ");
        scan.nextLine();
        PrintWriter fout =  new PrintWriter(new File(scan.nextLine()));
        
        fin = new BufferedReader(new FileReader(new File(filename)));
        line = fin.readLine();          //Read node template
        line += ",centrality DOUBLE";  //Append extra information
        fout.write(line + "\n");
        
        //Read each node, append centrality information, and append to output file
        line = fin.readLine();
        while(!line.substring(0,8).equals("edgedef>")) {
            long name = Long.parseLong(line.split(",")[0]);
            fout.write(line + "," + String.valueOf((double)dict.get(name)/(2*vertexCount*(vertexCount-1))) + "\n");
            line = fin.readLine();
        }
        //Copy edge template and all edge lines to output file intact
        while(line != null) {
            fout.write(line + "\n");
            line = fin.readLine();
        }
        
        fin.close();
        fout.close();
    }
}

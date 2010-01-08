/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bioviz.protannot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 *
 * @author hvora1
 */
public class TestSamples {

    static public void main(String args[])
    {
        String dirpath = "/afs/transvar.org/home/hvora1/src/protannot_python/samples/";
        File dir = new File(dirpath);
        String[] files = dir.list();
        System.out.println("Total files " + files.length);
        for(String s : files)
        {
            if(testFile(dirpath+s))
                System.out.println(s + "read sucessfully");
            else
                System.out.println("Error reading " + s);
        }
        
    }

    static private boolean testFile(String filename)
    {
        BufferedInputStream bistr;
        try {
            bistr = new BufferedInputStream(new FileInputStream(filename));
            Xml2GenometryParser parser = new Xml2GenometryParser();
            if(parser.parse(bistr) != null)
                return true;
        } catch (FileNotFoundException ex) {
            System.out.println(filename + "File not found");
        }
        return false;
    }
}

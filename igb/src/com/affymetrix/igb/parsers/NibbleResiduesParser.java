/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.zip.*;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.NibbleBioSeq;

import com.affymetrix.genoviz.util.Timer;

import com.affymetrix.igb.util.NibbleIterator;

public class NibbleResiduesParser {

  /**
   *  Parses an input stream.
   */
  public static NibbleBioSeq parse(InputStream istr, NibbleBioSeq input_seq) throws IOException {
    NibbleBioSeq result_seq = null;
    DataInputStream dis = null;
    try {
      Timer tim = new Timer();
      tim.start();
      BufferedInputStream bis;
      if (istr instanceof BufferedInputStream) {
	bis = (BufferedInputStream)istr;
      }
      else {
	bis = new BufferedInputStream(istr);
      }
      dis = new DataInputStream(bis);
      String name = dis.readUTF();
      //      System.out.println("name: " + name);
      String version = dis.readUTF();
      //      System.out.println("version: " + version);
      int num_residues = dis.readInt();
      //      System.out.println("length: " + num_residues);
      byte[] nibble_array;

      if (input_seq != null && name.equals(input_seq.getID())) {
	result_seq = input_seq;
      }
      else {
        result_seq = new NibbleBioSeq(name, version, num_residues);
      }

      System.out.println("NibbleBioSeq: " + result_seq);

      if ((num_residues % 2) == 0)  {
	nibble_array = new byte[num_residues / 2];
      }
      else {
	nibble_array = new byte[(num_residues / 2) + 1];
      }
      dis.readFully(nibble_array);
      System.out.println("nibble array length: " + nibble_array.length);

      NibbleIterator residues_provider = new NibbleIterator(nibble_array, num_residues);
      result_seq.setResiduesProvider(residues_provider);

      dis.close();
      float read_time = tim.read()/1000f;
      System.out.println("time to read in bnib residues file: " + read_time);
    }
    finally {
      if (dis != null) try {dis.close();} catch(Exception e) {}
    }
    return result_seq;
  }

  public static NibbleBioSeq readBinaryFile(String file_name) throws IOException {
    FileInputStream fis = null;
    NibbleBioSeq seq = null;
    try {
      File fil = new File(file_name);
      fis = new FileInputStream(fil);
      seq = parse(fis, null);
    }
    finally {
      if (fis != null) try {fis.close();} catch(Exception e) {}
    }
    return seq;
  }

  public static void writeBinaryFile(String file_name, String seqname, String seqversion,
				     String residues) throws IOException {
    // binary DNA residues format
    // header:
    //   UTF8-encoded sequence name
    //   UTF8-encoded sequence version
    //   length of sequence as 4-byte int
    //   residues encoded as "nibbles" (4 bits per residue ==> 2 bases / byte)
    //      4-bit residue encoding maps to 16 possible IUPAC codes:
    //     [ A, C, G, T, N, M, R, W, S, Y, K, V, H, D, B, X ]

    DataOutputStream dos = null;
    try {
      File fil = new File(file_name);
      FileOutputStream fos = new FileOutputStream(fil);
      BufferedOutputStream bos = new BufferedOutputStream(fos);
      dos = new DataOutputStream(bos);
      dos.writeUTF(seqname);
      dos.writeUTF(seqversion);
      dos.writeInt(residues.length());
      System.out.println("creating nibble array");
      //      byte[] nibble_array = NibbleBioSeq.stringToNibbles(residues, 0, residues.length());
      byte[] nibble_array = NibbleIterator.stringToNibbles(residues, 0, residues.length());

      System.out.println("done creating nibble array, now writing nibble array out");
      // hit problem with trying to output full nibble_array at once for large
      // (>100 Mb) sequences.  I would have thought the BufferedOutputStream would
      // handle this, but maybe it just passes through calls that write large amounts...
      //
      // anyway, trying to work around this by looping over the array and writing
      // smaller chunks
      int chunk_length = 65536;
      if (nibble_array.length > chunk_length) {
	int bytepos = 0;
	while (bytepos < (nibble_array.length - chunk_length)) {
	  dos.write(nibble_array, bytepos, chunk_length);
	  bytepos += chunk_length;
	}
	dos.write(nibble_array, bytepos, nibble_array.length - bytepos);
      }
      else {
	dos.write(nibble_array);
      }
      dos.flush();
      dos.close();
      System.out.println("done writing out nibble file");
    }
    finally {
      if (dos != null) try {dos.close();} catch(Exception e) {}
    }
  }

  /** Reads a FASTA file and outputs a binary sequence file. 
   *  @param args  sequence name, input file, output file, sequence version
   */
  public static void main(String[] args) {
    try {
      String infile_name = null;
      String outfile_name = null;
      String seq_version = null;
      String seq_name = null;
      if (args.length >= 4) {
	seq_name = args[0];
	infile_name = args[1];
	outfile_name = args[2];
        seq_version = args[3];
      }
      else {
        System.err.println("Usage: java com.affymetrix.igb.parsers.NibbleResiduesParser [seq_name] [in_file] [out_file] [seq_version]");
        System.exit(1);
      }
      File fil = new File(infile_name);
      StringBuffer sb = new StringBuffer();
      InputStream isr = Streamer.getInputStream(fil, sb);
      
      FastaParser fastparser = new FastaParser();
      // if file is gzipped or zipped, then fil.length() will not really be the max_seq_length,
      //   but that's okay because the parse() method only uses it as a suggestion
      MutableAnnotatedBioSeq seq = fastparser.parse(isr, null, (int)(fil.length()));
      int seqlength = seq.getResidues().length();
      System.out.println("length: " + seqlength);

      writeBinaryFile(outfile_name, seq_name, seq_version, seq.getResidues());
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}


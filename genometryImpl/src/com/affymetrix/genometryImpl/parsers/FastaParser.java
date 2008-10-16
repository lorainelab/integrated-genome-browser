/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.SimpleAnnotatedBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.Memer;
import com.affymetrix.genometryImpl.util.SynonymLookup;

/**
 *  Parses a fasta-formatted file.
 *  The default parse() method only loads the first sequence in a fasta file.
 *  If there are multiple sequences in the file, ignores the rest.
 *  The parseAll() method will load all sequences listed in the file.
 */
public class FastaParser {

  //  int max_residues = 32000000;
  boolean use_buffer_directly = false;
  boolean fixed_length_buffer = false;
  static final Pattern header_regex = Pattern.compile("^\\s*>(.+)");

  public FastaParser() {
  }

  /**
   * Parses an input stream which can contain one or more sequences in FASTA format.
   * Will merge the sequences with the given group.
   * (When necessary, new sequences will be added to the existing group; otherwise
   * sequence data will be stored in the existing, synonymous BioSeq objects.)
   * Returns the List of sequences that were read from the file, which will be
   * a subset of the sequences in the group.
   */
  public List<BioSeq> parseAll(InputStream istr, AnnotatedSeqGroup group) throws IOException {
    ArrayList<BioSeq> seqlist = new ArrayList<BioSeq>();
    //int line_count = 0;
    BufferedReader br = null;
    Matcher matcher = header_regex.matcher("");
    try {
      br = new BufferedReader(new InputStreamReader(istr));
      String header = br.readLine();
      while (br.ready()) {  // loop through lines till find a header line
        if (header == null) { continue; }  // skip null lines
        matcher.reset(header);
        boolean matched = matcher.matches();

        if (matched) {
          StringBuffer buf = new StringBuffer();
          String seqid = matcher.group(1);
          while (br.ready()) {
            String line = br.readLine();
            if (line == null) { continue; }  // skip null lines

            if (line.charAt(0) == ';') { continue; } // skip comment lines

            // break if hit header for another sequence --
            if (line.startsWith(">")) {
              header = line;
              break;
            }

            buf.append(line);
          }
          String residues = buf.toString();
          MutableAnnotatedBioSeq seq = group.getSeq(seqid);
          if (seq == null) {
            seq = group.addSeq(seqid, residues.length());
          }
          seq.setResidues(residues);

          seqlist.add(seq);
          System.out.println("length of sequence: " + residues.length());
        }
      }
    } finally {
      if (br != null) {try {br.close();} catch (IOException ioe) {}}
      if (istr !=null) {try {istr.close();} catch (IOException ioe) {}}
    }
    System.out.println("done loading fasta file");
    return seqlist;
  }

  /**
   *  Parse an input stream, creating a single new BioSeq.
   *  @param istr an InputStream that will be read and then closed
   */
  public MutableAnnotatedBioSeq parse(InputStream istr) throws IOException {
    return parse(istr, null);
  }


  /**
   *  Parse an input stream into a BioSeq.
   *  @param istr an InputStream that will be read and then closed
   *  @param aseq Usually null, but can be an existing seq that you want to load the
   *   residues into.  If not null, then the sequence in the file must have a name
   *   that is synonymous with aseq.
   */
  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq) {
    return parse(istr, aseq, -1);
  }

  /**
   *  Old parsing method.
   *  trying to optimize for case where number of residues is known, so can
   *  pre-allocate length of StringBuffer's internal char array, and then use
   *  the StringBuffer.toString() method to get residues without accidentally
   *  caching an array bigger than needed (see comments in method for more details...)
   *  @param istr an InputStream that will be read and then closed
   *  @param aseq Usually null, but can be an existing seq that you want to load the
   *   residues into.  If not null, then the sequence in the file must have a name
   *   that is synonymous with aseq.
   */
  public MutableAnnotatedBioSeq oldparse(InputStream istr, MutableAnnotatedBioSeq aseq,
                                      int max_seq_length) {
    if (max_seq_length > 0) {
      fixed_length_buffer = true;
      use_buffer_directly = true;
    }
    else {
      fixed_length_buffer = false;
      use_buffer_directly = false;
    }
    System.out.println("using buffer directly: " + use_buffer_directly);
    System.out.println("using fixed length buffer: " + fixed_length_buffer);

        com.affymetrix.genometryImpl.util.Timer tim = new com.affymetrix.genometryImpl.util.Timer();
    tim.start();
    MutableAnnotatedBioSeq seq = aseq;
    String seqid = ("unknown");
    // maybe guesstimate size of buffer needed based on file size???
    StringBuffer buf;
    if (fixed_length_buffer) {
      //      buf = new StringBuffer(max_residues);
      buf = new StringBuffer(max_seq_length);
    }
    else {
      buf = new StringBuffer();
    }
    Memer mem = new Memer();

    Matcher matcher = header_regex.matcher("");
    int line_count = 0;
    BufferedReader br = null;
    try {
      //      System.out.println("trying to read");
      br = new BufferedReader(new InputStreamReader(istr));
      while (br.ready()) {  // loop through lines till find a header line
        String header = br.readLine();
        if (header == null) { continue; }  // skip null lines
        matcher.reset(header);
        boolean matched = matcher.matches();
        //        if (! matched) { continue; }
        if (matched) {
          seqid = matcher.group(1);
          break;
        }
      }
      while (br.ready()) {
        String line = br.readLine();
        if (line == null) { continue; }  // skip null lines

        if (line.startsWith(";")) { continue; } // lines beginning with ";" are comments
        // see http://en.wikipedia.org/wiki/Fasta_format

        // end loop if hit header for another sequence --
        //   currently only parsing first sequence in fasta file
        if (line.startsWith(">")) {
          break;
        }
        //        buf.append(line.substring(0, line.length()-1));
        buf.append(line);
        line_count++;
        /*
        if (line_count % 100000 == 0) {
          System.out.println("line count: " + line_count);
          mem.printMemory();
        }
        */
          //            System.out.println("line count: " + line_count);
        /*
        boolean matched = seq_regex.match(line);
        if (matched) {
          String line_residues = seq_regex.getParen(1);
          //  System.out.println("^^^" + line_residues + "$$$");
          buf.append(line_residues);
          line_count++;
          if (line_count % 10000 == 0) {
            //            System.out.println("line count: " + line_count);
          }
        }
        */
      }
      //      System.out.println("Read entire sequence, length = " + buf.length());
      br.close();
      istr.close();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (br != null) {try {br.close();} catch (IOException ioe) {}}
      if (istr != null) {try {istr.close();} catch (IOException ioe2) {}}
    }
    System.gc();
    //    try  { Thread.currentThread().sleep(1000); } catch (Exception ex) { }

    // GAH 6-26-2002
    // contortions to try and minimize length of String's internal char array, because:
    //   if just do res = buf.toString(), or res = new String(buf),
    //     then String will point to StringBuffer's
    //     current internal array, which may be up to twice as big as actually needed to
    //     hold residues (see String(StringBuffer) constructor, and how StringBuffer
    //     stretches it's internal array in StringBuffer.expandCapacity()).  And even if
    //     StringBuffer changes later, it's the StringBuffer that creates a new array -- String
    //     still points to the old array
    //   this does take more time (and potentially more peak memory), since have to first
    //     copy buffer's characters to char array, and then the String(char[]) constructor will
    //     copy the charray's charatcters to it's own internal array.  BUT, will decrease
    //     long-term memory usage
    //   trying to force garbage collection via System.gc() to minimize memory spike while
    //      doing the double-copy
    String residues = null;
    if (use_buffer_directly) {
      //      System.out.println("using buffer directly");
      System.gc();
      residues = new String(buf);
      printMemory();
    }
    else {
      //      System.out.println("constructing residues via array");
      printMemory();
      // trying new strategy with String(String) constructor
      // looks weird, but not as convoluted as other technique
      //  use String(String) constructor, whose side effect is to trim new String's
      //  internal char array -- then can garbage collect temp_residues later,
      //  and hopefully will save space...
      String temp_residues = new String(buf);
      residues = new String(temp_residues);
      printMemory();
      temp_residues = null;
      /*
      int length = buf.length();
      //    StringBuffer newbuf = new StringBuffer(length);
      char[] charray = new char[length];
      buf.getChars(0, length, charray, 0);
      printMemory();
      buf = null;
      System.gc();
      //      try  { Thread.currentThread().sleep(1000); } catch (Exception ex) { }
      printMemory();
      residues = new String(charray);
      charray = null;
      printMemory();
      */
      System.gc();
      //      try  { Thread.currentThread().sleep(1000); } catch (Exception ex) { }
      printMemory();
      System.out.println("done constructing residues via array");
      buf = null;
    }
    System.gc();
    //    try  { Thread.currentThread().sleep(1000); } catch (Exception ex) { }

    // old way:
    //    residues = buf.toString();
    //    buf = null;
    System.out.println("id: " + seqid);
    //    System.out.println("residues: " + residues.length());
    if (seq == null) {
      seq = new SimpleAnnotatedBioSeq(seqid, residues);
      //      seq = new SimpleAnnotatedBioSeq(seqid, residues.length());
      //      seq = new SimpleAnnotatedBioSeq(seqid, 31234567);
    }
    else {  // try to merge with existing seq
      if (SynonymLookup.getDefaultLookup().isSynonym(seq.getID(), seqid)) {
          seq.setResidues(residues);
      }
      else {
        System.out.println("*****  ABORTING MERGE, sequence ids don't match: " +
                           "old seq id = " + seq.getID() + ", new seq id = " + seqid);
      }
    }
    System.out.println("time to execute: " + tim.read()/1000f);
    mem.printMemory();
    System.out.println("done loading fasta file");
    //    System.out.println(residues.substring(0, 1000));
    System.out.println("length of sequence: " + residues.length());
    return seq;
  }

  // to help eliminate memory spike (by dynamic reallocation of memory in StringBuffer -- don't ask...)
  // give upper limit to sequence length, based on file size -- this will be an overestimate (due to
  //   white space, name header, etc.), but probably no more than 10% greater than actual size, which
  //   is a lot better than aforementioned memory spike, which can temporarily double the amount of
  //   memory needed
  public MutableAnnotatedBioSeq parse(InputStream istr, MutableAnnotatedBioSeq aseq,
                                      int max_seq_length) {
    return oldparse(istr, aseq, max_seq_length);
  }

  /**
   *  trying a new strategy to speed parsing.
   */
  public MutableAnnotatedBioSeq newparse(InputStream istr, MutableAnnotatedBioSeq aseq,
                                      int max_seq_length) {
    StringBuffer buf;
    if (max_seq_length > 0) {
      buf = new StringBuffer(max_seq_length);
    }
    else {
      buf = new StringBuffer();
    }
    //    StringBuffer buf = new StringBuffer(10);
        com.affymetrix.genometryImpl.util.Timer tim = new com.affymetrix.genometryImpl.util.Timer();
    tim.start();
    Memer mem = new Memer();
    //int line_count = 0;
    MutableAnnotatedBioSeq seq = aseq;
    String seqid = "unknown";

    //    byte[] bytebuf = new byte[50];
    // with bytebuf_size = bisbuf_size = 2048 --> 0.81 seconds to read chr21.fa (45.5 Mb),
    //     or 0.35 seconds with no StringBuffer memory allocation...

    //    int bytebuf_size = 2048;
    //    int bytebuf_size = 4096;
    //    int bytebuf_size = 8192;
    int bytebuf_size = 16384;
    //    int bisbuf_size = 2048;
    //    int bisbuf_size = 4096;
    //    int bisbuf_size = 8192;
    int bisbuf_size = 16384;

    byte[] bytebuf = new byte[bytebuf_size];

    int bytecount = 0;
    int milcount = 0;
    /*
    try {
      BufferedInputStream bis = new BufferedInputStream(istr, bisbuf_size);
      //      for (int i= 0; i<max_seq_length; i++) {
      //              int b = bis.read();
      //      }
      for (int i= 0; i<max_seq_length-bytebuf_size; i+=bytebuf_size) {
        //      for (int i= 0; i<max_seq_length; i+=bytebuf_size) {
              bytecount += bis.read(bytebuf);
        for (int k=0; k<bytebuf.length; k++) {
          buf.append((char)bytebuf[k]);
        }
        //        System.out.println("buf length = " + buf.length());
        //        System.out.println("buf: " + buf.substring(0, 100));
      }
      System.out.println("bytes read: " + bytecount);
    }
    */
    BufferedReader br = null;
    try {
      int charcount = 0;
      char[] charbuf = new char[bytebuf_size];
      br = new BufferedReader(new InputStreamReader(istr));
      for (int i=0; i<max_seq_length-bytebuf_size; i+=bytebuf_size) {
        //      for (int i=0; i<max_seq_length; i+=bytebuf_size) {
        charcount += br.read(charbuf);
        buf.append(charbuf);
        //        for (int k=0; k<charbuf.length; k++) {
        //          buf.append(charbuf[k]);
        //        }
      }
      System.out.println("chars read: " + charcount);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (br != null) {try {br.close();} catch (IOException ioe) {}}
    }
    /*
    System.gc();

    String residues = null;
    System.gc();
    residues = new String(buf);
    buf = null;
    System.gc();
    System.out.println("id: " + seqid);
    //    System.out.println("residues: " + residues.length());
    if (seq == null) {
      seq = new SimpleAnnotatedBioSeq(seqid, residues);
    }
    else {  // try to merge with existing seq
      if (seq.getID().equals(seqid)) {
        seq.setResidues(residues);
      }
      else {
        System.out.println("ABORTING MERGE, sequence ids don't match: " +
                           "old seq id = " + seq.getID() + ", new seq id = " + seqid);
      }
    }
    */
    System.out.println("time to execute: " + tim.read()/1000f);
    mem.printMemory();
    System.out.println("done loading fasta file");
    System.out.println("StringBuffer 0-100: " + buf.substring(0, 100));
    return seq;
  }
  
  // Read FASTA sequence from specified file.
  // We assume a header of less than 500 characters, terminated by a newline.
  // We assume exactly one sequence in the FASTA file.
  // We assume no comment lines.
  // We assume exactly 70 nucleotides per line (until the last line), with a carriage return following each line.
  // Sequence range is specified in interbase format. (See http://www.biodas.org/documents/das2/das2_get.html.)
    public static byte[] ReadFASTA(File seqfile, int begin_sequence, int end_sequence)
            throws FileNotFoundException, IOException, IllegalArgumentException {
        
        if (end_sequence < begin_sequence)
            throw new java.lang.IllegalArgumentException("range " + begin_sequence + ":" + end_sequence + " was negative.");
        
        byte[] buf = null;
        DataInputStream dis = new DataInputStream(new FileInputStream(seqfile));
        BufferedInputStream bis = new BufferedInputStream(dis);
        
       // try {
            // Skip to the location past the header.
            int header_len = skipFASTAHeader(seqfile.getName(), bis);
            
            //System.out.println("header len was :" + header_len);
            
            bis.reset();
            bis.skip(header_len);

            // begin_sequence of 0 is first nucleotide.


            // Skip to location of begin_sequence.  Since there are 71 characters per line, that should be:
            // floor(begin_sequence / 70) * 71 + begin_sequence % 70.
            int full_lines_to_skip = (begin_sequence / 70);
            int chars_to_skip = 71 * full_lines_to_skip;
            int line_location = begin_sequence % 70;
            /*System.out.println("begin_sequence :" + begin_sequence + " " + 
                    full_lines_to_skip + " " +
                    chars_to_skip + " " +
                    line_location);
             */
            
            // skip all the full lines
            if (bis.skip(chars_to_skip) != chars_to_skip) {
                //System.out.println("skipped lines past EOF");
                return buf;
            }
        
            // skip the additional nucleotides in the line
            if (bis.skip(line_location) != line_location) {
                //System.out.println("skipped nucleotides past EOF");
                return buf;
            }
            
           
            int nucleotides_len = end_sequence - begin_sequence;
            buf = new byte[nucleotides_len];
            
            for (int i=0;i<nucleotides_len;) {
                if (line_location == 70) {
                    // skipping the newline
                    byte[] x = new byte[1];
                    int nucleotides_read = bis.read(x,0,1);
                    if (nucleotides_read < 1) {
                        // end of file hit.  quit parsing.
                        //System.out.println("Unexpected End of File at newline!");
                        return returnShortenedBuffer(buf, i);
                    }
                    if (nucleotides_read == 1 && x[0] == '\n') {
                        // expected
                        line_location=0;
                        continue;
                    }
                    
                    // What is this character?
                    throw new java.io.UnsupportedEncodingException("Unexpected char at end of line: " + (char)x[0]);
                }
                
                // Read several characters if possible
                int nucleotides_left_on_this_line = Math.min(70 - line_location, nucleotides_len - i);
                int nucleotides_read = bis.read(buf, i, nucleotides_left_on_this_line);
                i+= nucleotides_read;
                line_location = 70;
                
                if (nucleotides_read != nucleotides_left_on_this_line) {
                    // end of file hit.  quit parsing.
                    //System.out.println("Unexpected End of File!");
                    //System.out.println("i,nucleotides_read" + i + " " + nucleotides_read);
                    
                    return returnShortenedBuffer(buf, i);
                }
            }
          
            return buf;
     /*   }
        finally {
            dis.close();
            return buf;
        }
      * */
    }
    
     private static byte[] returnShortenedBuffer(byte[] buf, int i) {
        //System.out.println("in shortened buffer: i = " + (int)buf[i]);
        // Copy over shortened buffer
        // was last character a newline? skip it as well.
        if (buf[i] == '\n' || buf[i] == 0) {
            i--;
            //System.out.println("removing character");
        }
        if (i == 0) {
            return null;
        }
        byte[] buf2 = new byte[i];
        for (int j = 0; j < i; j++) {
            buf2[j] = buf[j];
            //System.out.print("[" + j + "," + (int)buf[j] + "]");
        }
        return buf2;
    }

    // Parse off the header (if it exists).
    private static int skipFASTAHeader(String filename, BufferedInputStream bis) throws IOException, UnsupportedEncodingException {
        // The header is less than 500 bytes, and if it exists, the header begins with a ">" and ends with a newline.
        //System.out.println("Reading FASTA file");
        byte[] header = new byte[500];

        bis.mark(500);
        bis.read(header, 0, 500);
        //System.out.println("Header : " + header.toString());
        if (header[0] == '>') {
            // We found a header
            // Parse until we're done with header.
            
            for (int i=1;i<500;i++) {
                //System.out.print(header[i] + " ");
                if (header[i] == '\n') {
                    //System.out.println("");
                    //System.out.println("Header was :" + header.toString());
                    return i + 1; // the actual header length, since it was 0-indexed.
                }
            }
            
            // There was no newline?
            throw new java.io.UnsupportedEncodingException("file " + filename + " header does not match expected FASTA format.");
        } 
        //System.out.println("Didn't start with >");
        // no header.
        return 0;
    }
    
    public static String getMimeType()  { return "text/plain"; } 

  static void printMemory() {
    Runtime rt = Runtime.getRuntime();
    long currFreeMem = rt.freeMemory();
    long currTotalMem = rt.totalMemory();
    long currMemUsed = currTotalMem - currFreeMem;
    System.out.println("memory used = " + currMemUsed/1000000 + " MB  ," +
                       " total memory = " + currTotalMem/1000000 + " MB");
  }
}



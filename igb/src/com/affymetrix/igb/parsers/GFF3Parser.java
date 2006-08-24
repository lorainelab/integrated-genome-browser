/**
*   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.igb.genometry.GFF3Sym;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 *  GFF version 3 parser.
 * <pre>
 *  GFF format is 9 tab-delimited fields:
 *   <seqname> <source> <feature> <start> <end> <score> <strand> <frame> <phase> <attributes>
 *
 *  The attribute field contains URL-encoded tag-value pairs separated by semicolons:
 *  "tag1=val1;tag2=val2;tag3=this%20is%20a%20test".
 *
 *  See http://song.sourceforge.net/gff3.shtml.
 *</pre>
 */
public class GFF3Parser {
  public static final int GFF3 = 3;

  // Any tag name beginning with a capital letter must be one of the reserved names.
  public static final String GFF3_ID = "ID";
  public static final String GFF3_NAME = "Name";
  public static final String GFF3_ALIAS = "Alias";
  public static final String GFF3_PARENT = "Parent";
  public static final String GFF3_TARGET = "Target";
  public static final String GFF3_GAP = "Gap";
  public static final String GFF3_DERIVES_FROM = "Derives_from";
  public static final String GFF3_NOTE = "Note";
  public static final String GFF3_DBXREF = "Dbxref";
  public static final String GFF3_ONTOLOGY_TERM = "Ontology_term";
  
  int gff_version = GFF3;

  // Must be exactly one tab between each column; not spaces or multiple tabs.
  static final Pattern line_regex = Pattern.compile("\\t");

  Map gff3_id_hash = new HashMap();

  TrackLineParser track_line_parser = new TrackLineParser();
  
  public GFF3Parser() {
  }


  /**
   *  Parses GFF3 format and adds annotations to the appropriate seqs on the
   *  given seq group.
   */
  public List parse(InputStream istr, AnnotatedSeqGroup seq_group)
    throws IOException {
    System.out.println("starting GFF3 parse.");

    int line_count = 0;

    gff3_id_hash = new HashMap();
    java.util.List results = new ArrayList();

    BufferedReader br = new BufferedReader(new InputStreamReader(istr));
    String line = null;

    Map id2sym = new HashMap();
    List all_syms = new ArrayList();
    
    try {
      Thread thread = Thread.currentThread();
      while ((! thread.isInterrupted()) && ((line = br.readLine()) != null)) {
        if (line == null) { continue; }
        if ("###".equals(line)) {
          // This directive signals that we can process all parent-child relationships up to this point.
          // But there is not much benefit in doing so.
          continue;
        }
        if (line.startsWith("##")) { processDirective(line); continue; }
        if (line.startsWith("#")) { continue; }
        if (line.startsWith("track")) {
          // in GFF files, the color will only be applied from track lines 
          // iff the "source" name matches the track line name.
          track_line_parser.setTrackProperties(line);
          continue;
        }
        String fields[] = line_regex.split(line);

        if (fields != null && fields.length >= 8) {
          line_count++;
          if ((line_count % 10000) == 0) { System.out.println("" + line_count + " lines processed"); }

          String seq_name = fields[0].intern();
          String source = fields[1].intern();
          String feature_type = fields[2].intern();
          int coord_a = Integer.parseInt(fields[3]);
          int coord_b = Integer.parseInt(fields[4]);
          String score_str = fields[5];
          char strand_char = fields[6].charAt(0);
          char frame_char = fields[7].charAt(0);
          String attributes_field = null;
          // last_field is "attributes" in both GFF2 and GFF3, but uses different format.
          if (fields.length>=9) { attributes_field = new String(fields[8]); } // creating a new String saves memory

          float score = GFF3Sym.UNKNOWN_SCORE;
          if (! ".".equals(score_str)) { score = Float.parseFloat(score_str); }

          MutableAnnotatedBioSeq seq = seq_group.getSeq(seq_name);
          if (seq == null) {
            seq = seq_group.addSeq(seq_name, 0);
          }
          
	  GFF3Sym sym = new GFF3Sym(seq, source, feature_type, coord_a, coord_b,
					  score, strand_char, frame_char,
					  attributes_field);

          int max = sym.getMax();
          if (max > seq.getLength()) { seq.setLength(max); }
          

          /*
           From GFF3 spec:
           The ID attributes are only mandatory for those features that have children,
           or for those that span multiple lines.  The IDs do not have meaning outside 
           the file in which they reside.
           */
          String the_id = GFF3Sym.getIdFromGFF3Attributes(attributes_field);
          if (the_id == null) {
            all_syms.add(sym);
          } else {
            // put it in the id2sym hash, or merge it with an existing item already in the hash
            
            GFF3Sym old_sym = (GFF3Sym) id2sym.get(the_id);
            if (old_sym == null) {
              id2sym.put(the_id, sym);
              all_syms.add(sym);
            } else {
              id2sym.put(the_id, mergeSyms(old_sym, sym));
              // old_sym is already in the list all_syms, and this new one should be discarded
            }
          }
        }
      }
    } finally {
      br.close();
    }

    Iterator iter = all_syms.iterator();
    while (iter.hasNext()) {
      GFF3Sym sym = (GFF3Sym) iter.next();
      String[] parent_ids = GFF3Sym.getGFF3PropertyFromAttributes(GFF3_PARENT, sym.getAttributes());
      
      if (parent_ids.length == 0) {
        // If no parents, then it is top-level
        results.add(sym);
      } else {
        // Else, add this as a child to *each* parent in its parent list.
        // It is an error if the parent doesn't exist.
        for (int i=0; i<parent_ids.length; i++) {
          String parent_id = parent_ids[i];
          GFF3Sym parent_sym = (GFF3Sym) id2sym.get(parent_id);
          if (parent_sym == null) {
            throw new IOException("No parent found with ID: " + parent_id);
          } else if (parent_sym == sym) {
            throw new IOException("Parent and child are the same for ID: " + parent_id);
          } else {
            parent_sym.addChild(sym);
          }
        }
      }
    }
    // hashtable no longer needed
    id2sym.clear();

    //Loop over the top-level annotations and add them to the bioseq.
    // (this can't be done in the loop above if we also want to resort children)
      
    Iterator iter2 = results.iterator();
    while (iter2.hasNext()) {
      GFF3Sym s = (GFF3Sym) iter2.next();
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq) s.getBioSeq();
      // I want to sort the Exons, but not other children.
      //s.sortChildren(SeqSpanStartComparator.getComparator(true));
      seq.addAnnotation(s);
    }

    System.out.print("Finished parsing GFF3.");
    System.out.print("  line count: " + line_count);
    System.out.println("  result count: " + results.size());
    return results;
  }


  static final Pattern directive_version = Pattern.compile("##gff-version\\s+(.*)");

  /**
   *  Process directive lines in the input, which are lines beginning with "##".
   *  Directives that are not understood are treated as comments.
   *  Directives that are understood include "##gff-version", which must match "3".
   */
  void processDirective(String line) throws IOException {
    Matcher m = directive_version.matcher(line);
    if (m.matches()) {
      String vstr = m.group(1).trim();
      if (! "3".equals(vstr)) {
        throw new IOException("The specified GFF version can not be processed by this parser: version = '" + vstr + "'");
      }
      return;
    }
  }

  /**
   *  Utility to merge GFF3 features that were specified on several lines with the same ID.
   *  Lines with the same ID are supposed to represent different parts of the same feature.
   *  CDS features are usually expressed this way.  For a truly-general
   *  GFF3 parser, we might want to create a parent-child relationship for the
   *  CDS and the CDS-Pieces.  (The CDS-pieces often have different values for
   *  'frame'; other properties might possibly vary.)
   *  For the purposes of IGB, just merge the CDS into one symmetry covering 
   *  the full extent of the CDS, and throw away the individual pieces.
   *  (The different 'frame' values are irrelevant to IGB.)
   *  Currently the only information that gets modified in the 'to_keep' symmetry
   *  are the start and end.
   *  @param to_keep  The symmetry that you want to expand.
   *  @param to_add   The ammount you want to extend the range of the first sym.
   *  @return  The modified sym to_keep is returned.
   */
  static GFF3Sym mergeSyms(GFF3Sym to_keep, GFF3Sym to_add) {
    if (! to_keep.getFeatureType().equals(to_add.getFeatureType())) {
      System.out.println("WARNING: Merging GFF3 Lines of different feature types: ID="+to_keep.getID());
    }
    if (! to_keep.getSource().equals(to_add.getSource())) {
      System.out.println("WARNING: Merging GFF3 lines of different sources: ID=" + to_keep.getID());
    }
    //Maybe also merge all attribute properties from second sym into first one?  May not be needed.

    // Here I make use of the fact that a GFF3Sym is a type of SeqSpan
    SeqUtils.encompass(to_keep, to_add, to_keep);    
    return to_keep;
  }
}

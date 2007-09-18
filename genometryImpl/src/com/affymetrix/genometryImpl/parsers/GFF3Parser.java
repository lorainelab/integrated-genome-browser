/**
*   Copyright (c) 2006-2007 Affymetrix, Inc.
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

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GFF3Sym;

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

  TrackLineParser track_line_parser = new TrackLineParser();

  public GFF3Parser() {
  }

  /**
   *  Parses GFF3 format and adds annotations to the appropriate seqs on the
   *  given seq group.
   */
  public List parse(InputStream istr, String default_source, AnnotatedSeqGroup seq_group)
    throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(istr));

    // Note that the parse(BufferedReader) method will call br.close(), so
    // don't worry about it.
    return parse(br, default_source, seq_group);
  }

  boolean use_track_lines = true;

  /**
   *  Parses GFF3 format and adds annotations to the appropriate seqs on the
   *  given seq group.
   */
  public List<SeqSymmetry> parse(BufferedReader br, String default_source, AnnotatedSeqGroup seq_group)
    throws IOException {
    System.out.println("starting GFF3 parse.");

    int line_count = 0;

    List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();

    String line = null;

    Map<String,SeqSymmetry> id2sym = new HashMap<String,SeqSymmetry>();
    ArrayList<SeqSymmetry> all_syms = new ArrayList<SeqSymmetry>();
    String track_name = null;

    try {
      Thread thread = Thread.currentThread();
      while ((! thread.isInterrupted()) && ((line = br.readLine()) != null)) {
        if (line == null) { continue; }
        if ("###".equals(line)) {
          // This directive signals that we can process all parent-child relationships up to this point.
          // But there is not much benefit in doing so.
          continue;
        }
        if ("##FASTA".equals(line)) {
          break;
        }
        if (line.startsWith("##track")) {
          track_line_parser.parseTrackLine(line);
          TrackLineParser.createAnnotStyle(seq_group, track_line_parser.getCurrentTrackHash(), default_source);
          track_name = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
          continue;
        }
        if (line.startsWith("##")) { processDirective(line); continue; }
        if (line.startsWith("#")) { continue; }
        String fields[] = line_regex.split(line);

        if (fields != null && fields.length >= 8) {
          line_count++;
          if ((line_count % 10000) == 0) { System.out.println("" + line_count + " lines processed"); }

          String seq_name = fields[0].intern();
          String source;
          if (".".equals(fields[1])) {
            source = default_source;
          } else {
            source = fields[1].intern();
          }
          String feature_type = GFF3Sym.normalizeFeatureType(fields[2]);
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

          if (use_track_lines && track_name != null) {
            sym.setProperty("method", track_name);
          } else {
            sym.setProperty("method", source);
          }

          int max = sym.getMax();
          if (max > seq.getLength()) { seq.setLength(max); }


          /*
           From GFF3 spec:
           The ID attributes are only mandatory for those features that have children,
           or for those that span multiple lines.  The IDs do not have meaning outside
           the file in which they reside.
           */
          String the_id = (String) sym.getProperty(GFF3_ID); // NOT: sym.getID()
          if (the_id == null) {
            all_syms.add(sym);
          } else if (the_id.equals("null") || "-".equals(the_id)) {
            // probably never happens, but just being safe.....
            all_syms.add(sym);
          } else {
            // put it in the id2sym hash, or merge it with an existing item already in the hash

            GFF3Sym old_sym = (GFF3Sym) id2sym.get(the_id);
            if (old_sym == null) {
              id2sym.put(the_id, sym);
              all_syms.add(sym);
            } else {
              if (old_sym.isMultiLine()) {
                // if a group symmetry with the same ID already exists,
                // just add this as a child of it.
                old_sym.addChild(sym);
                SeqUtils.encompass(old_sym, sym, old_sym);
              } else {
                // Create a group symmetry, with the both existing symmetries as children
                GFF3Sym group_sym = groupSyms(the_id, old_sym, sym);
                // Put the group symmetry in the id2sym hash, and also
                // put the group symmetry in the all_syms list, replacing the one that was there.
                id2sym.put(the_id, group_sym);
                all_syms.set(all_syms.indexOf(old_sym), group_sym);
              }
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


      String id = sym.getID();
      if (id != null && ! "-".equals(id)) {
        seq_group.addToIndex(id, sym);
      }

      if (parent_ids.length == 0) {
        // If no parents, then it is top-level
        results.add(sym);
      } else {
        // Else, add this as a child to *each* parent in its parent list.
        // It is an error if the parent doesn't exist.
        for (int i=0; i<parent_ids.length; i++) {
          String parent_id = parent_ids[i];
          if (parent_id == "-") {
            throw new IOException("Parent ID cannot be '-'");
          }
          GFF3Sym parent_sym = (GFF3Sym) id2sym.get(parent_id);
          if (parent_sym == null) {
            throw new IOException("No parent found with ID: " + parent_id);
          } else if (parent_sym == sym) {
            throw new IOException("Parent and child are the same for ID: " + parent_id);
          } else {
            parent_sym.addChild(sym);

            // I'm not sure about this.
            // In Genometry, parent span usually encompasses spans of all children.
            // In GFF3, that isn't really required.
            //SeqUtils.encompass(parent_sym, sym, parent_sym);
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
   *  Utility to group GFF3 features that were specified on several lines with the same ID.
   *  Lines with the same ID are supposed to represent different parts of the same feature.
   *  CDS features are usually expressed this way.
   *  All properties specified on separate lines, except start, stop and frame,
   *  are supposed to be equivalent on all lines.
   *  In Genometry, we need to create a parent symmetry to hold the individual
   *  pieces.  This parent symmetry will be a MultiLineGFF3Sym with the type as specified
   *  (such as "cds") and the two given sym's will become its children with "-part"
   *  attended to their type (such as "cds-type").
   *  The attributes of the group symmetry will be taken from the attributes of
   *  sym1; but the specification requires that all parts of the group have
   *  identical attributes.
   */
  static GFF3Sym groupSyms(String id, GFF3Sym sym1, GFF3Sym sym2) {
    char strand = '.';
    if (sym1.isForward()) {
      strand = '+';
    } else {
      strand = '-';
    }
    String type = sym1.getFeatureType();
    GFF3Sym parent = new GFF3Sym.MultiLineGFF3Sym(
        sym1.getBioSeq(), sym1.getSource(), type,
        Math.min(sym1.getMin(), sym2.getMin()) + 1, Math.max(sym1.getMax(), sym2.getMax()),
        GFF3Sym.UNKNOWN_SCORE, strand, GFF3Sym.UNKNOWN_FRAME, sym1.getAttributes());
    parent.setProperty("method", sym1.getProperty("method"));
    sym1.feature_type = type + "-part";
    sym2.feature_type = type + "-part";
    parent.addChild(sym1);
    parent.addChild(sym2);

    return parent;
  }
}

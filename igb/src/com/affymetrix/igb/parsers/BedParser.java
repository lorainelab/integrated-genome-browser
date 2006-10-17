/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.UcscBedSym;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SimpleSymWithProps;

/**
 *  A parser for UCSC's BED format.
 *  <pre>
 *  BED is tab-delimited
 *
 *  From http://genome.ucsc.edu/goldenPath/help/customTrack.html#BED
 *  BED format provides a flexible way to define the data lines that are displayed
 *  in an annotation track. BED lines have three required fields and nine additional
 *  optional fields. The number of fields per line must be consistent throughout
 *  any single set of data in an annotation track.
 *
 * Some BED files from UCSC contain an initial column *before* the chromosome name.
 * We simply ignore this column; we recognize that it is there by the fact that
 * the strand is given in (zero-based-)column 6 rather than 5.
 *
 * The first three required BED fields are:
 *    [0] chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random) or contig (e.g. ctgY1).
 *    [1] chromStart - The starting position of the feature in the chromosome or contig.
 *           The first base in a chromosome is numbered 0.
 *    [2] chromEnd - The ending position of the feature in the chromosome or contig. The chromEnd
 *           base is not included in the display of the feature. For example, the first 100 bases
 *           of a chromosome are defined as chromStart=0, chromEnd=100, and span the bases numbered 0-99.
 * The 9 additional optional BED fields are:
 *    [3] name - Defines the name of the BED line. This label is displayed to the left of the BED line
 *          in the Genome Browser window when the track is open to full display mode.
 *    [4] score - A score between 0 and 1000. If the track line useScore attribute is set to 1 for
 *          this annotation data set, the score value will determine the level of gray in which
 *          this feature is displayed (higher numbers = darker gray).
 *    [5] strand - Defines the strand - either '+' or '-'.
 *    [6] thickStart - The starting position at which the feature is drawn thickly (for example,
 *          the start codon in gene displays).
 *    [7] thickEnd - The ending position at which the feature is drawn thickly (for example,
 *          the stop codon in gene displays).
 *        If thickStart = thickEnd, that should be interpreted as the absence of a thick region
 *    [8] reserved - This should always be set to zero.
 *    [9] blockCount - The number of blocks (exons) in the BED line.
 *    [10] blockSizes - A comma-separated list of the block sizes. The number of items in this list
 *          should correspond to blockCount.
 *    [11] blockStarts - A comma-separated list of block starts. All of the blockStart positions
 *          should be calculated relative to chromStart. The number of items in this list should
 *          correspond to blockCount.
 * Example:
 *   Here's an example of an annotation track that uses a complete BED definition:
 *
 *  track name=pairedReads description="Clone Paired Reads" useScore=1
 *  chr22 1000 5000 cloneA 960 + 1000 5000 0 2 567,488, 0,3512
 *  chr22 2000 6000 cloneB 900 - 2000 6000 0 2 433,399, 0,3601
 *
 * </pre>
 */
public class BedParser implements AnnotationWriter, StreamingParser, ParserListener  {
  static final boolean DEBUG = false;
  static Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
  static Pattern comma_regex = Pattern.compile(",");
  static Pattern tagval_regex = Pattern.compile("=");
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  protected Map name_counts = new HashMap();
  java.util.List symlist = new ArrayList();
  Map seq2types = new HashMap();
  boolean annotate_seq = true;
  boolean create_container_annot = false;
  String default_type = null;


  static Integer int1 = new Integer(1);
  java.util.List parse_listeners = new ArrayList();

  TrackLineParser track_line_parser = new TrackLineParser();
  
  public BedParser() {
    super();
  }

  public void addParserListener(ParserListener listener) {
    parse_listeners.add(listener);
  }

  public void removeParserListener(ParserListener listener) {
    parse_listeners.remove(listener);
  }

  public java.util.List parse(InputStream istr, AnnotatedSeqGroup group, boolean annot_seq,
			      String stream_name, boolean create_container)
    throws IOException {
    System.out.println("BED parser called, annotate seq: " + annotate_seq +
		       ", create_container_annot: " + create_container);
    /*
     *  seq2types is hash for making container syms (if create_container_annot == true)
     *  each entry in hash is: BioSeq ==> type2psym hash
     *     Each type2csym is hash where each entry is "type" ==> container_sym
     *  so two-step process to find container sym for a particular type on a particular seq:
     *    Map type2csym = (Map)seq2types.get(seq);
     *    MutableSeqSymmetry container_sym = (MutableSeqSymmetry)type2csym.get(type);
     */
    seq2types = new HashMap();
    symlist = new ArrayList();
    name_counts = new HashMap();
    annotate_seq = annot_seq;
    this.create_container_annot = create_container;
    default_type = stream_name;

    if (stream_name.endsWith(".bed")) {
      default_type = stream_name.substring(0, stream_name.lastIndexOf(".bed"));
    }
    BufferedInputStream bis;
    if (istr instanceof BufferedInputStream) {
      bis = (BufferedInputStream)istr;
    }
    else {
      bis = new BufferedInputStream(istr);
    }
    DataInputStream dis = new DataInputStream(bis);
    addParserListener(this);
    parseWithEvents(dis, group, default_type);
    removeParserListener(this);
    System.out.println("BED annot count: " + symlist.size());
    return symlist;
  }

  public void parseWithEvents(DataInputStream dis, AnnotatedSeqGroup seq_group, String default_type)
     throws IOException  {
    System.out.println("called BedParser.parseWithEvents()");
    String line;

    Thread thread = Thread.currentThread();
    BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
    while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
      if (line.startsWith("#") || "".equals(line)) {  // skip comment lines
	continue;
      }
      else if (line.startsWith("track")) {
	track_line_parser.setTrackProperties(line, default_type);
	continue;
      }
      else if (line.startsWith("browser")) {
	// currently take no action for browser lines
      }
      else {
	if (DEBUG) {
	  System.out.println(line);
	}
	String[] fields = line_regex.split(line);
	int field_count = fields.length;
	String seq_name = null;
	String annot_name = null;
	int min, max;
	int thick_min = Integer.MIN_VALUE;  // Integer.MIN_VALUE signifies that thick_min is not used
	int thick_max = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_max is not used
	float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
	boolean forward;
	int[] blockSizes = null;
	int[] blockStarts = null;
	int[] blockMins = null;
	int[] blockMaxs = null;

	//	String type = (String)track_hash.get("name");
	String type = (String) track_line_parser.getCurrentTrackHash().get("name");
	if (type == null) { type = default_type; }
	if (fields != null && field_count >= 3) {
	  boolean includes_bin_field = (field_count > 6 &&
					(fields[6].startsWith("+") ||
					 fields[6].startsWith("-") ||
					 fields[6].startsWith(".") ) );
	  int findex = 0;
	  if (includes_bin_field) { findex++; }
	  seq_name = fields[findex++]; // seq id field
	  MutableAnnotatedBioSeq seq = seq_group.getSeq(seq_name);
	  if ((seq == null) && (seq_name.indexOf(';') > -1)) {
	    // if no seq found, try and split up seq_name by ";", in case it is in format
	    //    "seqid;genome_version"
	    String seqid = seq_name.substring(0, seq_name.indexOf(';'));
	    String version = seq_name.substring(seq_name.indexOf(';')+1);
	    //	    System.out.println("    seq = " + seqid + ", version = " + version);
	    if ((gmodel.getSeqGroup(version) == seq_group) ||
		seq_group.getID().equals(version)) {
	      // for format [chrom_name];[genome_version]
	      seq = seq_group.getSeq(seqid);
	      if (seq != null) { seq_name = seqid; }
	    }
	    // try flipping seqid and version around
	    else if ((gmodel.getSeqGroup(seqid) == seq_group) ||
		seq_group.getID().equals(seqid)) {
	      // for format [genome_version];[chrom_name]
	      String temp = seqid;
	      seqid = version;
	      version = temp;
	      seq = seq_group.getSeq(seqid);
	      if (seq != null) { seq_name = seqid; }
	    }
	  }

	  if (seq == null) {
	    System.out.println("seq not recognized, creating new seq: " + seq_name);
	    seq = seq_group.addSeq(seq_name, 0);
	  }
	  int beg = Integer.parseInt(fields[findex++]);  // start field
	  int end = Integer.parseInt(fields[findex++]);  // stop field
	  if (field_count >= 4) {
	    annot_name = fields[findex++]; // name field
	    Integer count = (Integer)name_counts.get(annot_name);
	    if (count == null) {
	      name_counts.put(annot_name, int1);
	    }
	    else {
	      Integer new_count = new Integer(count.intValue() + 1);
	      name_counts.put(annot_name, new_count);
	      annot_name = annot_name + "." + new_count.toString();
	    }
	  }
	  if (field_count >=5) { score = Float.parseFloat(fields[findex++]); } // score field
	  if (field_count >= 6) { forward = (fields[findex++].equals("+")); }  // strand field
	  else  { forward = (beg <= end); }
	  min = (int)Math.min(beg, end);
	  max = (int)Math.max(beg, end);

	  if (field_count >= 8) {
	    thick_min = Integer.parseInt(fields[findex++]); // thickStart field
	    thick_max = Integer.parseInt(fields[findex++]); // thickEnd field
	  }
	  findex += 2;
	  if (field_count >= 12) {
	    blockSizes = parseIntArray(fields[findex++]); // blockSizes field
	    blockStarts = parseIntArray(fields[findex++]); // blockStarts field
	    blockMins = makeBlockMins(min, blockStarts);
	    blockMaxs = makeBlockMaxs(blockSizes, blockMins);
	  }
	  else {
	    /*
	     * if no child blocks, make a single child block the same size as the parent
	     * Very Inefficient, ideally wouldn't do this
	     * But currently need this because of GenericAnnotGlyphFactory use of annotation depth to
	     *     determine at what level to connect glyphs -- if just leave blockMins/blockMaxs null (no children),
	     *     then factory will create a line container glyph to draw line connecting all the bed annots
	     * Maybe a way around this is to adjust depth preference based on overall depth (1 or 2) of bed file?
	     */
	    blockMins = new int[1];
	    blockMins[0] = min;
	    blockMaxs = new int[1];
	    blockMaxs[0] = max;
	  }

	  if (max > seq.getLength()) {
	    seq.setLength(max);
	  }
	  if (DEBUG) {
	    System.out.println("fields: "  + field_count + ", type = " + type + ", seq = " + seq_name +
			       ", min = " + min + ", max = " + max +
			       ", name = " + annot_name + ", score = " + score +
			       ", forward = " + forward +
			       ", thickmin = " + thick_min + ", thickmax = " + thick_max);
	    if (blockMins != null) {
	      int count = blockMins.length;
	      if (blockSizes != null && blockStarts != null && blockMins != null && blockMaxs != null) {
		for (int i=0; i<count; i++) {
		  System.out.println("   " + i + ": blockSize = " + blockSizes[i] +
				     ", blockStart = " + blockStarts[i] +
				     ", blockMin = " + blockMins[i] +
				     ", blockMax = " + blockMaxs[i]);
		}
	      }
	    }
	  }
	  SeqSymmetry bedline_sym = null;
	  bedline_sym = new UcscBedSym(type, seq, min, max, annot_name, score, forward,
				       thick_min, thick_max, blockMins, blockMaxs);
	  // if there are any ParserListeners registered, notify them of parse
	  if (parse_listeners.size() > 0) {
	    for (int i=0; i<parse_listeners.size(); i++) {
	      ParserListener listener = (ParserListener)parse_listeners.get(i);
	      listener.annotationParsed(bedline_sym);
	    }
	  }
          if (annot_name != null) {
            seq_group.addToIndex(annot_name, bedline_sym);
          }
	}  // end field_count >= 3
      }  // end of line.startsWith() else
    }   // end of line-reading loop
  }


  /**
   * Implementation of ParserListener interface.
   * This method must be public to meet ParserListener interface, but
   * for BedParser this is intended only for internal callbacks, and thus should
   * never be called from outside of BedParser.
   */
  public void annotationParsed(SeqSymmetry bedline_sym) {
    symlist.add(bedline_sym);
    if (annotate_seq) {
      MutableAnnotatedBioSeq seq = (MutableAnnotatedBioSeq)bedline_sym.getSpan(0).getBioSeq();
      if (create_container_annot) {
        String type = (String) track_line_parser.getCurrentTrackHash().get("name");
        if (type == null) { type = default_type; }
        Map type2csym = (Map)seq2types.get(seq);
        if (type2csym == null) {
          type2csym = new HashMap();
          seq2types.put(seq, type2csym);
        }
        SimpleSymWithProps parent_sym = (SimpleSymWithProps)type2csym.get(type);
	if (parent_sym == null) {
	  parent_sym = new SimpleSymWithProps();
	  parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
	  parent_sym.setProperty("method", type);
	  seq.addAnnotation(parent_sym);
	  type2csym.put(type, parent_sym);
	}
	parent_sym.addChild(bedline_sym);
      }
      else {
	seq.addAnnotation(bedline_sym);
      }
    }
  }


  protected int[] parseIntArray(String int_array) {
    String[] intstrings = comma_regex.split(int_array);
    int count = intstrings.length;
    int[] results = new int[count];
    for (int i=0; i<count; i++) {
      int val = Integer.parseInt(intstrings[i]);
      results[i] = val;
    }
    return results;
  }

  /**
   *  Converting blockStarts to blockMins.
   *  @param blockStarts  in coords relative to min of annotation
   *  @return blockMins in coords relative to sequence that annotation is "on"
   */
  protected int[] makeBlockMins(int min, int[] blockStarts) {
    int count = blockStarts.length;
    int[] blockMins = new int[count];
    for (int i=0; i<count; i++) {
      blockMins[i] = blockStarts[i] + min;
    }
    return blockMins;
  }

  protected int[] makeBlockMaxs(int[] blockMins, int[] blockSizes)  {
    int count = blockMins.length;
    int[] blockMaxs = new int[count];
    for (int i=0; i<count; i++) {
      blockMaxs[i] = blockMins[i] + blockSizes[i];
    }
    return blockMaxs;
  }



  /**
   *  Writes bed file format.
   *  WARNING. This currently assumes that each child symmetry contains
   *     a span on the seq given as an argument.
   */
  public static void writeBedFormat(Writer out, SeqSymmetry sym, BioSeq seq)
    throws IOException {
    if ((sym instanceof UcscBedSym) && (sym.getSpan(seq) != null)) {
      UcscBedSym bedsym = (UcscBedSym)sym;
      if (seq == bedsym.getBioSeq()) {
	bedsym.outputBedFormat(out);
      }
    }
    else {
      if (DEBUG) {System.out.println("writing sym: " + sym);}
      SeqSpan span = sym.getSpan(seq);
      SymWithProps propsym = null;
      if (sym instanceof SymWithProps) {
	propsym = (SymWithProps)sym;
      }
      if (span != null) {
	int childcount = sym.getChildCount();
	out.write(seq.getID());
	out.write('\t');
	int min = span.getMin();
	int max = span.getMax();
	out.write(Integer.toString(min));
	out.write('\t');
	out.write(Integer.toString(max));
	if ( (! span.isForward()) || (childcount > 0) || (propsym != null) ) {
	  out.write('\t');
	  if (propsym != null) {
	    if (propsym.getProperty("name") != null) { out.write((String)propsym.getProperty("name")); }
	    else if (propsym.getProperty("id") != null) { out.write((String)propsym.getProperty("id")); }
	  }
	  out.write('\t');
	  if ((propsym != null)  && (propsym.getProperty("score") != null))  {
	    out.write(propsym.getProperty("score").toString());
	  }
	  else { out.write('0'); }
	  out.write('\t');
	  if (span.isForward()) { out.write('+'); }
	  else { out.write('-'); }
	  if (childcount > 0) {
	    out.write('\t');
	    if ((propsym != null) && (propsym.getProperty("cds min") != null)) {
	      out.write(propsym.getProperty("cds min").toString());
	    }
	    else { out.write(Integer.toString(min)); }
	    out.write('\t');
	    if ((propsym != null) && (propsym.getProperty("cds max") != null))  {
	      out.write(propsym.getProperty("cds max").toString());
	    }
	    else { out.write(Integer.toString(max)); }
	    out.write('\t');
	    out.write('0');
	    out.write('\t');
	    out.write(Integer.toString(childcount));
	    out.write('\t');
	    int[] blockSizes = new int[childcount];
	    int[] blockStarts = new int[childcount];
	    for (int i=0; i<childcount; i++) {
	      SeqSymmetry csym = sym.getChild(i);
	      SeqSpan cspan = csym.getSpan(seq);
	      blockSizes[i] = cspan.getLength();
	      blockStarts[i] = cspan.getMin() - min;
	    }
	    for (int i=0; i<childcount; i++) {
	      out.write(Integer.toString(blockSizes[i]));
	      out.write(',');
	    }
	    out.write('\t');
	    for (int i=0; i<childcount; i++) {
	      out.write(Integer.toString(blockStarts[i]));
	      out.write(',');
	    }
	  }  // END "if (childcount > 0)"
	}  // END "if ( (! span.isForward()) || (childcount > 0) || (propsym != null) )"

	out.write('\n');
      }   // END "if (span != null)"
    }
  }

  public static void writeBedFormat(Writer wr, java.util.List syms, BioSeq seq)
    throws IOException  {
    int symcount = syms.size();
    for (int i=0; i<symcount; i++) {
      SeqSymmetry sym = (SeqSymmetry)syms.get(i);
      //      System.out.println("trying to write sym: " + sym);
      writeBedFormat(wr, sym, seq);
    }
  }

  /** Tests parsing of the file passed as a parameter. */
  public static void main(String[] args) {
    String file_name = args[0];
    try {
      BedParser test = new BedParser();
      File fil = new File(file_name);
      FileInputStream fis = new FileInputStream(fil);
      // Formerly, bookmarks with a seq-group of "unknown" would be interpreted
      // to mean 'current genome', but that is no longer true.
      AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("unknown");
      
      java.util.List annots = test.parse(fis, seq_group, true, file_name, true);
      System.out.println("total annots: " + annots.size());
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   *  Implementing AnnotationWriter interface to write out annotations
   *    to an output stream as "BED" format.
   **/
  public boolean writeAnnotations(java.util.Collection syms, BioSeq seq,
				  String type, OutputStream outstream) {
    System.out.println("in BedParser.writeAnnotations()");
    boolean success = true;
    try {
      Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
      Iterator iterator = syms.iterator();
      while (iterator.hasNext()) {
	SeqSymmetry sym = (SeqSymmetry)iterator.next();
	writeBedFormat(bw, sym, seq);
      }
      bw.flush();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    return success;
  }

  /** Returns "text/plain". */
  public String getMimeType() { return "text/plain"; }

}


package com.affymetrix.igb.parsers;

import java.io.*;
import java.util.*;

import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.SynonymLookup;

public class BarParser {
  static boolean DEBUG_READ = true;
  static boolean DEBUG_DATA = false;
  static boolean DEBUG_INDEXER = false;

  /** 8-byte floating-point.  Names of the other data-type constants can be interpreted similarly. */
  public static int BYTE8_FLOAT = 0;
  public static int BYTE4_FLOAT = 1;
  public static int BYTE4_SIGNED_INT = 2;
  public static int BYTE2_SIGNED_INT = 3;
  public static int BYTE1_SIGNED_INT = 4;
  public static int BYTE4_UNSIGNED_INT = 5;
  public static int BYTE2_UNSIGNED_INT = 6;
  public static int BYTE1_UNSIGNED_INT = 7;

  protected static int[] bytes_per_val = {
    8,    // BYTE8_FLOAT
    4,    // BYTE4_FLOAT
    4,    // BYTE4_SIGNED_INT
    2,    // BYTE2_SIGNED_INT
    1,    // BYTE1_SIGNED_INT
    4,    // BYTE4_UNSIGNED_INT
    2,    // BYTE2_UNSIGNED_INT
    1     // BYTE1_UNSIGNED_INT
  };

  public static String[] valstrings =
  { "BYTE8_FLOAT", "BYTE4_FLOAT",
    "BYTE4_SIGNED_INT", "BYTE2_SIGNED_INT", "BYTE1_SIGNED_INT",
    "BYTE4_UNSIGNED_INT", "BYTE2_UNSIGNED_INT", "BYTE1_UNSIGNED_INT" };

  /**
   *  For indexing of base coord sets, how many point to compress into single index entry
   */
  static int points_per_chunk = 1024;

  protected static Map coordset2seqs = new HashMap();

  /**
   *
   */
  public static List getSlice(String file_name, String seq_name, int min_base, int max_base) {
    boolean USE_RANDOM_ACCESS = false;
    System.out.println("trying to get slice, min = " + min_base + ", max = " + max_base);
    // first check and see if the file is already indexed
    //  if not already indexed, index it (unless it's too small?)
    //
    //  To make slicing functional, still need to change this so coord set is _not_ the file name,
    //     but rather extracted from a field (or set of fields) in the bar file, so can be shared
    //     across bar files that have the exact same base coords
    int[] chunk_mins = (int[])coordset2seqs.get(file_name);
    if (chunk_mins == null) {
      // index??
    }
    int min_index = 0;
    int max_index = 0;
    if (chunk_mins != null) {
      min_index = Arrays.binarySearch(chunk_mins, min_base);
      max_index = Arrays.binarySearch(chunk_mins, max_base);
      if (min_index < 0) {
	// want min_index to be index of max base coord <= min_base
	min_index = (-min_index -1) - 1;
	if (min_index < 0) { min_index = 0; }
      }
      System.out.println("min_index = " + min_index + ", base_pos = " + chunk_mins[min_index]);
      if (min_index > 0) { 
	System.out.println("  prev index, base_pos = " + chunk_mins[min_index - 1]);
      }
      if (min_index < (chunk_mins.length-1))  {
        System.out.println("  next index, base_pos = " + chunk_mins[min_index + 1]);
      }

      if (max_index < 0) {
	// want max_index to be index of min base coord >= max_base
	//   (insertion point)  [as defined in Arrays.binarySearch() docs]
	max_index = -max_index -1;
	if (max_index <= 0) { max_index = 0; }
	//	else if (max_index >= chunk_mins.length) { max_index = chunk_mins.length - 1; }
      }

      System.out.println("max_index = " + max_index + ", base_pos = " + chunk_mins[max_index]);
      if (max_index > 0) { 
	System.out.println("  prev index, base_pos = " + chunk_mins[max_index - 1]);
      }
      if (max_index < (chunk_mins.length-1))  {
        System.out.println("  next index, base_pos = " + chunk_mins[max_index + 1]);
      }
    }
    //    int points_to_skip = min_index * points_per_index;
    //    int start_point 
    //    int point_count = 
    Map seqs = new HashMap();
    try {
      DataInput dis = null;
      if (USE_RANDOM_ACCESS) {
	
      }
      else {
	FileInputStream fis = new FileInputStream(new File(file_name));
	dis = new DataInputStream(new BufferedInputStream(fis));
      }
      BarFileHeader bar_header = parseBarHeader(dis);
      BarSeqHeader seq_header = parseSeqHeader(dis, seqs, bar_header);
      int bytes_per_point = bar_header.bytes_per_point;
      int points_per_index = points_per_chunk;
      int points_to_skip = min_index * points_per_index;
      int bytes_to_skip = points_to_skip * bytes_per_point;
      int points_to_read = (max_index - min_index) * points_per_index;
      int bytes_to_read = points_to_read * bytes_per_point;
      System.out.println("bytes to skip: " + bytes_to_skip);
      System.out.println("bytes to read: " + bytes_to_read);
      while (bytes_to_skip > 0)  {
	int skipped = dis.skipBytes(bytes_to_skip);
	if (DEBUG_READ)  { System.out.println("   skipped: " + skipped); }
	if (skipped < 0) {
	  if (DEBUG_READ)  {System.out.println("end of file reached"); }
	  break;
	} // EOF reached
	bytes_to_skip -= skipped;
      }
      byte[] buf = new byte[bytes_to_read];
      dis.readFully(buf);

      DataInputStream bufstr = new DataInputStream(new ByteArrayInputStream(buf));
      int start_base_pos = bufstr.readInt();
      //      System.out.println("skipped to start base index, base coord = " + start_base_pos);
      System.out.println("start of byte array, base coord = " + start_base_pos);
      
    }
    catch (Exception ex)  { ex.printStackTrace(); }
    return null;
  }

  public static void main(String[] args) {
    String test_file = "c:/data/graph_slice_test/test.bar";
    if (args.length > 0) {
      test_file = args[0];
    }
    //    testFullRead(test_file);
    Map seqs = new HashMap();
    buildIndex(test_file, test_file, seqs);
    //    getSlice(test_file, "temp", 0, 2000000);
    getSlice(test_file, "temp", 67000000, 69000000);
    //    getSlice(test_file, "temp", 158000000, 158400000);
    //    getSlice(test_file, "temp", 157999267, 158400000);
    //    getSlice(test_file, "temp", 158400000, 159000000);
    //    getSlice(test_file, "temp", 158500000, 159000000);
  }

  public static void testFullRead(String test_file) {
    Timer tim = new Timer();
    tim.start();
    try {
      File fil = new File(test_file);
      int total_bytes = (int)fil.length();
      FileInputStream fis = new FileInputStream(fil);
      DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
      byte[] buf = new byte[total_bytes];
      dis.readFully(buf);
      dis.close();
    }
    catch (Exception ex) { ex.printStackTrace(); }
    long time_taken = tim.read();
    System.out.println("time to fully read file: " + time_taken/1000f);
  }




  /**
   *  assumes that first field of every data entry is 4-byte signed int representing base position
   *  assumes base positions are sorted
   *  makes no assumption about the "regularity" of the entries.  If there is some regularity to
   *     the data such that the index into the entries for a particular base position can be
   *     approximately or exactly calculated without prior indexing of the entries, then another
   *     approach may be desired.  For example, if the data is guaranteed to have an entry every 10 bases,
   *     then could just directly calculate which entries to retrieve for a particular base coord range
   *
   *  coord_set_id uniquely identifies the set of sorted base pair coordinates in this bar file,
   *     with the assumption that many other bar files share the same coord_set_id
   *     So for example in the case of a bar file with data from a tiling array chipset, for a
   *     single seq on a single genome assembly, the coord set id might be:
   *     [chipset_id]/[genome_id]/[seq_id]
   *
   *  basic idea is to cache an in-memory simple binary searchable index into the seq's data chunk
   *    in the bar file, and whenever slice is needed from bar files with same base coords as the
   *    indexed one, just use the cached index to figure out where to go directly in the bar files
   *    (using RandomAccessFile to read just the slice)
   *
   *    but rather than an exact index (which would take a lot of memory), just want to get close,
   *    so only build index for every Nth coord entry, for example every 1000th entry.  Then after
   *    a binary search of the index to find approximate start index for a slice, in worst case will
   *    only be reading N * bytes_per_entry extra bytes from disk.  For example if N = 1024, and it's
   *    a typical bar file with base coord + 1 float per entry, worst case is reading 8KB of extra
   *    data off disk per slice query.  And assumption is that this is being applied to bar files
   *    that have > 100K entries, and most likely millions, and the slices are fairly large (at
   *    least 10x > N), so overhead for reading extra data will be minor.
   *
   */
  public static void buildIndex(String file_name, String coord_set_id, Map seqs) {
    Timer tim = new Timer();
    tim.start();
    // builds an index per sequence in the bar file
    try {
      FileInputStream fis = new FileInputStream(new File(file_name));
      BufferedInputStream bis = new BufferedInputStream(fis);
      DataInputStream dis = new DataInputStream(bis);
      BarFileHeader file_header = parseBarHeader(dis);
      int[] val_types = file_header.val_types;
      int bytes_per_point = file_header.bytes_per_point;
      BarSeqHeader seq_header = parseSeqHeader(dis, seqs, file_header);
      int total_points = seq_header.data_point_count;

      int point_count = 0;
      int chunk_count = 0;
      // adding one because indexing _start_ of chunk, so also have partial chunk at end??
      int total_chunks = (total_points / points_per_chunk) + 1;
      int[] chunk_mins = new int[total_chunks];

      System.out.println("total points: " + total_points);
      System.out.println("bytes per data point: " + bytes_per_point);
      System.out.println("points_per_chunk: " + points_per_chunk);
      System.out.println("expected chunk count: " + total_chunks);

      int skip_offset = (points_per_chunk * bytes_per_point) - 4;  // -4 to account for read of 4-byte integer for base coord
      CHUNK_LOOP:
      while (point_count < total_points) {
	int base_pos = dis.readInt();
	chunk_mins[chunk_count] = base_pos;
        if (DEBUG_INDEXER)  {System.out.println("chunk: " + chunk_count + ", index: " + point_count + ",  start base: " + base_pos);}
	int bytes_to_skip = skip_offset;
	while (bytes_to_skip > 0)  {
	  int skipped = (int)dis.skip(bytes_to_skip);
	  if (DEBUG_INDEXER)  { System.out.println("   skipped: " + skipped); }
	  if (skipped < 0) {
	    if (DEBUG_INDEXER)  {System.out.println("end of file reached"); }
	    break CHUNK_LOOP;
	  } // EOF reached
	  bytes_to_skip -= skipped;
	}
	point_count += points_per_chunk;
	if (DEBUG_INDEXER)  {System.out.println("  point count: " + point_count); }
	chunk_count++;
      }
      // just making sure edge case doesn't mess things up...
      if (chunk_mins[total_chunks-1] == 0) { chunk_mins[total_chunks-1] = seq_header.aseq.getLength(); }
      System.out.println("chunk count: " + chunk_count);
      System.out.println("expected chunk count: " + total_chunks);

      //      coordset2indexmap.put(coord_set_id, indexmap);
      //      indexmap.put(seqid, chunk_mins);
      coordset2seqs.put(coord_set_id, chunk_mins);

      dis.close();
    }
    catch (Exception ex) { ex.printStackTrace(); }
    long index_time = tim.read();
    System.out.println("time to index: " + index_time/1000f);
  }

  public static List parse(InputStream istr, Map seqs, String stream_name)
    throws IOException {

    BufferedInputStream bis = new BufferedInputStream(istr);
    DataInputStream dis = new DataInputStream(bis);
    BarFileHeader bar_header = parseBarHeader(dis);

    boolean bar2 = (bar_header.version >= 2.0f);
    int total_seqs = bar_header.seq_count;
    int[] val_types = bar_header.val_types;
    int vals_per_point = bar_header.vals_per_point;
    Map file_tagvals = bar_header.tagvals;

    String graph_name = "unknown";
    if (stream_name != null) { graph_name = stream_name; }
    if (file_tagvals.get("file_type") != null) {
      graph_name += ":" + (String)file_tagvals.get("file_type");
    }
    List graphs = null;
    int total_total_points = 0;
    for (int k=0; k<total_seqs; k++) {
      BarSeqHeader seq_header = parseSeqHeader(dis, seqs, bar_header);
      int total_points = seq_header.data_point_count;
      Map seq_tagvals = seq_header.tagvals;
      MutableAnnotatedBioSeq seq = seq_header.aseq;
      if (vals_per_point == 1) {
        System.err.println("PARSING FOR BAR FILES WITH 1 VALUE PER POINT NOT YET IMPLEMENTED");
      }
      else if (vals_per_point == 2) {
        if (val_types[0] == BYTE4_SIGNED_INT &&
            val_types[1] == BYTE4_FLOAT) {
          if (graphs == null) { graphs = new ArrayList(); }
          //          System.out.println("reading graph data: " + k);
          int xcoords[] = new int[total_points];
          float ycoords[] = new float[total_points];
	  float prev_max_xcoord = -1;
	  boolean sort_reported = false;
          for (int i= 0; i<total_points; i++) {
            //            xcoords[i] = (double)dis.readInt();
            //            ycoords[i] = (double)dis.readFloat();
            int col0 = dis.readInt();
            float col1 = dis.readFloat();
	    if (col0 < prev_max_xcoord && (! sort_reported)) {
	      if (DEBUG_READ) { System.out.println("WARNING!! not sorted by ascending xcoord"); }
	      sort_reported = true;
	    }
	    prev_max_xcoord = col0;
            xcoords[i] = col0;
            ycoords[i] = col1;
            if ((DEBUG_DATA) && (i<100)) {
              System.out.println("Data[" + i + "]:\t" + col0 + "\t" + col1);
            }
          }
          GraphSym graf = new GraphSym(xcoords, ycoords, graph_name, seq);
	  //          graf.setProperties(new HashMap(file_tagvals));
	  copyProps(graf, file_tagvals);
	  if (bar2)  { copyProps(graf, seq_tagvals); }
	  //	  graf.setProperty("method", graph_name);
          //          System.out.println("done reading graph data: " + graf);
          graphs.add(graf);
        }
        else {
          System.err.println("currently, first val must be int4, second must be float4");
        }
      }
      else if (vals_per_point == 3) {
        // System.err.println("PARSING FOR BAR FILES WITH 3 VALUES PER POINT NOT YET IMPLEMENTED");
        if (val_types[0] == BYTE4_SIGNED_INT &&
            val_types[1] == BYTE4_FLOAT &&
            val_types[2] == BYTE4_FLOAT) {
          if (graphs == null) { graphs = new ArrayList(); }
          if (DEBUG_READ)  { System.out.println("reading graph data: " + k); }
          int xcoords[] = new int[total_points];
          float ycoords[] = new float[total_points];
          float zcoords[] = new float[total_points];
          for (int i = 0; i<total_points; i++) {
            //            xcoords[i] = (double)dis.readInt();
            //            ycoords[i] = (double)dis.readFloat();
            int col0 = dis.readInt();
            float col1 = dis.readFloat();
            float col2 = dis.readFloat();
            xcoords[i] = col0;
            ycoords[i] = col1;
            zcoords[i] = col2;
            if (DEBUG_DATA && i < 100) {
              System.out.println("Data[" + i + "]:\t" + col0 + "\t" + col1 + "\t" + col2); }
          }
          String pm_name = graph_name + " : pm";
          String mm_name = graph_name + " : mm";
          GraphSym pm_graf =
            new GraphSym(xcoords, ycoords, graph_name + " : pm", seq);
          GraphSym mm_graf =
            new GraphSym(xcoords, zcoords, graph_name + " : mm", seq);
	  //          mm_graf.setProperties(new HashMap(file_tagvals));
	  //          pm_graf.setProperties(new HashMap(file_tagvals));
	  copyProps(pm_graf, file_tagvals);
	  copyProps(mm_graf, file_tagvals);
          pm_graf.setGraphName(pm_name);
          mm_graf.setGraphName(mm_name);
          //pm_graf.setProperty("graph_name", pm_name);
          //mm_graf.setProperty("graph_name", mm_name);
	  if (bar2)  {
	    copyProps(pm_graf, seq_tagvals);
	    copyProps(mm_graf, seq_tagvals);
	  }
          System.out.println("done reading graph data: ");
          System.out.println("pmgraf, yval = column1: " + pm_graf);
          System.out.println("mmgraf, yval = column2: " + mm_graf);
	  pm_graf.setProperty("probetype", "PM (perfect match)");
	  mm_graf.setProperty("probetype", "MM (mismatch)");
          graphs.add(pm_graf);
          graphs.add(mm_graf);
        }
        else {
          System.err.println("currently, first val must be int4, second must be float4");
        }
      }
    }
    System.out.println("total data points in bar file: " + total_total_points);

    return graphs;
  }

  public static HashMap readTagValPairs(DataInput dis, int pair_count) throws IOException  {
    HashMap tvpairs = new HashMap(pair_count);
    if (DEBUG_READ) { System.out.println("seq tagval count: " + pair_count); }
    for (int i=0; i<pair_count; i++) {
      int taglength = dis.readInt();
      byte[] barray = new byte[taglength];
      dis.readFully(barray);
      String tag = new String(barray);
      // maybe should intern?
      //      String tag = (new String(barray)).intern();
      int vallength = dis.readInt();
      barray = new byte[vallength];
      dis.readFully(barray);
      String val = new String(barray);
      //      String val = (new String(barray)).intern();
      tvpairs.put(tag, val);
      if (DEBUG_READ)  { System.out.println("    tag = " + tag + ", val = " + val); }
    }
    return tvpairs;
  }

  public static void copyProps(GraphSym graf, Map tagvals) {
    if (tagvals == null) { return; }
    Iterator iter = tagvals.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry tagval = (Map.Entry)iter.next();
      String tag = (String)tagval.getKey();
      String val = (String)tagval.getValue();
      graf.setProperty(tag, val);
    }
  }

  public static BarFileHeader parseBarHeader(DataInput dis) throws IOException {
    // READING HEADER
    //    dis.readBytes("barr\r\n\032\n");  // char  "barr\r\n\032\n"
    byte[] headbytes = new byte[8];
    //    byte[] headbytes = new byte[10];
    dis.readFully(headbytes);
    String headstr = new String(headbytes);
    float version = dis.readFloat();       // int  #rows in data section (nrow)
    int total_seqs = dis.readInt();
    int vals_per_point = dis.readInt(); // int  #columns in data section (ncol)
    if (DEBUG_READ) {
      System.out.println("header: " + headstr);
      System.out.println("version: " + version);
      System.out.println("total seqs: " + total_seqs);
      System.out.println("vals per point: " + vals_per_point);
    }
    int[] val_types = new int[vals_per_point];
    for (int i=0; i<vals_per_point; i++) {
      val_types[i] = dis.readInt();
      if (DEBUG_READ)  { System.out.println("val type for column " + i + ": " + valstrings[val_types[i]]); }
    }
    int tvcount = dis.readInt();
    if (DEBUG_READ) { System.out.println("tag-value count: " + tvcount); }
    HashMap file_tagvals = readTagValPairs(dis, tvcount);
    BarFileHeader header = new BarFileHeader(version, total_seqs, val_types, file_tagvals);
    return header;
    /*
    String graph_name = "unknown";
    if (stream_name != null) { graph_name = stream_name; }
    if (file_tagvals.get("file_type") != null) {
      graph_name += ":" + (String)file_tagvals.get("file_type");
    }
    */
  }

  public static BarSeqHeader parseSeqHeader(DataInput dis, Map seqs, BarFileHeader file_header)  throws IOException {
      int namelength = dis.readInt();
      //      String
      byte[] barray = new byte[namelength];
      dis.readFully(barray);
      String seqname = new String(barray);
      if (DEBUG_READ)  { System.out.println("seq: " + seqname); }

      String groupname = null;
      boolean bar2 = (file_header.version >= 2.0f);
      if (bar2) {
	int grouplength = dis.readInt();
	barray = new byte[grouplength];
	dis.readFully(barray);
	groupname = new String(barray);
	if (DEBUG_READ)  { System.out.println("group length: " + grouplength + ", group: " + groupname); }
      }

      int verslength = dis.readInt();
      barray = new byte[verslength];
      dis.readFully(barray);
      String seqversion = new String(barray);
      if (DEBUG_READ) { System.out.println("version length: " + verslength + ", version: " + seqversion); }

      // hack to extract seq version and seq name from seqname field for bar files that were made
      //   with the version and name concatenated (with ";" separator) into the seqname field
      int sc_pos = seqname.lastIndexOf(";");
      if (sc_pos >= 0) {
        seqversion = seqname.substring(0, sc_pos);
	seqname = seqname.substring(sc_pos+1);
	if (DEBUG_READ)  { System.out.println("seqname = " + seqname + ", seqversion = " + seqversion); }
      }

      HashMap seq_tagvals = null;
      if (bar2) {
	int seq_tagval_count = dis.readInt();
	seq_tagvals = readTagValPairs(dis, seq_tagval_count);
      }

      int total_points = dis.readInt();
      System.out.println("   seqname = " + seqname + ", version = " + seqversion +
			 ", group = " + groupname +
			 ", data points = " + total_points);
      //      System.out.println("total data points for graph " + k + ": " + total_points);
      MutableAnnotatedBioSeq seq = null;
      SynonymLookup lookup = SynonymLookup.getDefaultLookup();
      Iterator iter = seqs.values().iterator();
      // can't just hash, because _could_ be a synonym instead of an exact match

      while (iter.hasNext()) {
	// testing both seq id and version id (if version id is available)
        MutableAnnotatedBioSeq testseq = (MutableAnnotatedBioSeq) iter.next();
        if (lookup.isSynonym(testseq.getID(), seqname)) {
	  // GAH 1-23-2005
	  // need to ensure that if bar2 format, the seq group is also a synonym!
	  // GAH 7-7-2005
	  //    but now there's some confusion about seqversion vs seqgroup, so try all three possibilities:
	  //      groupname
	  //      seqversion
	  //      groupname + ":" + seqversion
	  if (seqversion == null || seqversion.equals("") || (! (testseq instanceof Versioned))) {
	    seq = testseq;
	    break;
	  }
	  else {
	    String test_version = ((Versioned)testseq).getVersion();
	    if ((lookup.isSynonym(test_version, seqversion)) ||
		(lookup.isSynonym(test_version, groupname)) ||
		(lookup.isSynonym(test_version, (groupname + ":" + seqversion))) ) {
	      if (DEBUG_READ) { System.out.println("found synonymn"); }
	      seq = testseq;
	      break;
	    }
	  }
        }
      }
      if (seq == null) {
	if (bar2 && groupname != null) {
	  seqversion = groupname + ":" + seqversion;
	}
        System.out.println("seq not found, creating new seq:  name = " + seqname + ", version = " + seqversion);
        seq = new NibbleBioSeq(seqname, seqversion, 500000000);
      }
      //      System.out.println("seq: " + seq);
      BarSeqHeader seq_header = new BarSeqHeader(seq, total_points, seq_tagvals);
      return seq_header;
  }

}

class BarSeqHeader {
  MutableAnnotatedBioSeq aseq;
  int data_point_count;
  Map tagvals;

  public BarSeqHeader(MutableAnnotatedBioSeq seq, int data_points, Map tagvals)  {
    this.aseq = seq;
    this.data_point_count = data_points;
    this.tagvals = tagvals;
  }

}

class BarFileHeader {
  float version;
  int seq_count;
  int vals_per_point;
  int val_types[];
  int bytes_per_point = 0;
  Map tagvals;

  public BarFileHeader(float version, int seq_count, int[] val_types, Map tagvals) {
    this.version = version;
    this.seq_count = seq_count;
    this.val_types = val_types;
    this.vals_per_point = val_types.length;
    this.tagvals = tagvals;

    for (int i=0; i<val_types.length; i++) {
      int valtype = val_types[i];
      bytes_per_point += BarParser.bytes_per_val[valtype];
    }
  }
}

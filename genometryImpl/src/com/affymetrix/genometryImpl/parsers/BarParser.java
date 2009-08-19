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

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.Timer;



/**
 * Parser for files in BAR format.
 * <pre>
 Bar format definition:

 1	Char	8	The file type identifier. This is always set to "barr\r\n\032\n".
 2	Float	4	The file version number.  Valid versions are 1.0 and 2.0
 3	Integer	4	The number of sequences stored in the file. Referred to as NSEQ.
 4	Integer	4	The number of columns per data point. Referred to as NCOL.
 5	Integer	4*NCOL	The field types, one per column of data. The possible values are:
 0 - Double
 1 - Float
 2 - 4 byte signed integer
 3 - 2 byte signed integer
 4 - 1 byte signed integer
 5 - 4 byte unsigned integer
 6 - 2 byte unsigned integer
 7 - 1 byte unsigned integer
 6	Integer	4	Numbern of tag/value pairs.
 7	Integer	4	The number of characters in the name of the tag. Referred to as TAGNAMELEN.
 8	Char	TAGNAMELEN	The name of the tag.
 9	Integer	4	The number of characters in the value part of the tag/value pair. Referred to as TAGVALLEN.
 10	Char	TAGVALLEN	The value of the tag/value pair.


 BAR SEQ/DATA SECTION HEADER

 11	Integer	4	The number of characters in the name of the sequence. Referred to as SEQNAMELEN.
 12	Char	SEQNAMELEN	The sequence name.
 13	Integer	4	The number of characters in the name of the sequence group.  Referred to as SEQGROUPNAMELEN.  Used only in version 2.0 or greater.
 14	Char	SEQGROUPNAMELEN	The name of the group of which the sequence is a member (for example, often specifies organism).  Referred to as SEQGROUPNAME.  Used only in version 2.0 or greater.
 15	Integer	4	The number of characters in the sequence version string. Referred to as SEQVERLEN.
 16	Char	SEQVERLEN	The sequence version.
 17	Integer	4	Number of tag/value pairs.  Used only in version 2.0 or greater.
 18	Integer	4	The number of characters in the name of the tag. Referred to as TAGNAMELEN.  Used only in version 2.0 or greater.
 19	Char	TAGNAMELEN	The name of the tag.  Used only in version 2.0 or greater.
 20	Integer	4	The number of characters in the value part of the tag/value pair. Referred to as TAGVALLEN.  Used only in version 2.0 or greater.
 21	Char	TAGVALLEN	The value of the tag/value pair.  Used only in version 2.0 or greater.
 22	Integer	4	The number of data points defined in the sequence. Each data point will contain NCOL column values.
 23			The next set of values in the file is the data points for the sequence. Each data point contains NCOL column values. The type, thus the size, of each column is defined above in the field types section.
 *</pre>
 */
public final class BarParser implements AnnotationWriter  {
	static boolean DEBUG_READ = false;
	static boolean DEBUG_SLICE = false;
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

	protected static Map<String,Object> coordset2seqs = new HashMap<String,Object>();

	/**
	 *  Gets a slice from a graph bar file.  The returned GraphSym is intended to
	 *  be used only inside a CompositeGraphSym.
	 */
	public static GraphSymFloat getSlice(String file_name, GenometryModel gmodel, SeqSpan span) throws IOException {	
		Timer tim = new Timer();
		tim.start();
		//boolean USE_RANDOM_ACCESS = false;
		GraphSymFloat graf = null;
		MutableAnnotatedBioSeq aseq = span.getBioSeq();
		String seq_name = aseq.getID();
		int min_base = span.getMin();
		int max_base = span.getMax();
		if (DEBUG_SLICE)  { SingletonGenometryModel.logInfo("trying to get slice, min = " + min_base + ", max = " + max_base); }
		// first check and see if the file is already indexed
		//  if not already indexed, index it (unless it's too small?)
		//
		//  To make slicing functional, still need to change this so coord set is _not_ the file name,
		//     but rather extracted from a field (or set of fields) in the bar file, so can be shared
		//     across bar files that have the exact same base coords
		int[] chunk_mins = (int[])coordset2seqs.get(file_name);

		//    AnnotatedSeqGroup seq_group = gmodel.getSelectedSeqGroup();
		AnnotatedSeqGroup seq_group;
		if (aseq instanceof BioSeq) {
			seq_group = ((BioSeq)aseq).getSeqGroup();
		}
		else {
			seq_group = gmodel.getSelectedSeqGroup();
		}
		if (DEBUG_SLICE) {
			System.out.println("in BarParser.getSlice(), seq_group: " + seq_group.getID() + ", seq: " + aseq.getID());
		}
		if (chunk_mins == null) {
			buildIndex(file_name, file_name, gmodel, seq_group);
			// index??
			chunk_mins = (int[])coordset2seqs.get(file_name);
		}
		int min_index = 0;
		int max_index = 0;
		boolean readToEnd = false;
		if (chunk_mins != null) {
			min_index = Arrays.binarySearch(chunk_mins, min_base);
			max_index = Arrays.binarySearch(chunk_mins, max_base);
			if (min_index < 0) {
				// want min_index to be index of max base coord <= min_base
				min_index = (-min_index -1) - 1;
				if (min_index < 0) { min_index = 0; }
			}
			if (DEBUG_SLICE)  {
				System.out.println("min_index = " + min_index + ", base_pos = " + chunk_mins[min_index]);
				if (min_index > 0) {
					System.out.println("  prev index, base_pos = " + chunk_mins[min_index - 1]); }
				if (min_index < (chunk_mins.length-1))  {
					System.out.println("  next index, base_pos = " + chunk_mins[min_index + 1]);	}
				System.out.println("max_index = " +max_index);
			}

			//did the binary search return a negative number indicating that the requested max_base 
			//   is beyond the last chunk? If so then read to end.
			if (max_index < 0) readToEnd = true;
		}

		DataInput dis = null;
		try {
			/*if (USE_RANDOM_ACCESS) {

			}
			else {*/
				FileInputStream fis = new FileInputStream(new File(file_name));
				dis = new DataInputStream(new BufferedInputStream(fis));
//			}
			BarFileHeader bar_header = parseBarHeader(dis);
			BarSeqHeader seq_header = parseSeqHeader(dis, gmodel, seq_group, bar_header);
			int bytes_per_point = bar_header.bytes_per_point;
			int points_per_index = points_per_chunk;
			int points_to_skip = min_index * points_per_index;
			int bytes_to_skip = points_to_skip * bytes_per_point;
			int points_to_read = 0;
			if (readToEnd) points_to_read = seq_header.data_point_count- points_to_skip;
			else points_to_read = (max_index - min_index) * points_per_index;
			//watch out for cases where max_index - min_index == 0, such as when bar file is very small, say for chrM
			if (points_to_read == 0) points_to_read = seq_header.data_point_count;
			int bytes_to_read = points_to_read * bytes_per_point;
			if (DEBUG_SLICE)  {
				System.out.println("points to skip: "+points_to_skip);
				System.out.println("bytes to skip: " + bytes_to_skip);
				System.out.println("points to read: "+points_to_read);
				System.out.println("bytes to read: " + bytes_to_read);
			}
			while (bytes_to_skip > 0)  {
				int skipped = dis.skipBytes(bytes_to_skip);
				if (DEBUG_SLICE)  { System.out.println("   skipped: " + skipped); }
				if (skipped < 0) {
					if (DEBUG_SLICE)  {System.out.println("end of file reached"); }
					break;
				} // EOF reached
				bytes_to_skip -= skipped;
			}
			byte[] buf = new byte[bytes_to_read];
			dis.readFully(buf);
			if (dis instanceof InputStream)  { ((InputStream)dis).close(); }

			DataInputStream bufstr = new DataInputStream(new ByteArrayInputStream(buf));
			int[] xcoord = new int[points_to_read];
			float[] ycoord = new float[points_to_read];

			int start_index = 0;
			int max_end_index = points_to_read - 1;
			int end_index = max_end_index;

			for (int i=0; i<points_to_read; i++) {
				xcoord[i] = bufstr.readInt();
				ycoord[i] = bufstr.readFloat();
				if ((start_index == 0) && (xcoord[i] >= min_base)) {
					start_index = i;
				}
				if ((end_index == max_end_index) && (xcoord[i] > max_base)) {
					end_index = i-1;
				}
			}
			int graph_point_count = end_index - start_index + 1;
			//      System.out.println("skipped to start base index, base coord = " + start_base_pos);
			if (DEBUG_SLICE) {
				System.out.println("start of byte array, base coord = " + xcoord[0]);
				System.out.println("coords returned: count = " + graph_point_count + ", first = " + xcoord[start_index] + ", last = " + xcoord[end_index]);
			}
			int[] graph_xcoords = new int[graph_point_count];
			float[] graph_ycoords = new float[graph_point_count];
			System.arraycopy(xcoord, start_index, graph_xcoords, 0, graph_point_count);
			System.arraycopy(ycoord, start_index, graph_ycoords, 0, graph_point_count);

			// making GraphSym because bar writer takes GraphSym list as argument
			//      SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
			//      MutableAnnotatedBioSeq gseq = gmodel.getSelectedSeq();
			//      if (gseq == null) { gseq = new SimpleBioSeq(seq_name); }
			//      graf = new GraphSym(graph_xcoords, graph_ycoords, "slice", gseq);

			checkSeqLength(aseq, graph_xcoords);
			// don't need a unique id for this GraphSym, since slices are not added directly as annotations
			//    on BioSeqs, but rather as child annotations of CompositeGraphSyms...
			graf = new GraphSymFloat(graph_xcoords, graph_ycoords, "slice", aseq);
			graf.removeSpan(graf.getSpan(aseq));
			graf.addSpan(span);
			long t1 = tim.read();
			if (DEBUG_SLICE) {
				System.out.println("getSlice() done, points: " + graph_xcoords.length + ", time taken: " + (t1/1000f));
			}
			if (DEBUG_SLICE)  { System.out.println("made graph for slice: " + graf); }

			//fetch tagValues and write properties
			Map<String, String> seq_tagvals = seq_header.tagvals;
			if (seq_tagvals != null && seq_tagvals.size() > 0) {
				copyProps(graf, seq_tagvals);
				//attempt to find and set strand information
				if (seq_tagvals.containsKey("strand")) {
					String strand = seq_tagvals.get("strand");
					if (strand.equals("+")) {
						graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
					}
					if (strand.equals("-")) {
						graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
					}
				}
			}


			// now output bar file slice??

		}
		finally {
			if (dis instanceof InputStream) try { ((InputStream)dis).close(); } catch (Exception e) {}
		}
		return graf;
	}
/**
	public static void main(String[] args) throws Exception {
		String test_file = "c:/data/graph_slice_test/test.bar";
		if (args.length > 0) {
			test_file = args[0];
		}
		//    testFullRead(test_file);

		SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
		AnnotatedSeqGroup seq_group = gmodel.addSeqGroup("Test Seq Group");
		buildIndex(test_file, test_file, gmodel, seq_group);
		BioSeq testseq = new SimpleBioSeq("test", 150200300);
		//    getSlice(test_file, "temp", 0, 2000000);
		getSlice(test_file, gmodel, new SimpleSeqSpan(67000000, 68000000, testseq));
		getSlice(test_file, gmodel, new SimpleSeqSpan(67000000, 68000000, testseq));  // 1 MB
		getSlice(test_file, gmodel, new SimpleSeqSpan(62000000, 65000000, testseq));  // 3 MB
		getSlice(test_file, gmodel, new SimpleSeqSpan(51000000, 56000000, testseq));  // 5 MB
		getSlice(test_file, gmodel, new SimpleSeqSpan(120300400, 130400500, testseq)); // 10 MB
		getSlice(test_file, gmodel, new SimpleSeqSpan(100300400, 110400500, testseq)); // 20 MB
		getSlice(test_file, gmodel, new SimpleSeqSpan(20300400, 60400500, testseq));   // 40 MB
		//    getSlice(test_file, gmodel, new SimpleSeqSpan(158000000, 158400000, testseq));
		//    getSlice(test_file, gmodel, new SimpleSeqSpan(157999267, 158400000, testseq));
		//    getSlice(test_file, gmodel, new SimpleSeqSpan(158400000, 159000000, testseq));
		//    getSlice(test_file, gmodel, new SimpleSeqSpan(158500000, 159000000, testseq));
	}

	public static void testFullRead(String test_file) throws IOException {
		Timer tim = new Timer();
		tim.start();
		DataInputStream dis = null;
		try {
			File fil = new File(test_file);
			int total_bytes = (int)fil.length();
			FileInputStream fis = new FileInputStream(fil);
			dis = new DataInputStream(new BufferedInputStream(fis));
			byte[] buf = new byte[total_bytes];
			dis.readFully(buf);
			dis.close();
		}
		finally {
			try {dis.close();} catch (Exception e) {}
		}
		long time_taken = tim.read();
		SingletonGenometryModel.logInfo("time to fully read file: " + time_taken/1000f);
	}
**/



	/**
	 *  Builds an index for each sequence in the BAR file.
	 *  <pre>
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
	 * </pre>
	 */
	public static void buildIndex(String file_name, String coord_set_id, GenometryModel gmodel, AnnotatedSeqGroup seq_group)
		throws IOException {
		Timer tim = new Timer();
		tim.start();
		// builds an index per sequence in the bar file
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(file_name))));
			BarFileHeader file_header = parseBarHeader(dis);
			int[] val_types = file_header.val_types;
			int bytes_per_point = file_header.bytes_per_point;
			BarSeqHeader seq_header = parseSeqHeader(dis, gmodel, seq_group, file_header);
			int total_points = seq_header.data_point_count;

			int point_count = 0;
			int chunk_count = 0;
			// adding one because indexing _start_ of chunk, so also have partial chunk at end??
			int total_chunks = (total_points / points_per_chunk) + 1;
			int[] chunk_mins = new int[total_chunks];

			if (DEBUG_READ) {
				System.out.println("total points: " + total_points);
				System.out.println("bytes per data point: " + bytes_per_point);
				System.out.println("points_per_chunk: " + points_per_chunk);
				System.out.println("expected chunk count: " + total_chunks);
			}

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
			if (DEBUG_READ) {
				System.out.println("chunk count: " + chunk_count);
				System.out.println("expected chunk count: " + total_chunks);
			}

			//      coordset2indexmap.put(coord_set_id, indexmap);
			//      indexmap.put(seqid, chunk_mins);
			coordset2seqs.put(coord_set_id, chunk_mins);

			dis.close();
		}
		finally {
			try {dis.close();} catch (Exception e) {}
		}
		long index_time = tim.read();
		if (DEBUG_READ) {
			System.out.println("time to index: " + index_time/1000f);
			System.out.println(" ");
		}
	}

	/** Parse a file in BAR format. */
	public static List<GraphSym> parse(InputStream istr, GenometryModel gmodel, AnnotatedSeqGroup seq_group, String stream_name)
		throws IOException  {
		return parse(istr, gmodel, seq_group, stream_name, true);
	}

	/** Parse a file in BAR format. */
	public static List<GraphSym> parse(InputStream istr, GenometryModel gmodel,
			AnnotatedSeqGroup default_seq_group, String stream_name,
			boolean ensure_unique_id)
		throws IOException {		
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		List<GraphSym> graphs = null;

		Timer tim = new Timer();
		tim.start();
		try {
			if (istr instanceof BufferedInputStream) {
				bis = (BufferedInputStream)istr;
			}
			else {
				bis = new BufferedInputStream(istr);
			}
			dis = new DataInputStream(bis);
			BarFileHeader bar_header = parseBarHeader(dis);

			boolean bar2 = (bar_header.version >= 2.0f);
			int total_seqs = bar_header.seq_count;
			int[] val_types = bar_header.val_types;
			int vals_per_point = bar_header.vals_per_point;
			Map<String,String> file_tagvals = bar_header.tagvals;

			String graph_id = "unknown";
			if (stream_name != null) { graph_id = stream_name; }
			if (file_tagvals.get("file_type") != null) {
				graph_id += ":" + file_tagvals.get("file_type");
			}
			for (int k=0; k<total_seqs; k++) {
				BarSeqHeader seq_header = parseSeqHeader(dis, gmodel, default_seq_group, bar_header);
				int total_points = seq_header.data_point_count;
				Map<String,String> seq_tagvals = seq_header.tagvals;
				//      MutableAnnotatedBioSeq seq = seq_header.aseq;
				BioSeq seq = (BioSeq)seq_header.aseq;
				if (vals_per_point == 1) {
					throw new IOException("PARSING FOR BAR FILES WITH 1 VALUE PER POINT NOT YET IMPLEMENTED");
				}
				else if (vals_per_point == 2) {
					if (val_types[0] == BYTE4_SIGNED_INT &&
							val_types[1] == BYTE4_FLOAT) {
						if (graphs == null) { graphs = new ArrayList<GraphSym>(); }
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
						if (DEBUG_READ)  {
							System.out.println("^^^ creating GraphSym in BarParser, group = " + seq.getSeqGroup().getID() +
									", seq = " + seq.getID());
							System.out.println("      graph id: " + graph_id);
						}
						if (ensure_unique_id) { graph_id = AnnotatedSeqGroup.getUniqueGraphID(graph_id, seq); }
						checkSeqLength(seq, xcoords);
						GraphSymFloat graf = new GraphSymFloat(xcoords, ycoords, graph_id, seq);
						//          graf.setProperties(new HashMap(file_tagvals));
						copyProps(graf, file_tagvals);
						if (bar2)  { copyProps(graf, seq_tagvals); }
						//	  graf.setProperty("method", graph_id);
						//          System.out.println("done reading graph data: " + graf);

						//attempt to find and set strand information
						if (seq_tagvals.containsKey("strand")) {						
							String strand = seq_tagvals.get("strand");
							if (strand.equals("+")) graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
							if (strand.equals("-")) graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
						}

						graphs.add(graf);
							}
					else {
						throw new IOException("Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
				}
				else if (vals_per_point == 3) {
					// if three values per point, assuming #1 is int base coord, #2 is Pm score, #3 is Mm score
					if (val_types[0] == BYTE4_SIGNED_INT &&
							val_types[1] == BYTE4_FLOAT &&
							val_types[2] == BYTE4_FLOAT) {
						if (graphs == null) { graphs = new ArrayList<GraphSym>(); }
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
						String pm_name = graph_id + " : pm";
						String mm_name = graph_id + " : mm";
						if (ensure_unique_id) {
							pm_name = AnnotatedSeqGroup.getUniqueGraphID(pm_name, seq);
							mm_name = AnnotatedSeqGroup.getUniqueGraphID(mm_name, seq);
						}
						checkSeqLength(seq, xcoords);
						GraphSymFloat pm_graf =
							new GraphSymFloat(xcoords, ycoords, pm_name, seq);
						GraphSymFloat mm_graf =
							new GraphSymFloat(xcoords, zcoords, mm_name, seq);
						//          mm_graf.setProperties(new HashMap(file_tagvals));
						//          pm_graf.setProperties(new HashMap(file_tagvals));
						copyProps(pm_graf, file_tagvals);
						copyProps(mm_graf, file_tagvals);
						if (bar2)  {
							copyProps(pm_graf, seq_tagvals);
							copyProps(mm_graf, seq_tagvals);
						}
						if (DEBUG_READ) {
							System.out.println("done reading graph data: ");
							System.out.println("pmgraf, yval = column1: " + pm_graf);
							System.out.println("mmgraf, yval = column2: " + mm_graf);
						}
						pm_graf.setProperty("probetype", "PM (perfect match)");
						mm_graf.setProperty("probetype", "MM (mismatch)");
						graphs.add(pm_graf);
						graphs.add(mm_graf);
							}
					else {
						throw new IOException("Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
				}
			}
			long t1 = tim.read();
			SingletonGenometryModel.getLogger().fine("bar load time: " + t1/1000f);
		}
		finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
		}

		return graphs;
	}

	/*public static void writeTagVal(DataOutput dos, String tag, String val)
	  throws IOException  {
  dos.writeInt(tag.length());
  dos.writeBytes(tag);
  dos.writeInt(val.length());
  dos.writeBytes(val);
  }*/

	private static final HashMap<String,String> readTagValPairs(DataInput dis, int pair_count) throws IOException  {
		HashMap<String,String> tvpairs = new HashMap<String,String>(pair_count);
		if (DEBUG_READ) { System.out.println("reading tagvals: "); }
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

	public static void writeBarHeader(DataOutput dos) throws IOException {

	}

	static BarFileHeader parseBarHeader(DataInput dis) throws IOException {
		try {
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
				//        System.out.println("header: " + headstr);
				System.out.println("bar version: " + version);
				System.out.println("total seqs: " + total_seqs);
				System.out.println("vals per point: " + vals_per_point);
			}
			int[] val_types = new int[vals_per_point];
			for (int i=0; i<vals_per_point; i++) {
				val_types[i] = dis.readInt();
				if (DEBUG_READ)  { System.out.println("val type for column " + i + ": " + valstrings[val_types[i]]); }
			}
			int tvcount = dis.readInt();
			if (DEBUG_READ) { System.out.println("file tagval count: " + tvcount); }
			HashMap<String,String> file_tagvals = readTagValPairs(dis, tvcount);
			BarFileHeader header = new BarFileHeader(version, total_seqs, val_types, file_tagvals);
			return header;
		} catch (Throwable t) {
			// Catch out-of-memory errors, and other errors caused by poorly-formatted headers.
			IOException ioe = new IOException("Could not parse bar-file header.");
			ioe.initCause(t);
			throw ioe;
		}
	}

	static BarSeqHeader parseSeqHeader(DataInput dis, GenometryModel gmodel, AnnotatedSeqGroup default_seq_group, BarFileHeader file_header) throws IOException {
		int namelength = dis.readInt();
		//      String
		byte[] barray = new byte[namelength];
		dis.readFully(barray);
		String seqname = new String(barray);
		if (DEBUG_READ) {
			System.out.println("seq: " + seqname);
		}

		String groupname = null;
		boolean bar2 = (file_header.version >= 2.0f);
		if (bar2) {
			int grouplength = dis.readInt();
			barray = new byte[grouplength];
			dis.readFully(barray);
			groupname = new String(barray);
			if (DEBUG_READ) {
				System.out.println("group length: " + grouplength + ", group: " + groupname);
			}
		}

		int verslength = dis.readInt();
		barray = new byte[verslength];
		dis.readFully(barray);
		String seqversion = new String(barray);
		if (DEBUG_READ) {
			System.out.println("version length: " + verslength + ", version: " + seqversion);
		}

		// hack to extract seq version and seq name from seqname field for bar files that were made
		//   with the version and name concatenated (with ";" separator) into the seqname field
		int sc_pos = seqname.lastIndexOf(';');
		String orig_seqname = seqname;
		if (sc_pos >= 0) {
			seqversion = seqname.substring(0, sc_pos);
			seqname = seqname.substring(sc_pos + 1);
			if (DEBUG_READ) {
				System.out.println("seqname = " + seqname + ", seqversion = " + seqversion);
			}
		}

		HashMap<String, String> seq_tagvals = null;
		if (bar2) {
			int seq_tagval_count = dis.readInt();
			if (DEBUG_READ) {
				System.out.println("seq tagval count: " + seq_tagval_count);
			}
			seq_tagvals = readTagValPairs(dis, seq_tagval_count);
		}

		int total_points = dis.readInt();
		if (DEBUG_READ) {
			System.out.println("   seqname = " + seqname + ", version = " + seqversion +
							", group = " + groupname +
							", data points = " + total_points);
		}
		//      System.out.println("total data points for graph " + k + ": " + total_points);
		MutableAnnotatedBioSeq seq = null;

		AnnotatedSeqGroup seq_group = getSeqGroup(groupname, seqversion, gmodel, default_seq_group);
		seq = determineSeq(seq_group, seqname, seq, orig_seqname, seqversion, groupname, bar2);
		BarSeqHeader seq_header = new BarSeqHeader(seq, total_points, seq_tagvals);
		return seq_header;
	}


	private static MutableAnnotatedBioSeq determineSeq(AnnotatedSeqGroup seq_group, String seqname, MutableAnnotatedBioSeq seq, String orig_seqname, String seqversion, String groupname, boolean bar2) {
		// trying standard AnnotatedSeqGroup seq id resolution first
		seq = seq_group.getSeq(seqname);
		if (seq == null) {
			seq = seq_group.getSeq(orig_seqname);
		}
		// if standard AnnotatedSeqGroup seq id resolution doesn't work, try old technique
		//    (hopefully can eliminate this soon)
		if (seq == null) {
			SynonymLookup lookup = SynonymLookup.getDefaultLookup();
			//TODO: Convert this to the standard way of getting synomous sequences,
			// but we may have to check for extra bar-specific synonyms involving seq group and version
			for (BioSeq testseq : seq_group.getSeqList()) {
				// testing both seq id and version id (if version id is available)
				if (lookup.isSynonym(testseq.getID(), seqname)) {
					// GAH 1-23-2005
					// need to ensure that if bar2 format, the seq group is also a synonym!
					// GAH 7-7-2005
					//    but now there's some confusion about seqversion vs seqgroup, so try all three possibilities:
					//      groupname
					//      seqversion
					//      groupname + ":" + seqversion
					if ((seqversion == null && groupname == null) ||
									(((seqversion == null) || seqversion.equals("")) && ((groupname == null) || groupname.equals("")))) {
						seq = testseq;
						break;
					} else {
						String test_version = testseq.getVersion();
						if ((lookup.isSynonym(test_version, seqversion)) || (lookup.isSynonym(test_version, groupname)) || (lookup.isSynonym(test_version, groupname + ":" + seqversion))) {
							if (DEBUG_READ) {
								System.out.println("found synonymn");
							}
							seq = testseq;
							break;
						}
					}
				}
			}
		}
		if (seq == null) {
			if (bar2 && groupname != null) {
				seqversion = groupname + ":" + seqversion;
			}
			//System.out.println("seq not found, creating new seq:  name = " + seqname + ", version = " + seqversion);
			//        seq = seq_group.addSeq(seqname, 500000000);
			seq = seq_group.addSeq(seqname, 1000);
		}
		if (DEBUG_READ) {
			System.out.println("seq: " + seq);
		}
		return seq;
	}


	/**
	 *  if group and version are null/blank, assume default_seq_group is correct.
	 *  otherwise try an match with an existing AnnotatedSeqGroup
	 *  if existing AnnotatedSeqGroup can't be found, create a new one?
	 */
	public static AnnotatedSeqGroup getSeqGroup(String groupname, String version, GenometryModel gmodel, AnnotatedSeqGroup default_seq_group) {
		AnnotatedSeqGroup group = null;
		if (((version == null)  || version.equals("")) &&
				((groupname == null) || groupname.equals("") )) {
			group = default_seq_group;
				}
		else {
			if (groupname != null && version != null) {
				group = gmodel.getSeqGroup(groupname + ":" + version);
			}
			if (group == null && groupname != null) {
				group = gmodel.getSeqGroup(groupname);
			}
			if (group == null && version != null) {
				group = gmodel.getSeqGroup(version);
			}
			// no group found, so create a new one
			if (group == null)  {
				String new_group_name;
				if (groupname != null && groupname.length()>0 && version != null && version.length()>0) {
					new_group_name = groupname + ":" + version;
				}
				else if (version == null || version.length()==0) {
					new_group_name = groupname;
				}
				else { new_group_name = version; }
				if (DEBUG_READ) {
					System.out.println("group not found, creating new seq group: " + new_group_name);
				}
				group = gmodel.addSeqGroup(new_group_name);
			}

			if (gmodel.getSelectedSeqGroup() != group) {
				// This is necessary to make sure new groups get added to the DataLoadView.
				// maybe need a SeqGroupModifiedEvent class instead.
				gmodel.setSelectedSeqGroup(group);
			}
		}
		return group;
	}



	/**
	 * Writes bar format.
	 * Assumes syms size is one and single sym is a GraphSym with same MutableAnnotatedBioSeq as seq
	 */
	public boolean writeAnnotations(java.util.Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
			String type, OutputStream ostr) {
		boolean success = false;
		BufferedOutputStream bos = new BufferedOutputStream(ostr);
		DataOutputStream dos = new DataOutputStream(bos);

		// for now assume just outputting one graph?
		Iterator iter = syms.iterator();
		GraphSym graf = (GraphSym) iter.next();
		// should check to make sure seq is same as graf.getGraphSeq()??

		AnnotatedSeqGroup group = null;
		if (seq instanceof BioSeq) {
			group = ((BioSeq)seq).getSeqGroup();
		}
		String groupid = group.getID();
		String version = groupid;
		String seqid = seq.getID();

		try {
			dos.writeBytes("barr\r\n\032\n");  // char  "barr\r\n\032\n"
			dos.writeFloat(2.0f);       // version of bar format = 2.0
			dos.writeInt(1);  // number of seq data sections in file -- if single graph, then 1
			dos.writeInt(2);  // number of columns (dimensions) per data point
			dos.writeInt(BYTE4_SIGNED_INT);  // int  first column/dimension type ==> 4-byte signed int
			dos.writeInt(BYTE4_FLOAT);  // int  second column/dimension type ==> 4-byte float

			// should write out all properties from group and/or graphs as tag/vals?  For now just saying no tag/vals
			dos.writeInt(0);

			// assuming one graph for now, so only one seq section
			dos.writeInt(seqid.length());
			dos.writeBytes(seqid);
			dos.writeInt(groupid.length());
			dos.writeBytes(groupid);
			dos.writeInt(version.length());
			dos.writeBytes(version);

			//write out all properties from seq and/or graphs as tag/vals
			writeTagValuePairs(dos,graf.getProperties());

			int[] xcoords = graf.getGraphXCoords();
			//float[] ycoords = (float[]) graf.getGraphYCoords();
			int total_points = xcoords.length;

			dos.writeInt(total_points);
			for (int i=0; i<total_points; i++) {
				dos.writeInt(xcoords[i]);
				dos.writeFloat(graf.getGraphYCoord(i));
			}
			dos.close();  // or should responsibility for closing stream be left to the caller??
			success = true;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}

		System.out.println("wrote .bar file to stream");
		return success;
	}

	public static void writeTagValuePairs(DataOutputStream dos,Map<String,Object> tagValuePairs) throws IOException{
		//any tag value pairs?
		if (tagValuePairs == null || tagValuePairs.size() == 0) dos.writeInt(0);
		else {
			//write number of pairs
			dos.writeInt(tagValuePairs.size());
			//write tag values preceded by their respective lengths
			Iterator<String> it = tagValuePairs.keySet().iterator();
			while (it.hasNext()){
				String tag = it.next();
				dos.writeInt(tag.length());
				dos.writeBytes(tag);
				String value = tagValuePairs.get(tag).toString();
				dos.writeInt(value.length());
				dos.writeBytes(value);
			}
		}
	}

	public String getMimeType() { return "binary/bar"; }

	public static void checkSeqLength(MutableAnnotatedBioSeq seq, int[] xcoords) {
		if (seq instanceof MutableAnnotatedBioSeq) {
			MutableAnnotatedBioSeq aseq = seq;
			int xcount = xcoords.length;
			if (xcount > 0 && (xcoords[xcount-1] > aseq.getLength())) {
				aseq.setLength(xcoords[xcount-1]);
			}
		}
	}


}
class BarSeqHeader {
	MutableAnnotatedBioSeq aseq;
	int data_point_count;
	Map<String,String> tagvals;

	public BarSeqHeader(MutableAnnotatedBioSeq seq, int data_points, Map<String,String> tagvals)  {
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
	Map<String,String> tagvals;

	public BarFileHeader(float version, int seq_count, int[] val_types, Map<String,String> tagvals) {
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

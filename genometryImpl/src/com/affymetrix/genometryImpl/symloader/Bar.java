package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.SeqSpan;
import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.Timer;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public final class Bar extends SymLoader {

	private static final boolean DEBUG = false;

	/** 8-byte floating-point.  Names of the other data-type constants can be interpreted similarly. */
	private static final int BYTE4_FLOAT = 1;
	private static final int BYTE4_SIGNED_INT = 2;
	static final int[] bytes_per_val = {
		8, // BYTE8_FLOAT
		4, // BYTE4_FLOAT
		4, // BYTE4_SIGNED_INT
		2, // BYTE2_SIGNED_INT
		1, // BYTE1_SIGNED_INT
		4, // BYTE4_UNSIGNED_INT
		2, // BYTE2_UNSIGNED_INT
		1 // BYTE1_UNSIGNED_INT
	};
	private static final String[] valstrings = {"BYTE8_FLOAT", "BYTE4_FLOAT",
		"BYTE4_SIGNED_INT", "BYTE2_SIGNED_INT", "BYTE1_SIGNED_INT",
		"BYTE4_UNSIGNED_INT", "BYTE2_UNSIGNED_INT", "BYTE1_UNSIGNED_INT"};
	/**
	 *  For indexing of base coord sets, how many point to compress into single index entry
	 */
	private static final int points_per_chunk = 1024;

	private final File f;
	private final BioSeq seq;
	private int[] chunk_mins;

	public Bar(URI uri, BioSeq seq) {
		super(uri);
		this.f = LocalUrlCacher.convertURIToFile(uri);
		this.seq = seq;
	}

	/**
	 *  Gets a slice from a graph bar file.  The returned GraphSym is intended to
	 *  be used only inside a CompositeGraphSym.
	 */
	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		Timer tim = new Timer();
		tim.start();
		BioSeq aseq = span.getBioSeq();
		int min_base = span.getMin();
		int max_base = span.getMax();
		List<GraphSym> graphs = new ArrayList<GraphSym>();

		// first check and see if the file is already indexed
		//  if not already indexed, index it (unless it's too small?)
		//
		//  To make slicing functional, still need to change this so coord set is _not_ the file name,
		//     but rather extracted from a field (or set of fields) in the bar file, so can be shared
		//     across bar files that have the exact same base coords

		if (aseq == null) {
			Logger.getLogger(Bar.class.getName()).log(
					Level.WARNING, "Chromosome was null");
			return graphs;
		}
		if (aseq != this.seq) {
			Logger.getLogger(Bar.class.getName()).log(
					Level.WARNING, "Chromosomes " + aseq.getID() + " and " + seq.getID() + " do not match.");
			return graphs;
		}

		if (!this.isInitialized) {
			if (!buildIndex()) {
				return graphs;	// something went wrong; return empty list and don't init
			}
			super.init();
		}
		int min_index = 0;
		int max_index = 0;
		boolean readToEnd = false;
		if (chunk_mins != null) {
			min_index = Arrays.binarySearch(chunk_mins, min_base);
			max_index = Arrays.binarySearch(chunk_mins, max_base);
			if (min_index < 0) {
				// want min_index to be index of max base coord <= min_base
				min_index = (-min_index - 1) - 1;
				if (min_index < 0) {
					min_index = 0;
				}
			}
			if (DEBUG) {
				System.out.println("min_index = " + min_index + ", base_pos = " + chunk_mins[min_index]);
				if (min_index > 0) {
					System.out.println("  prev index, base_pos = " + chunk_mins[min_index - 1]);
				}
				if (min_index < (chunk_mins.length - 1)) {
					System.out.println("  next index, base_pos = " + chunk_mins[min_index + 1]);
				}
				System.out.println("max_index = " + max_index);
			}

			//did the binary search return a negative number indicating that the requested max_base 
			//   is beyond the last chunk? If so then read to end.
			if (max_index < 0) {
				readToEnd = true;
			}
		}
		
		GraphSym graf = constructGraf(
				min_index, readToEnd, max_index, min_base, max_base, aseq, span, tim);
		if (graf != null) {
			graphs.add(graf);
		}
		return graphs;
	}

	private GraphSym constructGraf(
			int min_index,
			boolean readToEnd, int max_index, int min_base, int max_base, BioSeq aseq, SeqSpan span, Timer tim)
			{
		GraphSym graf = null;
		DataInputStream dis = null;
		try {
			FileInputStream fis = new FileInputStream(this.f);
			dis = new DataInputStream(new BufferedInputStream(fis));
			BarFileHeader bar_header = parseBarHeader(dis);
			BarSeqHeader seq_header = parseSeqHeader(dis, bar_header);
			int bytes_per_point = bar_header.bytes_per_point;
			int points_per_index = points_per_chunk;
			int points_to_skip = min_index * points_per_index;
			int bytes_to_skip = points_to_skip * bytes_per_point;
			int points_to_read = 0;
			if (readToEnd) {
				points_to_read = seq_header.data_point_count - points_to_skip;
			} else {
				points_to_read = (max_index - min_index) * points_per_index;
			}
			//watch out for cases where max_index - min_index == 0, such as when bar file is very small, say for chrM
			if (points_to_read == 0) {
				points_to_read = seq_header.data_point_count;
			}
			int bytes_to_read = points_to_read * bytes_per_point;
			if (DEBUG) {
				System.out.println("points to skip: " + points_to_skip);
				System.out.println("bytes to skip: " + bytes_to_skip);
				System.out.println("points to read: " + points_to_read);
				System.out.println("bytes to read: " + bytes_to_read);
			}
			skipBytes(bytes_to_skip, dis);
			byte[] buf = new byte[bytes_to_read];
			dis.readFully(buf);
			((InputStream) dis).close();
			DataInputStream bufstr = new DataInputStream(new ByteArrayInputStream(buf));
			int[] xcoord = new int[points_to_read];
			float[] ycoord = new float[points_to_read];
			int start_index = 0;
			int max_end_index = points_to_read - 1;
			int end_index = max_end_index;
			for (int i = 0; i < points_to_read; i++) {
				xcoord[i] = bufstr.readInt();
				ycoord[i] = bufstr.readFloat();
				if ((start_index == 0) && (xcoord[i] >= min_base)) {
					start_index = i;
				}
				if ((end_index == max_end_index) && (xcoord[i] > max_base)) {
					end_index = i - 1;
				}
			}
			int graph_point_count = end_index - start_index + 1;
			if (DEBUG) {
				System.out.println("start of byte array, base coord = " + xcoord[0]);
				System.out.println("coords returned: count = " + graph_point_count + ", first = " + xcoord[start_index] + ", last = " + xcoord[end_index]);
			}
			int[] graph_xcoords = new int[graph_point_count];
			float[] graph_ycoords = new float[graph_point_count];
			System.arraycopy(xcoord, start_index, graph_xcoords, 0, graph_point_count);
			System.arraycopy(ycoord, start_index, graph_ycoords, 0, graph_point_count);
			checkSeqLength(aseq, graph_xcoords);
			// don't need a unique id for this GraphSym, since slices are not added directly as annotations
			//    on BioSeqs, but rather as child annotations of CompositeGraphSyms...
			graf = new GraphSym(graph_xcoords, graph_ycoords, "slice", aseq);
			graf.removeSpan(graf.getSpan(aseq));
			graf.addSpan(span);
			long t1 = tim.read();
			if (DEBUG) {
				System.out.println("getSlice() done, points: " + graph_xcoords.length + ", time taken: " + (t1 / 1000f));
				System.out.println("made graph for slice: " + graf);
			}
			setTagValues(seq_header, graf);
			// now output bar file slice??
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose((InputStream) dis);
		}
		return graf;
	}

	


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
	public boolean buildIndex() {
		Timer tim = new Timer();
		tim.start();
		// builds an index per sequence in the bar file
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(this.f)));
			BarFileHeader file_header = parseBarHeader(dis);
			int bytes_per_point = file_header.bytes_per_point;
			BarSeqHeader seq_header = parseSeqHeader(dis, file_header);
			int total_points = seq_header.data_point_count;

			int point_count = 0;
			int chunk_count = 0;
			// adding one because indexing _start_ of chunk, so also have partial chunk at end??
			int total_chunks = (total_points / points_per_chunk) + 1;
			chunk_mins = new int[total_chunks];

			if (DEBUG) {
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
				if (DEBUG) {
					System.out.println("chunk: " + chunk_count + ", index: " + point_count + ",  start base: " + base_pos);
				}
				if (skipBytes(skip_offset, dis)) {
					break CHUNK_LOOP;
				}
				point_count += points_per_chunk;
				if (DEBUG) {
					System.out.println("  point count: " + point_count);
				}
				chunk_count++;
			}
			// just making sure edge case doesn't mess things up...
			if (chunk_mins[total_chunks - 1] == 0) {
				chunk_mins[total_chunks - 1] = seq_header.aseq.getLength();
			}
			if (DEBUG) {
				System.out.println("chunk count: " + chunk_count);
				System.out.println("expected chunk count: " + total_chunks);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		} finally {
			GeneralUtils.safeClose(dis);
		}
		if (DEBUG) {
			long index_time = tim.read();
			System.out.println("time to index: " + index_time / 1000f);
			System.out.println(" ");
		}
		return true;
	}

	private static void setTagValues(BarSeqHeader seq_header, GraphSym graf) {
		Map<String, String> seq_tagvals = seq_header.tagvals;
		if (seq_tagvals != null && seq_tagvals.size() > 0) {
			copyProps(graf, seq_tagvals);
			setStrandProp(seq_tagvals, graf);
		}
	}

	//attempt to find and set strand information
	private static void setStrandProp(Map<String, String> seq_tagvals, GraphSym graf) {
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

	private static boolean skipBytes(int bytes_to_skip, DataInputStream dis) throws IOException {
		while (bytes_to_skip > 0) {
			int skipped = (int) dis.skip(bytes_to_skip);
			if (DEBUG) {
				System.out.println("   skipped: " + skipped);
			}
			if (skipped < 0) {
				if (DEBUG) {
					System.out.println("end of file reached");
				}
				return true;
			} // EOF reached
			bytes_to_skip -= skipped;
		}
		return false;
	}

	/** Parse a file in BAR format. */
	public List<GraphSym> parse(InputStream istr, String stream_name,
			boolean ensure_unique_id)
			throws IOException {
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		List<GraphSym> graphs = new ArrayList<GraphSym>();

		Timer tim = new Timer();
		tim.start();
		try {
			if (istr instanceof BufferedInputStream) {
				bis = (BufferedInputStream) istr;
			} else {
				bis = new BufferedInputStream(istr);
			}
			dis = new DataInputStream(bis);
			BarFileHeader bar_header = parseBarHeader(dis);

			boolean bar2 = (bar_header.version >= 2.0f);
			int total_seqs = bar_header.seq_count;
			int[] val_types = bar_header.val_types;
			int vals_per_point = bar_header.vals_per_point;
			Map<String, String> file_tagvals = bar_header.tagvals;

			String graph_id = "unknown";
			if (stream_name != null) {
				graph_id = stream_name;
			}
			if (file_tagvals.get("file_type") != null) {
				graph_id += ":" + file_tagvals.get("file_type");
			}
			for (int k = 0; k < total_seqs; k++) {
				
				if (vals_per_point == 1) {
					throw new IOException("PARSING FOR BAR FILES WITH 1 VALUE PER POINT NOT YET IMPLEMENTED");
				}
				if (vals_per_point == 2) {
					if (val_types[0] != BYTE4_SIGNED_INT || val_types[1] != BYTE4_FLOAT) {
						throw new IOException("Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
					handle2ValPerPoint(bar_header, dis, graph_id, ensure_unique_id, file_tagvals, bar2, graphs);
				} else if (vals_per_point == 3) {
					// if three values per point, assuming #1 is int base coord, #2 is Pm score, #3 is Mm score
					if (val_types[0] != BYTE4_SIGNED_INT || val_types[1] != BYTE4_FLOAT || val_types[2] != BYTE4_FLOAT) {
						throw new IOException("Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
					handle3ValPerPoint(bar_header, dis, graph_id, ensure_unique_id, file_tagvals, bar2, graphs);
				}
			}
			long t1 = tim.read();
			GenometryModel.getLogger().fine("bar load time: " + t1 / 1000f);
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
		}

		return graphs;
	}



	private void handle2ValPerPoint(
			BarFileHeader bar_header,
			DataInputStream dis, String graph_id, boolean ensure_unique_id,
			Map<String, String> file_tagvals, boolean bar2, List<GraphSym> graphs)
			throws IOException {
		BarSeqHeader seq_header = parseSeqHeader(dis, bar_header);
				int total_points = seq_header.data_point_count;
				Map<String, String> seq_tagvals = seq_header.tagvals;
				// TODO: handle case where there are multiple chromosomes in single file.
				//BioSeq seq = seq_header.aseq;
		int[] xcoords = new int[total_points];
		float[] ycoords = new float[total_points];
		float prev_max_xcoord = -1;
		boolean sort_reported = false;
		for (int i = 0; i < total_points; i++) {
			int col0 = dis.readInt();
			float col1 = dis.readFloat();
			if (col0 < prev_max_xcoord && (!sort_reported)) {
				if (DEBUG) {
					System.out.println("WARNING!! not sorted by ascending xcoord");
				}
				sort_reported = true;
			}
			prev_max_xcoord = col0;
			xcoords[i] = col0;
			ycoords[i] = col1;
			if (DEBUG && i < 100) {
				System.out.println("Data[" + i + "]:\t" + col0 + "\t" + col1);
			}
		}
		if (DEBUG) {
			System.out.println("^^^ creating GraphSym in BarParser, group = " + seq.getSeqGroup().getID() + ", seq = " + seq.getID());
			System.out.println("      graph id: " + graph_id);
		}
		if (ensure_unique_id) {
			graph_id = AnnotatedSeqGroup.getUniqueGraphID(graph_id, seq);
		}
		checkSeqLength(seq, xcoords);
		GraphSym graf = new GraphSym(xcoords, ycoords, graph_id, seq);
		copyProps(graf, file_tagvals);
		if (bar2) {
			copyProps(graf, seq_tagvals);
		}
		setStrandProp(seq_tagvals, graf);
		graphs.add(graf);
	}


	private void handle3ValPerPoint(
			BarFileHeader bar_header,
			DataInputStream dis, String graph_id, boolean ensure_unique_id,
			Map<String, String> file_tagvals, boolean bar2, List<GraphSym> graphs)
			throws IOException {
		BarSeqHeader seq_header = parseSeqHeader(dis, bar_header);
				int total_points = seq_header.data_point_count;
				Map<String, String> seq_tagvals = seq_header.tagvals;
				// TODO: handle case where there are multiple chromosomes in single file.
				//BioSeq seq = seq_header.aseq;
		int[] xcoords = new int[total_points];
		float[] ycoords = new float[total_points];
		float[] zcoords = new float[total_points];
		for (int i = 0; i < total_points; i++) {
			int col0 = dis.readInt();
			float col1 = dis.readFloat();
			float col2 = dis.readFloat();
			xcoords[i] = col0;
			ycoords[i] = col1;
			zcoords[i] = col2;
			if (DEBUG && i < 100) {
				System.out.println("Data[" + i + "]:\t" + col0 + "\t" + col1 + "\t" + col2);
			}
		}
		String pm_name = graph_id + " : pm";
		String mm_name = graph_id + " : mm";
		if (ensure_unique_id) {
			pm_name = AnnotatedSeqGroup.getUniqueGraphID(pm_name, seq);
			mm_name = AnnotatedSeqGroup.getUniqueGraphID(mm_name, seq);
		}
		checkSeqLength(seq, xcoords);
		GraphSym pm_graf = new GraphSym(xcoords, ycoords, pm_name, seq);
		GraphSym mm_graf = new GraphSym(xcoords, zcoords, mm_name, seq);
		copyProps(pm_graf, file_tagvals);
		copyProps(mm_graf, file_tagvals);
		if (bar2) {
			copyProps(pm_graf, seq_tagvals);
			copyProps(mm_graf, seq_tagvals);
		}
		if (DEBUG) {
			System.out.println("done reading graph data: ");
			System.out.println("pmgraf, yval = column1: " + pm_graf);
			System.out.println("mmgraf, yval = column2: " + mm_graf);
		}
		pm_graf.setProperty("probetype", "PM (perfect match)");
		mm_graf.setProperty("probetype", "MM (mismatch)");
		graphs.add(pm_graf);
		graphs.add(mm_graf);
	}



	private static final HashMap<String, String> readTagValPairs(DataInput dis, int pair_count) throws IOException {
		HashMap<String, String> tvpairs = new HashMap<String, String>(pair_count);
		if (DEBUG) {
			System.out.println("reading tagvals: ");
		}
		for (int i = 0; i < pair_count; i++) {
			int taglength = dis.readInt();
			byte[] barray = new byte[taglength];
			dis.readFully(barray);
			String tag = new String(barray);
			// maybe should intern?
			int vallength = dis.readInt();
			barray = new byte[vallength];
			dis.readFully(barray);
			String val = new String(barray);
			tvpairs.put(tag, val);
			if (DEBUG) {
				System.out.println("    tag = " + tag + ", val = " + val);
			}
		}
		return tvpairs;
	}

	private static void copyProps(GraphSym graf, Map<String, String> tagvals) {
		if (tagvals == null) {
			return;
		}
		for (Map.Entry<String, String> tagval : tagvals.entrySet()) {
			graf.setProperty(tagval.getKey(), tagval.getValue());
		}
	}

	static BarFileHeader parseBarHeader(DataInput dis) throws IOException {
		try {
			// READING HEADER
			byte[] headbytes = new byte[8];
			dis.readFully(headbytes);
			float version = dis.readFloat();       // int  #rows in data section (nrow)
			int total_seqs = dis.readInt();
			int vals_per_point = dis.readInt(); // int  #columns in data section (ncol)
			if (DEBUG) {
				System.out.println("bar version: " + version);
				System.out.println("total seqs: " + total_seqs);
				System.out.println("vals per point: " + vals_per_point);
			}
			int[] val_types = new int[vals_per_point];
			for (int i = 0; i < vals_per_point; i++) {
				val_types[i] = dis.readInt();
				if (DEBUG) {
					System.out.println("val type for column " + i + ": " + valstrings[val_types[i]]);
				}
			}
			int tvcount = dis.readInt();
			if (DEBUG) {
				System.out.println("file tagval count: " + tvcount);
			}
			HashMap<String, String> file_tagvals = readTagValPairs(dis, tvcount);
			BarFileHeader header = new BarFileHeader(version, total_seqs, val_types, file_tagvals);
			return header;
		} catch (Throwable t) {
			// Catch out-of-memory errors, and other errors caused by poorly-formatted headers.
			IOException ioe = new IOException("Could not parse bar-file header.");
			ioe.initCause(t);
			throw ioe;
		}
	}

	private BarSeqHeader parseSeqHeader(DataInput dis, BarFileHeader file_header) throws IOException {
		int namelength = dis.readInt();
		byte[] barray = new byte[namelength];
		dis.readFully(barray);
		String seqname = new String(barray);
		if (DEBUG) {
			System.out.println("seq: " + seqname);
		}

		String groupname = null;
		boolean bar2 = (file_header.version >= 2.0f);
		if (bar2) {
			int grouplength = dis.readInt();
			barray = new byte[grouplength];
			dis.readFully(barray);
			groupname = new String(barray);
			if (DEBUG) {
				System.out.println("group length: " + grouplength + ", group: " + groupname);
			}
		}

		int verslength = dis.readInt();
		barray = new byte[verslength];
		dis.readFully(barray);
		String seqversion = new String(barray);
		if (DEBUG) {
			System.out.println("version length: " + verslength + ", version: " + seqversion);
		}

		// hack to extract seq version and seq name from seqname field for bar files that were made
		//   with the version and name concatenated (with ";" separator) into the seqname field
		int sc_pos = seqname.lastIndexOf(';');
		String orig_seqname = seqname;
		if (sc_pos >= 0) {
			seqversion = seqname.substring(0, sc_pos);
			seqname = seqname.substring(sc_pos + 1);
			if (DEBUG) {
				System.out.println("seqname = " + seqname + ", seqversion = " + seqversion);
			}
		}

		HashMap<String, String> seq_tagvals = null;
		if (bar2) {
			int seq_tagval_count = dis.readInt();
			if (DEBUG) {
				System.out.println("seq tagval count: " + seq_tagval_count);
			}
			seq_tagvals = readTagValPairs(dis, seq_tagval_count);
		}

		int total_points = dis.readInt();
		if (DEBUG) {
			System.out.println("   seqname = " + seqname + ", version = " + seqversion
					+ ", group = " + groupname
					+ ", data points = " + total_points);
		}

		return new BarSeqHeader(this.seq, total_points, seq_tagvals);
	}

	private static void writeHeaderInfo(BioSeq seq, DataOutputStream dos) throws IOException {
		AnnotatedSeqGroup group = seq.getSeqGroup();
		String groupid = group.getID();
		String seqid = seq.getID();
		dos.writeBytes("barr\r\n\032\n"); // char  "barr\r\n\032\n"
		dos.writeFloat(2.0f); // version of bar format = 2.0
		dos.writeInt(1); // number of seq data sections in file -- if single graph, then 1
		dos.writeInt(2); // number of columns (dimensions) per data point
		dos.writeInt(BYTE4_SIGNED_INT); // int  first column/dimension type ==> 4-byte signed int
		dos.writeInt(BYTE4_FLOAT); // int  second column/dimension type ==> 4-byte float
		// should write out all properties from group and/or graphs as tag/vals?  For now just saying no tag/vals
		dos.writeInt(0);
		// assuming one graph for now, so only one seq section
		dos.writeInt(seqid.length());
		dos.writeBytes(seqid);
		dos.writeInt(groupid.length());
		dos.writeBytes(groupid);
		dos.writeInt(groupid.length());
		dos.writeBytes(groupid);
	}

	private static void writeTagValuePairs(DataOutputStream dos, Map<String, Object> tagValuePairs) throws IOException {
		//any tag value pairs?
		if (tagValuePairs == null || tagValuePairs.size() == 0) {
			dos.writeInt(0);
			return;
		}
		//write number of pairs
		dos.writeInt(tagValuePairs.size());
		//write tag values preceded by their respective lengths
		for (Map.Entry<String, Object> entry : tagValuePairs.entrySet()) {
			String tag = entry.getKey();
			dos.writeInt(tag.length());
			dos.writeBytes(tag);
			String value = entry.getValue().toString();
			dos.writeInt(value.length());
			dos.writeBytes(value);
		}
	}

	private static void checkSeqLength(BioSeq seq, int[] xcoords) {
		if (seq != null) {
			BioSeq aseq = seq;
			int xcount = xcoords.length;
			if (xcount > 0 && (xcoords[xcount - 1] > aseq.getLength())) {
				aseq.setLength(xcoords[xcount - 1]);
			}
		}
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos) throws IOException {
		int total_points = graf.getPointCount();
		dos.writeInt(total_points);
		for (int i = 0; i < total_points; i++) {
			int w = graf.getGraphWidthCoord(i);
			if (w == 0) {
				dos.writeInt(graf.getGraphXCoord(i));
				dos.writeFloat(graf.getGraphYCoord(i));
			} else {
				// Write a point at each interval location.
				for (int j = 0; j < w; j++) {
					dos.writeInt(j + graf.getGraphXCoord(i));
					dos.writeFloat(graf.getGraphYCoord(i));
				}
			}
		}
	}

	public String getMimeType() {
		return "binary/bar";
	}
}

final class BarSeqHeader {

	BioSeq aseq;
	int data_point_count;
	Map<String, String> tagvals;

	BarSeqHeader(BioSeq seq, int data_points, Map<String, String> tagvals) {
		this.aseq = seq;
		this.data_point_count = data_points;
		this.tagvals = tagvals;
	}
}

final class BarFileHeader {

	float version;
	int seq_count;
	int vals_per_point;
	int val_types[];
	int bytes_per_point = 0;
	Map<String, String> tagvals;

	BarFileHeader(float version, int seq_count, int[] val_types, Map<String, String> tagvals) {
		this.version = version;
		this.seq_count = seq_count;
		this.val_types = val_types;
		this.vals_per_point = val_types.length;
		this.tagvals = tagvals;

		for (int i = 0; i < val_types.length; i++) {
			int valtype = val_types[i];
			bytes_per_point += Bar.bytes_per_val[valtype];
		}
	}
}

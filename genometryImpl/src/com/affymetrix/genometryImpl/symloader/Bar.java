package com.affymetrix.genometryImpl.symloader;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.SeqSpan;
import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
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

	private final Map<BioSeq,File> seq2file = new HashMap<BioSeq,File>();
	private final String featureName;
	//private final BioSeq seq;
	// private final List<BioSeq> seqs = new ArrayList<BioSeq>();
	private final AnnotatedSeqGroup group;

	public Bar(URI uri, String featureName, AnnotatedSeqGroup group) {
		super(uri);
		this.featureName = featureName;
		this.group = group;
		
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		File f = LocalUrlCacher.convertURIToFile(uri);
		if (f.isDirectory()) {
			// This is some sort of graphs.seq directory.  Iterate over seqs
			for (File g : f.listFiles()) {
				seq2file.put(determineSeq(g,group),g);
			}
		} else {
			seq2file.put(determineSeq(f,group), f);
		}
	}

	@Override
	public List<GraphSym> getGenome() {
		init();
		List<GraphSym> results = new ArrayList<GraphSym>();
		for (BioSeq seq : seq2file.keySet()) {
			results.addAll(getChromosome(seq));
		}
		return results;
	}
	
	@Override
	public List<GraphSym> getChromosome(BioSeq aseq) {
		init();
		if (aseq == null) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, "Chromosome was null");
			return Collections.<GraphSym>emptyList();
		}
		SeqSpan span = new SimpleSeqSpan(aseq.getMin(), aseq.getMax(), aseq);
		return this.getRegion(span);
	}

	/**
	 *  Gets a slice from a graph bar file.  The returned GraphSym is intended to
	 *  be used only inside a CompositeGraphSym.
	 */
	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		init();
		BioSeq aseq = span.getBioSeq();
		int min_base = span.getMin();
		int max_base = span.getMax();
		if (aseq == null) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, "Chromosome was null");
			return Collections.<GraphSym>emptyList();
		}
		File f = seq2file.get(aseq);
		if (f == null) {
			Logger.getLogger(Bar.class.getName()).log(Level.WARNING, "Chromosome " + aseq.getID() + " was not in file");
			return Collections.<GraphSym>emptyList();
		}
		List<GraphSym> graphs = new ArrayList<GraphSym>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			graphs = this.parse(aseq, fis, featureName, true, min_base, max_base);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return graphs;
	}

	/**
	 * Determine which file associates with the given seq.
	 * @param seq
	 * @return
	 */
	private BioSeq determineSeq(File f, AnnotatedSeqGroup group) {
		String seqname = f.getName().substring(0,f.getName().lastIndexOf(".bar"));
		BioSeq seq = group.getSeq(seqname);
		if (seq == null) {
			seq = group.addSeq(seqname, 1000);	// arbitrary size
		}
		return seq;
	}

	/** Parse a file in BAR format. */
	private List<GraphSym> parse(BioSeq seq, InputStream istr, String stream_name,
			boolean ensure_unique_id, int min, int max)
			{
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
					Logger.getLogger(Bar.class.getName()).log(
					Level.SEVERE, "PARSING FOR BAR FILES WITH 1 VALUE PER POINT NOT YET IMPLEMENTED");
				}
				if (vals_per_point == 2) {
					if (val_types[0] != BYTE4_SIGNED_INT || val_types[1] != BYTE4_FLOAT) {
						Logger.getLogger(Bar.class.getName()).log(
								Level.SEVERE, "Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
					handle2ValPerPoint(seq, bar_header, dis, graph_id, ensure_unique_id, file_tagvals, bar2, graphs, min, max);
				} else if (vals_per_point == 3) {
					// if three values per point, assuming #1 is int base coord, #2 is Pm score, #3 is Mm score
					if (val_types[0] != BYTE4_SIGNED_INT || val_types[1] != BYTE4_FLOAT || val_types[2] != BYTE4_FLOAT) {
						Logger.getLogger(Bar.class.getName()).log(
								Level.SEVERE, "Error in BAR file: Currently, first val must be int4, others must be float4.");
					}
					handle3ValPerPoint(seq, bar_header, dis, graph_id, ensure_unique_id, file_tagvals, bar2, graphs, min, max);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
		}

		return graphs;
	}



	private void handle2ValPerPoint(
			BioSeq seq,
			BarFileHeader bar_header,
			DataInputStream dis, String graph_id, boolean ensure_unique_id,
			Map<String, String> file_tagvals, boolean bar2, List<GraphSym> graphs,
			int min, int max)
			throws IOException {
		BarSeqHeader seq_header = parseSeqHeader(seq, dis, bar_header);
				int total_points = seq_header.data_point_count;
				Map<String, String> seq_tagvals = seq_header.tagvals;
				// TODO: handle case where there are multiple chromosomes in single file.
				//BioSeq seq = seq_header.aseq;
		IntArrayList xcoords = new IntArrayList();
		FloatArrayList ycoords = new FloatArrayList();
		float prev_max_xcoord = -1;
		boolean sort_reported = false;
		for (int i = 0; i < total_points; i++) {
			int col0 = dis.readInt();
			float col1 = dis.readFloat();
			if (col0 < min || col0 >= max) {
				// interbase format
				continue;
			}
			if (col0 < prev_max_xcoord && (!sort_reported)) {
				if (DEBUG) {
					System.out.println("WARNING!! not sorted by ascending xcoord");
				}
				sort_reported = true;
			}
			prev_max_xcoord = col0;
			xcoords.add(col0);
			ycoords.add(col1);
			if (DEBUG && xcoords.size() < 100) {
				System.out.println("Data:\t" + col0 + "\t" + col1);
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
		GraphSym graf = new GraphSym(xcoords.elements(), ycoords.elements(), graph_id, seq);
		copyProps(graf, file_tagvals);
		if (bar2) {
			copyProps(graf, seq_tagvals);
		}
		setStrandProp(seq_tagvals, graf);
		graphs.add(graf);
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

	private void handle3ValPerPoint(
			BioSeq seq,
			BarFileHeader bar_header,
			DataInputStream dis, String graph_id, boolean ensure_unique_id,
			Map<String, String> file_tagvals, boolean bar2, List<GraphSym> graphs,
			int min, int max)
			throws IOException {
		BarSeqHeader seq_header = parseSeqHeader(seq, dis, bar_header);
				int total_points = seq_header.data_point_count;
				Map<String, String> seq_tagvals = seq_header.tagvals;
				// TODO: handle case where there are multiple chromosomes in single file.
				//BioSeq seq = seq_header.aseq;
		IntArrayList xcoords = new IntArrayList();
		FloatArrayList ycoords = new FloatArrayList();
		FloatArrayList zcoords = new FloatArrayList();
		for (int i = 0; i < total_points; i++) {
			int col0 = dis.readInt();
			float col1 = dis.readFloat();
			float col2 = dis.readFloat();
			if (col0 < min || col0 >= max) {
				// interbase format
				continue;
			}
			// interbase format
			xcoords.add(col0);
			ycoords.add(col1);
			zcoords.add(col2);
			if (DEBUG && xcoords.size() < 100) {
				System.out.println("Data:\t" + col0 + "\t" + col1 + "\t" + col2);
			}
		}
		String pm_name = graph_id + " : pm";
		String mm_name = graph_id + " : mm";
		if (ensure_unique_id) {
			pm_name = AnnotatedSeqGroup.getUniqueGraphID(pm_name, seq);
			mm_name = AnnotatedSeqGroup.getUniqueGraphID(mm_name, seq);
		}
		checkSeqLength(seq, xcoords);
		GraphSym pm_graf = new GraphSym(xcoords.elements(), ycoords.elements(), pm_name, seq);
		GraphSym mm_graf = new GraphSym(xcoords.elements(), zcoords.elements(), mm_name, seq);
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
	}

	private BarSeqHeader parseSeqHeader(BioSeq seq, DataInput dis, BarFileHeader file_header) throws IOException {
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

		return new BarSeqHeader(seq, total_points, seq_tagvals);
	}

	private static void checkSeqLength(BioSeq seq, IntArrayList xcoords) {
		if (seq != null) {
			BioSeq aseq = seq;
			int xcount = xcoords.size();
			if (xcount > 0 && (xcoords.get(xcount-1) > aseq.getLength())) {
				aseq.setLength(xcoords.get(xcount-1));
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

package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometryImpl.GraphSymFloat;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.util.FloatList;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IntList;
import com.affymetrix.genometryImpl.util.PointIntFloat;

public final class SgrParser {
	private static final boolean DEBUG = false;
	private static final Comparator<PointIntFloat> pointcomp = PointIntFloat.getComparator(true, true);
	private static final Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace

	public static List<GraphSym> parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
					boolean annotate_seq)
					throws IOException {
		return parse(istr, stream_name, seq_group, annotate_seq, true);
	}

	public static List<GraphSym> parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
					boolean annotate_seq, boolean ensure_unique_id)
					throws IOException {
		if (DEBUG) {
			System.out.println("Parsing with SgrParser: " + stream_name);
		}
		ArrayList<GraphSym> results = new ArrayList<GraphSym>();

		try {
			InputStreamReader isr = new InputStreamReader(istr);
			BufferedReader br = new BufferedReader(isr);

			Map<String, IntList> xhash = new HashMap<String, IntList>();
			Map<String, FloatList> yhash = new HashMap<String, FloatList>();

			String gid = stream_name;
			if (ensure_unique_id) {
				// Making sure the ID is unique on the whole genome, not just this seq
				// will make sure the GraphState is also unique on the whole genome.
				gid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq_group);
			}
			
			parseLines(br, xhash, yhash);

			// after populating all xlists, now make sure sorted
			sortAll(xhash, yhash);
			
			createResults(xhash, seq_group, yhash, gid, results);

		} catch (Exception e) {
			if (!(e instanceof IOException)) {
				IOException ioe = new IOException("Trouble reading SGR file: " + stream_name);
				ioe.initCause(e);
				throw ioe;
			}
		}

		return results;
	}

	private static void parseLines(BufferedReader br, Map<String, IntList> xhash, Map<String, FloatList> yhash) throws IOException, NumberFormatException {
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("%")) {
				continue;
			}
			String[] fields = line_regex.split(line);
			String seqid = fields[0];
			IntList xlist = xhash.get(seqid);
			if (xlist == null) {
				xlist = new IntList();
				xhash.put(seqid, xlist);
			}
			FloatList ylist = yhash.get(seqid);
			if (ylist == null) {
				ylist = new FloatList();
				yhash.put(seqid, ylist);
			}
			int x = Integer.parseInt(fields[1]);
			float y = Float.parseFloat(fields[2]);
			if (DEBUG) {
				System.out.println("seq = " + seqid + ", x = " + x + ", y = " + y);
			}
			xlist.add(x);
			ylist.add(y);
		}
	}


	private static void sortAll(Map<String, IntList> xhash, Map<String, FloatList> yhash) {
		// after populating all xlists, now make sure sorted
		for (Map.Entry<String,IntList> entry : xhash.entrySet()) {
			String seqid = entry.getKey();
			IntList xlist = entry.getValue();
			if (DEBUG) {
				System.out.println("key = " + seqid);
			}
			int xcount = xlist.size();
			boolean sorted = true;
			int prevx = Integer.MIN_VALUE;
			for (int i = 0; i < xcount; i++) {
				int x = xlist.get(i);
				if (x < prevx) {
					sorted = false;
					break;
				}
				prevx = x;
			}
			if (!sorted) {
				pointSort(seqid, xhash, yhash);
			}
		}
	}

	private static void pointSort(String seqid, Map<String, IntList> xhash, Map<String, FloatList> yhash) {
		// System.out.println("points aren't sorted for seq = " + seqid + ", sorting now");
		IntList xlist = xhash.get(seqid);
		FloatList ylist = yhash.get(seqid);
		int graph_length = xlist.size();
		List<PointIntFloat> points = new ArrayList<PointIntFloat>(graph_length);
		for (int i = 0; i < graph_length; i++) {
			int x = xlist.get(i);
			float y = ylist.get(i);
			PointIntFloat pnt = new PointIntFloat(x, y);
			points.add(pnt);
		}
		Collections.sort(points, pointcomp);
		IntList new_xlist = new IntList(graph_length);
		FloatList new_ylist = new FloatList(graph_length);
		for (int i = 0; i < graph_length; i++) {
			PointIntFloat pnt = points.get(i);
			new_xlist.add(pnt.x);
			new_ylist.add(pnt.y);
		}
		xhash.put(seqid, new_xlist);
		yhash.put(seqid, new_ylist);
	}

	public static boolean writeSgrFormat(GraphSym graf, OutputStream ostr) throws IOException {
		BioSeq seq = graf.getGraphSeq();
		if (seq == null) {
			throw new IOException("You cannot use the '.sgr' format when the sequence is unknown. Use '.gr' instead.");
		}
		String seq_id = seq.getID();

		int xpos[] = graf.getGraphXCoords();
		//float ypos[] = (float[]) graf.getGraphYCoords();

		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new BufferedOutputStream(ostr);
			dos = new DataOutputStream(bos);

			for (int i = 0; i < xpos.length; i++) {
				dos.writeBytes(seq_id + "\t" + xpos[i] + "\t" + graf.getGraphYCoordString(i) + "\n");
			}
		} finally {
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(dos);
		}
		return true;
	}


	private static void createResults(Map<String, IntList> xhash, AnnotatedSeqGroup seq_group, Map<String, FloatList> yhash, String gid, ArrayList<GraphSym> results) {
		for (Map.Entry<String, IntList> keyval : xhash.entrySet()) {
			String seqid = keyval.getKey();
			SmartAnnotBioSeq aseq = seq_group.getSeq(seqid);
			IntList xlist = keyval.getValue();
			FloatList ylist = yhash.get(seqid);
			if (aseq == null) {
				aseq = seq_group.addSeq(seqid, xlist.get(xlist.size() - 1));
			}
			int[] xcoords = xlist.copyToArray();
			xlist = null;
			float[] ycoords = ylist.copyToArray();
			ylist = null;
			GraphSymFloat graf = new GraphSymFloat(xcoords, ycoords, gid, aseq);
			results.add(graf);
		}
	}

}

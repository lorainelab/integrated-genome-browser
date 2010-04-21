package com.affymetrix.genometryImpl.symloader;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.graph.GrParser;
import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Sgr extends SymLoader {
	private static final Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
	private final File f;
	private final AnnotatedSeqGroup group;
	private final String featureName;
	
	public Sgr(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri);
		this.f = LocalUrlCacher.convertURIToFile(uri);
		this.group = seq_group;
		this.featureName = featureName;
	}


	@Override
	public List<GraphSym> getGenome() {
		FileInputStream fis = null;
		InputStream is = null;
		try {
			fis = new FileInputStream(this.f);
			is = GeneralUtils.unzipStream(fis, featureName, new StringBuffer());
			return SgrParser.parse(is, featureName, group, false, true);
		} catch (Exception ex) {
			Logger.getLogger(Sgr.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(is);
			GeneralUtils.safeClose(fis);
		}
		return null;
	}

	@Override
	public List<GraphSym> getChromosome(BioSeq seq) {
		return parse(seq, seq.getMin(), seq.getMax());
	}


	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		return parse(span.getBioSeq(), span.getMin(), span.getMax());
	}

	public String getMimeType() {
		return "text/sgr";
	}


	public List<GraphSym> parse(BioSeq seq, int min, int max) {
		List<GraphSym> results = new ArrayList<GraphSym>();
		IntArrayList xlist = new IntArrayList();
		FloatArrayList ylist = new FloatArrayList();

		FileInputStream fis = null;
		InputStream is = null;
		BufferedReader br = null;
		try {
			fis = new FileInputStream(this.f);
			is = GeneralUtils.unzipStream(fis, featureName, new StringBuffer());
			br = new BufferedReader(new InputStreamReader(is));
			
			// Making sure the ID is unique on the whole genome, not just this seq
			// will make sure the GraphState is also unique on the whole genome.
			String gid = AnnotatedSeqGroup.getUniqueGraphID(this.featureName, this.group);
			
			parseLines(br, xlist, ylist, seq, min, max);

			results.add(createResults(xlist, seq, ylist, gid));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
		}

		return results;
	}

	private static void parseLines(
			BufferedReader br, IntArrayList xlist, FloatArrayList ylist, BioSeq seq, int min, int max)
			throws IOException, NumberFormatException {
		String line;
		AnnotatedSeqGroup group = seq.getSeqGroup();
		int x = 0;
		float y = 0.0f;

		while ((line = br.readLine()) != null) {
			if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == '%') {
				continue;
			}
			String[] fields = line_regex.split(line);
			if (fields == null || fields.length == 0) {
				continue;
			}
			String seqid = fields[0];
			
			if (seq != null) {
				// getChromosome() or getRegion()
				if (group == null) {
					if (!seq.getID().equalsIgnoreCase(seqid)) {
						continue;
					}
				} else {
					BioSeq synonymSeq = group.getSeq(seqid);
					if (synonymSeq == null || !synonymSeq.equals(seq)) {
						continue;
					}
				}
				x = Integer.parseInt(fields[1]);
				if (x < min || x >= max) {
					// only look in range
					continue;
				}
			} else {
				// getGenome()
				x = Integer.parseInt(fields[1]);
			}
			
			y = Float.parseFloat(fields[2]);
			xlist.add(x);
			ylist.add(y);
		}
	}

	public static boolean writeSgrFormat(GraphSym graf, OutputStream ostr) throws IOException {
		BioSeq seq = graf.getGraphSeq();
		if (seq == null) {
			throw new IOException("You cannot use the '.sgr' format when the sequence is unknown. Use '.gr' instead.");
		}
		String seq_id = seq.getID();

		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new BufferedOutputStream(ostr);
			dos = new DataOutputStream(bos);
			writeGraphPoints(graf, dos, seq_id);
		} finally {
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos, String seq_id) throws IOException {
		int total_points = graf.getPointCount();
		for (int i = 0; i < total_points; i++) {
			dos.writeBytes(seq_id + "\t" + graf.getGraphXCoord(i) + "\t"
					+ graf.getGraphYCoordString(i) + "\n");
		}
	}


	private static GraphSym createResults(
			IntArrayList xlist, BioSeq aseq, FloatArrayList ylist, String gid) {
			int[] xcoords = Arrays.copyOf(xlist.elements(), xlist.size());
			xlist = null;
			float[] ycoords = Arrays.copyOf(ylist.elements(), ylist.size());
			ylist = null;

			//Is data sorted?
			int xcount = xcoords.length;
			boolean sorted = true;
			int prevx = Integer.MIN_VALUE;
			for (int i = 0; i < xcount; i++) {
				int x = xcoords[i];
				if (x < prevx) {
					sorted = false;
					break;
				}
				prevx = x;
			}
			
			if (!sorted) {
				GrParser.sortXYDataOnX(xcoords, ycoords);
			}

			return new GraphSym(xcoords, ycoords, gid, aseq);
	}

}

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
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.graph.GrParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URI;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Sgr extends SymLoader {
	private static final Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
	private File f;
	private final AnnotatedSeqGroup group;
	private final String featureName;
	protected final Map<BioSeq,File> chrList = new HashMap<BioSeq,File>();
	
	public Sgr(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri);
		this.group = seq_group;
		this.featureName = featureName;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		this.f = LocalUrlCacher.convertURIToFile(uri);
		buildIndex();
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		List<BioSeq> chromosomeList = new ArrayList<BioSeq>(chrList.keySet());
		Collections.sort(chromosomeList,new BioSeqComparator());
		return chromosomeList;
	}

	@Override
	public List<GraphSym> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<GraphSym> retList = new ArrayList<GraphSym>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	}

	@Override
	public List<GraphSym> getChromosome(BioSeq seq) {
		init();
		return parse(seq, seq.getMin(), seq.getMax());
	}


	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(), span.getMin(), span.getMax());
	}

	public String getMimeType() {
		return "text/sgr";
	}


	private List<GraphSym> parse(BioSeq seq, int min, int max) {
		List<GraphSym> results = new ArrayList<GraphSym>();
		IntArrayList xlist = new IntArrayList();
		FloatArrayList ylist = new FloatArrayList();

		FileInputStream fis = null;
		InputStream is = null;
		BufferedReader br = null;
		FileOutputStream fos = null;

		try {

			if (!chrList.containsKey(seq)) {
				Logger.getLogger(Sgr.class.getName()).log(Level.FINE, "Could not find chromosome " + seq.getID());
				return Collections.<GraphSym>emptyList();
			}

			fis = new FileInputStream(chrList.get(seq));
			is = GeneralUtils.unzipStream(fis, featureName, new StringBuffer());
			br = new BufferedReader(new InputStreamReader(is));
			
			// Making sure the ID is unique on the whole genome, not just this seq
			// will make sure the GraphState is also unique on the whole genome.
			String gid = AnnotatedSeqGroup.getUniqueGraphID(this.featureName, this.group);
			
			boolean sorted = parseLines(br, xlist, ylist, seq, min, max);

			GraphSym sym = createResults(xlist, seq, ylist, gid);

			results.add(sym);

			if(!sorted){
				fos = new FileOutputStream(chrList.get(sym.getGraphSeq()));
				writeSgrFormat(sym,fos);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(fos);
		}

		return results;
	}

	private static boolean parseLines(
			BufferedReader br, IntArrayList xlist, FloatArrayList ylist, BioSeq seq, int min, int max)
			throws IOException, NumberFormatException {
		String line;
		AnnotatedSeqGroup group = seq.getSeqGroup();
		int x = 0;
		float y = 0.0f;
		int prevx = 0;
		boolean sorted = true;

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
				if (x < min || x > max) {
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

			if(prevx > x && sorted){
				sorted = false;
			}

			prevx = x;
		}

		return sorted;
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

	private boolean buildIndex(){
		FileInputStream fis = null;
		InputStream is = null;
		Map<String, Integer> chrLength = new HashMap<String, Integer>();
		Map<String, File> chrFiles = new HashMap<String, File>();
		
		try {
			fis = new FileInputStream(this.f);
			is = GeneralUtils.unzipStream(fis, featureName, new StringBuffer());	
			parseLines(is, chrLength, chrFiles);
			createResults(chrLength, chrFiles);
		} catch (Exception ex) {
			Logger.getLogger(Sgr.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
			GeneralUtils.safeClose(fis);
			GeneralUtils.safeClose(is);
		}
		return true;
	}

	private static void parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String,File> chrFiles) {
		Map<String, BufferedWriter> chrs = new HashMap<String, BufferedWriter>();
		BufferedReader br = null;
		BufferedWriter bw = null;
		String line;
		
		try {
			br = new BufferedReader(new InputStreamReader(istr));
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == '%') {
					continue;
				}
				String[] fields = line_regex.split(line);
				String seqid = fields[0];
				int x = Integer.parseInt(fields[1]);
				if (!chrs.containsKey(seqid)) {
					String fileName = seqid;
					if (fileName.length() < 3) {
						fileName += "___";
					}
					File tempFile = File.createTempFile(fileName, ".sgr");
					tempFile.deleteOnExit();
					chrs.put(seqid, new BufferedWriter(new FileWriter(tempFile, true)));
					chrFiles.put(seqid, tempFile);
					chrLength.put(seqid, x);
				}
				bw = chrs.get(seqid);
				if (x > chrLength.get(seqid)) {
					chrLength.put(seqid, x);
				}
				bw.write(line + "\n");
			}
		} catch (IOException ex) {
			Logger.getLogger(Sgr.class.getName()).log(Level.SEVERE, null, ex);
		}finally{
			for(BufferedWriter b : chrs.values()){
				GeneralUtils.safeClose(b);
			}
			GeneralUtils.safeClose(bw);
			GeneralUtils.safeClose(br);
		}
	}

	private void createResults(Map<String, Integer> chrLength, Map<String, File> chrFiles){
		for(Entry<String, Integer> bioseq :chrLength.entrySet()){
			chrList.put(group.addSeq(bioseq.getKey(), bioseq.getValue()), chrFiles.get(bioseq.getKey()));
		}
	}


	
}

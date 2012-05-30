package com.affymetrix.genometryImpl.symloader;

import java.io.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.filter.SymmetryFilterIntersecting;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.symmetry.*;
import com.affymetrix.genometryImpl.thread.PositionCalculator;
import com.affymetrix.genometryImpl.thread.ProgressUpdater;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.*;

/**
 *
 * @author jnicol
 * Could be improved with iterators.  But for now this should be fine.
 */
public abstract class SymLoader implements LineTrackerI {
	public static final int SLEEP_INTERVAL_TIME = 30; // in seconds - any shorter may slow down application
	public static final long SLEEP_INTERVAL_TIME_NANO = (long)SLEEP_INTERVAL_TIME * (long)1000000000; // in nanoseconds
	public static final int SLEEP_TIME = 1; // in milliseconds
	protected long lastSleepTime;
	public static final String FILE_PREFIX = "file:";
	public static final int UNKNOWN_CHROMOSOME_LENGTH = 1; // for unknown chromosomes when the length is not known
	public String extension;	// used for ServerUtils call
	public final URI uri;
	protected boolean isResidueLoader = false;	// Let other classes know if this is just residues
	protected volatile boolean isInitialized = false;
	protected final Map<BioSeq,File> chrList = new HashMap<BioSeq,File>();
	protected final Map<BioSeq,Boolean> chrSort = new HashMap<BioSeq,Boolean>();
	protected final AnnotatedSeqGroup group;
	public final String featureName;
	protected SymLoaderProgressUpdater symLoaderProgressUpdater;
	protected ParseLinesProgressUpdater parseLinesProgressUpdater;

	/**
	 * Implementation of the ProgressUpdater for use in methods that load a data
	 * source and return a List&lt;SeqSymmetry&gt;, like getRegion()
	 * The progress calculation takes the SeqSymmetry span min position as the
	 * position within the passed SeqSpan.
	 */
	public class SymLoaderProgressUpdater extends ProgressUpdater {
		public SymLoaderProgressUpdater(String name, final SeqSpan span) {
			super(name, span.getMin(), span.getMax(),
				new PositionCalculator() {
					@Override
					public long getCurrentPosition() {
						if (symLoaderProgressUpdater == null) {
//							Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "symLoaderProgressUpdater == null in SymLoaderProgressUpdater constructor");
							return span.getMin();
						}
						else {
							SeqSpan testSpan = symLoaderProgressUpdater.getLastSeqSymmetry().getSpan(span.getBioSeq());
							return testSpan.getMin();
						}
					}
				}
			);
		}

		private SeqSymmetry lastSeqSymmetry;

		public SeqSymmetry getLastSeqSymmetry() {
			return lastSeqSymmetry;
		}

		public void setLastSeqSymmetry(SeqSymmetry lastSeqSymmetry) {
			this.lastSeqSymmetry = lastSeqSymmetry;
		}
	};

	/**
	 * Implementation of the ProgressUpdater for use in the parseLines method.
	 * The progress calculation accumulates the line length (assuming LF, not CRLF)
	 * and using that position within the entire file length. Note - for compressed
	 * files the compressed file length is known the uncompressed file length is
	 * needed, but only approximated.
	 */
	protected class ParseLinesProgressUpdater extends ProgressUpdater {
		public ParseLinesProgressUpdater(String name) {
			this(name, 0, GeneralUtils.getUriLength(uri));
		}
		public ParseLinesProgressUpdater(String name, long startPosition, long endPosition) {
			super(name, startPosition, endPosition,
				new PositionCalculator() {
					@Override
					public long getCurrentPosition() {
						if (parseLinesProgressUpdater == null) {
							return 0;
						}
						else {
							return parseLinesProgressUpdater.getFilePosition();
						}
					}
				}
			);
		}

		private long filePosition;

		public long getFilePosition() {
			return filePosition;
		}

		public void lineRead(int lineLength) {
			filePosition += (lineLength + 1); // don't know if line ends with LF (+ 1) or CRLF (+ 2)
		}
	};

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public SymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
        this.uri = uri;
		this.featureName = featureName;
		this.group = group;
		extension = getExtension(uri);
    }

	protected void init() throws Exception {
		this.isInitialized = true;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getFeatureName() {
		return featureName;
	}

	protected boolean buildIndex() throws Exception {
		BufferedInputStream bis = null;
		Map<String, Integer> chrLength = new HashMap<String, Integer>();
		Map<String, File> chrFiles = new HashMap<String, File>();

		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			if(parseLines(bis, chrLength, chrFiles)){
				createResults(chrLength, chrFiles);
				Logger.getLogger(SymLoader.class.getName()).fine("Indexing successful");
				return true;
			}
		} catch(Exception ex){
			throw ex;
		} finally {
			GeneralUtils.safeClose(bis);
		}
		return false;
	}

	protected void sortCreatedFiles() throws Exception {
		//Now Sort all files
		for (Entry<BioSeq, File> file : chrList.entrySet()) {
			chrSort.put(file.getKey(), SortTabFile.sort(file.getValue()));
		}
	}
	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}
	/**
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	public List<BioSeq> getChromosomeList() throws Exception {
		return Collections.<BioSeq>emptyList();
	}
	
    /**
     * @return List of symmetries in genome
     */
    public List<? extends SeqSymmetry> getGenome() throws Exception {

		BufferedInputStream bis = null;
		try {
			// This will also unzip the stream if necessary
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(this.uri);
			return parse(bis, false);
		} catch(Exception ex){
			throw ex;
		} finally {
			GeneralUtils.safeClose(bis);
		}

		//Logger.getLogger(this.getClass().getName()).log(
		//		Level.SEVERE, "Retrieving genome is not defined");
		//return null;
	}

    /**
     * @param seq - chromosome
     * @return List of symmetries in chromosome
     */
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
		Logger.getLogger(this.getClass().getName()).log(
					Level.FINE, "Retrieving chromosome is not optimized");
		List<? extends SeqSymmetry> genomeResults = this.getGenome();
		if (seq == null || genomeResults == null) {
			return genomeResults;
		}
		return filterResultsByChromosome(genomeResults, seq);
    }

	public List<String> getFormatPrefList(){
		return Collections.<String>emptyList();
	}

	public String getExtension(){
		return extension;
	}

	public static String getExtension(URI uri){
		if (uri == null) {
			return null;
		}
		return getExtension(uri.toASCIIString().toLowerCase());
	}
	
	public static String getExtension(String uriString){
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		String extension = ParserController.getExtension(unzippedStreamName);
		extension = extension.substring(extension.indexOf('.') + 1);	// strip off first .
		return extension;
	}
	
	/**
	 * Return the symmetries that match the given chromosome.
	 * @param genomeResults
	 * @param seq
	 * @return
	 */
	public static List<SeqSymmetry> filterResultsByChromosome(List<? extends SeqSymmetry> genomeResults, BioSeq seq) {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (SeqSymmetry sym : genomeResults) {
			BioSeq seq2 = null;
			if (sym instanceof UcscPslSym) {
				seq2 = ((UcscPslSym) sym).getTargetSeq();
			} else {
				seq2 = sym.getSpanSeq(0);
			}
			if (seq.equals(seq2)) {
				results.add(sym);
			}
		}
		return results;
	}

	/**
	 * called by the SymLoaders when they add a new SeqSymmetry during data load
	 * used by the progress updater to show the progress of the data load
	 * @param sym the last SeqSymmetry added during the data load
	 */
	protected void notifyAddSymmetry(SeqSymmetry sym) {
		if (symLoaderProgressUpdater != null) {
			symLoaderProgressUpdater.setLastSeqSymmetry(sym);
		}
	}

	/**
	 * called by the SymLoaders when they parse a new line during parseLines()
	 * used by the progress updater to show the progress of the parseLines()
	 * @param sym the last line read during the parseLines()
	 */
	public void notifyReadLine(int lineLength) {
		if (parseLinesProgressUpdater != null) {
			parseLinesProgressUpdater.lineRead(lineLength + 1);
		}
	}

    /**
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
     * @return List of symmetries satisfying requirements
     */
    public List<? extends SeqSymmetry> getRegion(final SeqSpan overlapSpan) throws Exception {
		symLoaderProgressUpdater = new SymLoaderProgressUpdater("SymLoaderProgressUpdater getRegion for " + uri + " - " + overlapSpan, overlapSpan);
		symLoaderProgressUpdater.start();
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Retrieving region is not supported.  Returning entire chromosome.");
		List<? extends SeqSymmetry> chrResults = this.getChromosome(overlapSpan.getBioSeq());
		symLoaderProgressUpdater.kill();
		symLoaderProgressUpdater = null;
		return chrResults;
    }

	public boolean isResidueLoader(){
		return isResidueLoader;
	}
	
	/**
     * Get residues in the region of the chromosome.  This is generally only defined for some parsers
     * @param span - span of chromosome
     * @return String of residues
     */
    public String getRegionResidues(SeqSpan span) throws Exception {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Not supported.  Returning empty string.");
		return "";
    }

	protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) throws Exception {
		Logger.getLogger(this.getClass().getName()).log(
					Level.SEVERE, "parseLines is not defined");
		return false;
	}
	
	protected void addToLists(
			Map<String, BufferedWriter> chrs, String current_seq_id, Map<String, File> chrFiles, Map<String,Integer> chrLength, String format) throws IOException {

		String fileName = current_seq_id;
		if (fileName.length() < 3) {
			fileName += "___";
		}
		format = !format.startsWith(".") ? "." + format : format;
		File tempFile = File.createTempFile(fileName, format);
		tempFile.deleteOnExit();
		chrs.put(current_seq_id, new BufferedWriter(new FileWriter(tempFile, true)));
		chrFiles.put(current_seq_id, tempFile);
		chrLength.put(current_seq_id, 0);
	}


	protected void createResults(Map<String, Integer> chrLength, Map<String, File> chrFiles){
		for(Entry<String, Integer> bioseq : chrLength.entrySet()){
			String key = bioseq.getKey();
			chrList.put(group.addSeq(key, bioseq.getValue(), uri.toString()), chrFiles.get(key));
		}
	}

	/**
   * Split list of symmetries by track.
   * @param results - list of symmetries
   * @return - Map<String trackName,List<SeqSymmetry>>
   */
  public static Map<String, List<SeqSymmetry>> splitResultsByTracks(List<? extends SeqSymmetry> results) {
		Map<String, List<SeqSymmetry>> track2Results = new HashMap<String, List<SeqSymmetry>>();
		List<SeqSymmetry> resultList = null;
		String method = null;
		for (SeqSymmetry result : results) {
			method = BioSeq.determineMethod(result);
			if (track2Results.containsKey(method)) {
				resultList = track2Results.get(method);
			} else {
				resultList = new ArrayList<SeqSymmetry>();
				track2Results.put(method, resultList);
			}
			resultList.add(result);
		}

	  return track2Results;
  }

  public static Map<BioSeq, List<SeqSymmetry>> splitResultsBySeqs(List<? extends SeqSymmetry> results){
	  Map<BioSeq, List<SeqSymmetry>> seq2Results = new HashMap<BioSeq, List<SeqSymmetry>>();
	  List<SeqSymmetry> resultList = null;
	  BioSeq seq = null;
		for (SeqSymmetry result : results) {

			for(int i=0; i<result.getSpanCount(); i++){
				seq = result.getSpan(i).getBioSeq();

				if (seq2Results.containsKey(seq)) {
					resultList = seq2Results.get(seq);
				} else {
					resultList = new ArrayList<SeqSymmetry>();
					seq2Results.put(seq, resultList);
				}
				resultList.add(result);
			}

		}

	  return seq2Results;
  }

	public static List<? extends SeqSymmetry> splitFilterAndAddAnnotation(final SeqSpan span, List<? extends SeqSymmetry> results, GenericFeature feature){
		Map<String, List<SeqSymmetry>> entries = SymLoader.splitResultsByTracks(results);
		List<SeqSymmetry> added = new ArrayList<SeqSymmetry>();
		SymmetryFilterIntersecting filter = new SymmetryFilterIntersecting();
		filter.setParam(feature.getRequestSym());
		
		for (Entry<String, List<SeqSymmetry>> entry : entries.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}	
			
			List<? extends SeqSymmetry> filteredFeats = filterOutExistingSymmetries(span.getBioSeq(), entry.getValue(), filter);	
			if (filteredFeats.isEmpty()) {
				continue;
			}
			
			added.addAll(filteredFeats);
			SymLoader.addAnnotations(filteredFeats, span, feature.getURI(), feature);
			// Some format do not annotate. So it might not have method name. e.g bgn
			if (entry.getKey() != null) {
				feature.addMethod(entry.getKey());
			}
		}

		return added;
	}
	
	public static void addAnnotations(
			List<? extends SeqSymmetry> filteredFeats, SeqSpan span, URI uri, GenericFeature feature) {
		if (filteredFeats.get(0) instanceof GraphSym) {
			GraphSym graphSym = (GraphSym)filteredFeats.get(0);
			if (filteredFeats.size() == 1 && graphSym.isSpecialGraph()) {
				BioSeq seq = graphSym.getGraphSeq();
				seq.addAnnotation(graphSym);
			}
			else {
				// We assume that if there are any GraphSyms, then we're dealing with a list of GraphSyms.
				for(SeqSymmetry feat : filteredFeats) {
					//grafs.add((GraphSym)feat);
					if (feat instanceof GraphSym) {
						GraphSymUtils.addChildGraph((GraphSym) feat, ((GraphSym) feat).getID(), ((GraphSym) feat).getGraphName(), uri.toString(), span);
					}
				}
			}

			return;
		}

		BioSeq seq = span.getBioSeq();
		for (SeqSymmetry feat : filteredFeats) {
			seq.addAnnotation(feat, feature.getExtension());
		}
		
	}


	private static List<? extends SeqSymmetry> filterOutExistingSymmetries(BioSeq seq, List<? extends SeqSymmetry> syms, SymmetryFilterIntersecting filter) {
		List<SeqSymmetry> filteredFeats = new ArrayList<SeqSymmetry>(syms.size());
		
		for (SeqSymmetry sym : syms) {
			if (filter.filterSymmetry(seq, sym)) {
				filteredFeats.add(sym);
			}
		}
		
		return filteredFeats;
	}

	public static List<BioSeq> getChromosomes(URI uri, String featureName, String groupID) throws Exception {
		AnnotatedSeqGroup temp_group = new AnnotatedSeqGroup(groupID);
		SymLoader temp = new SymLoader(uri, featureName, temp_group) {};
		List<? extends SeqSymmetry> syms = temp.getGenome();
		List<BioSeq> seqs = new ArrayList<BioSeq>();
		seqs.addAll(temp_group.getSeqList());
		
		// Force GC
		syms.clear();
		syms = null;
		temp = null;
		temp_group = null;

		return seqs;
	}

	public List<? extends SeqSymmetry> parse(InputStream is, boolean annotate_seq)
		throws Exception {
		FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(extension);
		if (fileTypeHandler == null) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, MessageFormat.format(GenometryConstants.BUNDLE.getString("noHandler"), extension));
			return null;
		}
		return fileTypeHandler.getParser().parse(new BufferedInputStream(is), group, featureName, uri.toString(), false);
	}

	protected void checkSleep() throws InterruptedException {
		long currentTime = System.nanoTime();
		if (currentTime - lastSleepTime >= SLEEP_INTERVAL_TIME_NANO) {
			Thread.sleep(SLEEP_TIME); // so that thread does not monopolize cpu
			lastSleepTime = currentTime;
		}
	}
	
	public void clear(){}
}

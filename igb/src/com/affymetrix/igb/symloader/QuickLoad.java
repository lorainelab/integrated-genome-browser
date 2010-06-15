package com.affymetrix.igb.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.FeatureRequestSym;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.BED;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.Gr;
import com.affymetrix.genometryImpl.symloader.Sgr;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 */
public final class QuickLoad extends SymLoader {
	private final GenericVersion version;
	public final String featureName;
	private SymLoader symL;	// parser factory

	public QuickLoad(GenericVersion version, String featureName) {
		super(determineURI(version, featureName));
		this.featureName = featureName;
		this.version = version;
		this.symL = determineLoader(extension, uri, featureName, version.group);
	}

	public QuickLoad(GenericVersion version, URI uri) {
		super(uri);
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String strippedName = unzippedName.substring(0, unzippedName.toLowerCase().lastIndexOf(this.extension));
		String friendlyName = strippedName.substring(strippedName.lastIndexOf("/") + 1);
		this.featureName = friendlyName;
		this.version = version;
		this.symL = determineLoader(extension, uri, featureName, version.group);
	}

	@Override
	protected void init() {
		this.isInitialized = true;
	}

	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	@Override
	public List<LoadStrategy> getLoadChoices() {
		// If we're using a symloader, return its load choices.
		if (this.symL != null) {
			return this.symL.getLoadChoices();
		}
		if (this.extension.endsWith(".chp")) {
			List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
			strategyList.add(LoadStrategy.NO_LOAD);
			strategyList.add(LoadStrategy.GENOME);
			return strategyList;
		}
		return super.getLoadChoices();
	}

	private static URI determineURI(GenericVersion version, String featureName) {
		URI uri = null;

		if (version.gServer.URL == null || version.gServer.URL.length() == 0) {
			int httpIndex = featureName.toLowerCase().indexOf("http:");
			if (httpIndex > -1) {
				// Strip off initial characters up to and including http:
				// Sometimes this is necessary, as URLs can start with invalid "http:/"
				featureName = GeneralUtils.convertStreamNameToValidURLName(featureName);
				uri = URI.create(featureName);
			} else {
				uri = (new File(featureName)).toURI();
			}
		} else {
			uri = URI.create(
					version.gServer.URL + "/"
					+ version.versionID + "/"
					+ determineFileName(version, featureName));
		}
		return uri;
	}

	private static String determineFileName(GenericVersion version, String featureName) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) version.gServer.serverObj);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return "";
		}

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
		List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(version.versionID);

		// Linear search, but over a very small list.
		for (AnnotMapElt annotMapElt : annotsList) {
			if (annotMapElt.title.equals(featureName)) {
				return annotMapElt.fileName;
			}
		}
		return "";
	}


	public boolean loadFeatures(final SeqSpan overlapSpan, final LoadStrategy strategy)
			throws OutOfMemoryError {

		final SeqMapView gviewer = Application.getSingleton().getMapView();
		Executor vexec = ThreadUtils.getPrimaryExecutor(this.version.gServer);
		if (this.symL != null && this.symL.isResidueLoader) {
			final BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			return loadResiduesThread(strategy, overlapSpan, seq, gviewer, vexec);
		}

		return loadSymmetriesThread(strategy, overlapSpan, gviewer, vexec);

	}

	private boolean loadSymmetriesThread(
			final LoadStrategy strategy, final SeqSpan overlapSpan, final SeqMapView gviewer, Executor vexec)
			throws OutOfMemoryError {

		SwingWorker<List<? extends SeqSymmetry>, Void> worker = new SwingWorker<List<? extends SeqSymmetry>, Void>() {

			public List<? extends SeqSymmetry> doInBackground() {
				try {
					if (QuickLoad.this.extension.endsWith(".chp") && strategy == LoadStrategy.GENOME) {
						// special-case chp files, due to their LazyChpSym DAS/2 loading
						QuickLoad.this.getGenome();
						gviewer.setAnnotatedSeq(overlapSpan.getBioSeq(), true, true);
						SeqGroupView.refreshTable();
						return null;
					}
					List<FeatureRequestSym> output_requests = FeatureRequestSym.determineFeatureRequestSyms(
							QuickLoad.this.symL, QuickLoad.this.uri, QuickLoad.this.featureName,
							strategy, overlapSpan);
					if (output_requests.isEmpty()) {
						return null;
					}
					List<SeqSymmetry> overallResults = loadAndAddSymmetries(
							QuickLoad.this.symL, QuickLoad.this.featureName, strategy, output_requests);
					return overallResults;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			
			@Override
			public void done() {
				try {
					final List<? extends SeqSymmetry> results = get();
					if (results != null && !results.isEmpty()) {
						gviewer.updateDependentData();
						gviewer.setAnnotatedSeq(overlapSpan.getBioSeq(), true, true);
						SeqGroupView.refreshTable();
					}
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + QuickLoad.this.featureName);
				}
			}
		};
		vexec.execute(worker);
		return true;
	}

	/**
	 * Below are methods normally used by QuickLoad, DAS, DAS/2, etc.
	 */


	protected List<SeqSymmetry> loadAndAddSymmetries(
			SymLoader symL, String featureName,
			LoadStrategy strategy, List<FeatureRequestSym> output_requests)
			throws IOException, OutOfMemoryError {
		if (output_requests.isEmpty()) {
			return null;
		}

		List<? extends SeqSymmetry> results;
		List<SeqSymmetry> overallResults = new ArrayList<SeqSymmetry>();
		for (FeatureRequestSym request : output_requests) {
			// short-circuit if there's a failure... which may not even be signaled in the code
			results = loadFeature(symL, featureName, strategy, request.getOverlapSpan());
			if (results == null) {
				return overallResults;
			}
			results = ServerUtils.filterForOverlappingSymmetries(request.getOverlapSpan(), results);
			if (request.getInsideSpan() != null) {
				results = ServerUtils.specifiedInsideSpan(request.getInsideSpan(), results);
			}
			if (!results.isEmpty()) {
				request.setProperty("method", this.uri.toString());
				FeatureRequestSym.addToRequestSym(results, request, this.uri, featureName, request.getOverlapSpan());
				FeatureRequestSym.addAnnotations(results, request, request.getOverlapSpan().getBioSeq());
				overallResults.addAll(results);
			}
		}
		return overallResults;
	}


	private List<? extends SeqSymmetry> loadFeature(
			SymLoader symL, String featureName, final LoadStrategy strategy, SeqSpan overlapSpan)
			throws IOException, OutOfMemoryError {
		if (!this.isInitialized) {
			this.init();
		}
		IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(this.uri.toString(), featureName);
		style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(featureName, featureName);
		if (strategy == LoadStrategy.GENOME && symL == null) {
			// no symloader... only option is whole genome.
			return this.getGenome();
		}
		if (strategy == LoadStrategy.GENOME || strategy == LoadStrategy.CHROMOSOME) {
			return this.getChromosome(overlapSpan.getBioSeq());
		}
		if (strategy == LoadStrategy.VISIBLE) {
			return this.getRegion(overlapSpan);
		}
		return null;
	}



	public boolean loadResiduesThread(final LoadStrategy strategy, final SeqSpan span, final BioSeq seq, final SeqMapView gviewer, Executor vexec) {
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

			public String doInBackground() {
				try {
					String results = QuickLoad.this.getRegionResidues(span);
					return results;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			@Override
			public void done() {
				try {
					final String results = get();
					if (results != null && !results.isEmpty()) {
						BioSeq.addResiduesToComposition(seq, results, span);
						gviewer.setAnnotatedSeq(seq, true, true);
					}
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + QuickLoad.this.featureName);
				}
			}
		};

		vexec.execute(worker);
		return true;
	}

	/**
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	@Override
	public List<BioSeq> getChromosomeList() {
		if (this.symL != null) {
			return this.symL.getChromosomeList();
		}
		return super.getChromosomeList();
	}


	/**
	 * Only used for non-symloader files.
	 * @return
	 */
	@Override
	public List<? extends SeqSymmetry> getGenome() {
		if (this.symL != null) {
			Logger.getLogger(QuickLoad.class.getName()).severe("Should not get genome here");
			return this.symL.getGenome();
		}

		if (GraphSymUtils.isAGraphFilename(this.extension)) {
			BufferedInputStream bis = null;
			try {
				GenometryModel gmodel = GenometryModel.getGenometryModel();
				bis = LocalUrlCacher.convertURIToBufferedStream(this.uri);
				List<GraphSym> graphs = GraphSymUtils.readGraphs(bis, this.uri.toString(), gmodel, gmodel.getSelectedSeqGroup(), null);
				GraphSymUtils.setName(graphs, OpenGraphAction.getGraphNameForURL(this.uri.toURL()));
				return graphs;
			} catch (Exception ex) {
				Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(bis);
			}
		}

		List<? extends SeqSymmetry> feats = null;
		try {
			if (this.extension.endsWith(".chp")) {
				// special-case CHP files. ChpParser only has
				//    a parse() method that takes the file name
				// (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
				File f = LocalUrlCacher.convertURIToFile(this.uri);
				return ChpParser.parse(f.getAbsolutePath(), true);
			}
			BufferedInputStream bis = null;
			try {
				// This will also unzip the stream if necessary
				bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(this.uri);
				feats = FeatureRequestSym.Parse(this.extension, this.uri, bis, this.version.group, this.featureName);
				return feats;
			} catch (FileNotFoundException ex) {
				Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(bis);
			}
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}


	/**
	 * Determine the appropriate loader.
	 * @return
	 */
	private final static SymLoader determineLoader(String extension, URI uri, String featureName, AnnotatedSeqGroup group) {
		// residue loaders
		extension = extension.substring(extension.lastIndexOf('.') + 1);	// strip off first .
		if (extension.equals("bnib")) {
			return new BNIB(uri, group);
		}
		if (extension.equals("fa") || extension.equals("fas") || extension.equals("fasta")) {
			return new Fasta(uri, group);
		}
		if (extension.equals("2bit")) {
			return new TwoBit(uri);
		}

		// symmetry loaders
		if (extension.equals("bam")) {
			return new BAM(uri, featureName, group);
		}
		/*if (extension.equals("bar")) {
			return new Bar(uri, featureName, group);
		}*/
		if (extension.equals("bed")) {
			return new BED(uri, featureName, group);
		}
		if (extension.equals("gr")) {
			return new Gr(uri, featureName, group);
		}
		if (extension.equals("sgr")) {
			return new Sgr(uri, featureName, group);
		}
		// commented out until the USeq class is updated
//		if (extension.equals("useq")) {
//			return new USeq(uri, featureName, group);
//		}
		if (extension.equals("wig")) {
			return new Wiggle(uri, featureName, group);
		}
		return null;
	}

	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		if (this.symL != null) {
			return this.symL.getChromosome(seq);
		}
		return super.getChromosome(seq);
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		if (this.symL != null) {
			return this.symL.getRegion(span);
		}
		return super.getRegion(span);
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		if (this.symL != null && this.isResidueLoader) {
			return this.symL.getRegionResidues(span);
		}
		Logger.getLogger(QuickLoad.class.getName()).log(
				Level.SEVERE, "Residue loading was called with a non-residue format.");
		return "";
	}
}

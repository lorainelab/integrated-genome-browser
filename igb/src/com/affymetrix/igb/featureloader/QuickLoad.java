package com.affymetrix.igb.featureloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.igb.util.ThreadHandler;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 *
 * @author jnicol
 * @version $Id$
 */
public final class QuickLoad extends SymLoader {

	private final GenericVersion version;
	private SymLoader symL;	// parser factory
	GenometryModel gmodel = GenometryModel.getGenometryModel();

	public QuickLoad(GenericVersion version, String featureName, String organism_dir) {
		super(determineURI(version, featureName, organism_dir), featureName, null);
		this.version = version;
		this.symL = ServerUtils.determineLoader(extension, uri, featureName, version.group);
		this.isResidueLoader = (this.symL != null && this.symL.isResidueLoader);
	}

	public QuickLoad(GenericVersion version, URI uri) {
		super(uri, detemineFriendlyName(uri), null);
		this.version = version;
		this.symL = ServerUtils.determineLoader(extension, uri, featureName, version.group);
		this.isResidueLoader = (this.symL != null && this.symL.isResidueLoader);
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
		Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, "No symloader found.");
		return super.getLoadChoices();
	}

	private static String detemineFriendlyName(URI uri) {
		String uriString = uri.toASCIIString().toLowerCase();
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		String ext = ParserController.getExtension(unzippedStreamName);

		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String strippedName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
		String friendlyName = strippedName.substring(0, strippedName.toLowerCase().indexOf(ext));
		return friendlyName;
	}

	public static URI determineURI(GenericVersion version, String featureName, String organism_dir) {
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
					version.gServer.URL
					+ ((organism_dir != null && !organism_dir.isEmpty()) ? organism_dir + "/" : "")
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

	public boolean loadFeatures(final SeqSpan overlapSpan, final GenericFeature feature)
			throws OutOfMemoryError, IOException {

		if (this.symL != null && this.symL.isResidueLoader) {
			loadResiduesThread(feature, overlapSpan);
			return true;
		} else {
			return loadSymmetriesThread(feature, overlapSpan);
		}
	}

	private boolean loadSymmetriesThread(final GenericFeature feature, final SeqSpan overlapSpan)
			throws OutOfMemoryError, IOException {

		if (QuickLoad.this.extension.endsWith("chp")) {
			// special-case chp files, due to their LazyChpSym DAS/2 loading
			QuickLoad.this.getGenome();
			return true;
		}

		//Do not not anything in case of genome. Just refresh.
		if (IGBConstants.GENOME_SEQ_ID.equals(overlapSpan.getBioSeq().getID())) {
			return false;
		}

		return loadAndAddSymmetries(feature, overlapSpan);
	}

	/**
	 * For unoptimized file formats load symmetries and add them.
	 * @param feature
	 * @return
	 */
	public void loadAllSymmetriesThread(final GenericFeature feature) {

		final SeqMapView gviewer = Application.getSingleton().getMapView();

		CThreadWorker worker = new CThreadWorker("Loading feature " + feature.featureName) {

			@Override
			protected Object runInBackground() {
				try {
					loadAndAddAllSymmetries(feature);
					TrackView.updateDependentData();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			protected void finished() {
				try {
					BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
					if (aseq != null) {
						gviewer.setAnnotatedSeq(aseq, true, true);
					} else if (GenometryModel.getGenometryModel().getSelectedSeq() == null && QuickLoad.this.version.group != null) {
						// This can happen when loading a brand-new genome
						GenometryModel.getGenometryModel().setSelectedSeq(QuickLoad.this.version.group.getSeq(0));
					}

					SeqGroupView.getInstance().refreshTable();
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};

		ThreadHandler.getThreadHandler().execute(feature, worker);
	}

	/**
	 * For optimized file format load symmetries and add them.
	 * @param feature
	 * @param span
	 * @throws IOException
	 * @throws OutOfMemoryError
	 */
	private boolean loadAndAddSymmetries(GenericFeature feature, final SeqSpan span)
			throws IOException, OutOfMemoryError {

		if (this.symL != null && !this.symL.getChromosomeList().contains(span.getBioSeq())) {
			return false;
		}

		setStyle(feature);

		List<? extends SeqSymmetry> results;

		// short-circuit if there's a failure... which may not even be signaled in the code
		if (!this.isInitialized) {
			this.init();
		}

		results = this.getRegion(span);

		if (Thread.currentThread().isInterrupted()) {
			results = null;
			return false;
		}

		boolean ret = false;
		if (results != null) {
			ret = addSymmtries(span, results, feature, extension);
		}

		return ret;
	}

	private void loadAndAddAllSymmetries(final GenericFeature feature)
			throws OutOfMemoryError {

		setStyle(feature);

		// short-circuit if there's a failure... which may not even be signaled in the code
		if (!this.isInitialized) {
			this.init();
		}

		List<? extends SeqSymmetry> results = this.getGenome();

		if (Thread.currentThread().isInterrupted()) {
			feature.setLoadStrategy(LoadStrategy.NO_LOAD); //Change the loadStrategy for this type of files.
			GeneralLoadView.loadModeDataTableModel.fireTableDataChanged();
			results = null;
			return;
		}

		//For a file format that adds SeqSymmetries from
		//within the parser handle them here.
		if (extension.endsWith("chp")) {
			// special-case chp files, due to their LazyChpSym DAS/2 loading
			return;
		}

		Map<BioSeq, List<SeqSymmetry>> seq_syms = SymLoader.splitResultsBySeqs(results);
		SeqSpan span = null;
		BioSeq seq = null;

		for (Entry<BioSeq, List<SeqSymmetry>> seq_sym : seq_syms.entrySet()) {
			seq = seq_sym.getKey();
			span = new SimpleSeqSpan(seq.getMin(), seq.getMax() - 1, seq);
			addSymmtries(span, seq_sym.getValue(), feature, extension);
			feature.addLoadedSpanRequest(span); // this span is now considered loaded.
		}

	}

	private void setStyle(GenericFeature feature) {
		// TODO - not necessarily unique, since the same file can be loaded to multiple tracks for different organisms
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(this.uri.toString(), featureName, extension, feature.featureProps);
		style.setFeature(feature);
		
		// TODO - probably not necessary
		//style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(featureName, featureName, extension, feature.featureProps);
		//style.setFeature(feature);
	}

	private static boolean addSymmtries(final SeqSpan span, List<? extends SeqSymmetry> results, GenericFeature feature, String extension) {
		results = ServerUtils.filterForOverlappingSymmetries(span, results);
		Map<String, List<SeqSymmetry>> entries = SymLoader.splitResultsByTracks(results);
		for (Entry<String, List<SeqSymmetry>> entry : entries.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			SymLoader.filterAndAddAnnotations(entry.getValue(), span, feature.getURI(), feature);
			// Some format do not annotate. So it might not have method name. e.g bgn
			if (entry.getKey() != null) {
				feature.addMethod(entry.getKey());
			}
		}

		return (entries != null && !entries.isEmpty());
	}

	private void loadResiduesThread(final GenericFeature feature, final SeqSpan span) {
		String results = QuickLoad.this.getRegionResidues(span);
		if (results != null && !results.isEmpty()) {
			// TODO: make this more general.  Since we can't currently optimize all residue requests,
			// we are simply considering the span loaded if it loads the entire chromosome
			// Since all request are optimized considering every request to be loaded -- HV 07/26/11
			//if (span.getMin() <= span.getBioSeq().getMin() && span.getMax() >= span.getBioSeq().getMax()) {
			//	feature.addLoadedSpanRequest(span);	// this span is now considered loaded.
			//}
			BioSeq.addResiduesToComposition(span.getBioSeq(), results, span);
		}
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
		try {
			if (this.extension.endsWith("chp")) {
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
				return symL.parse(bis, false);
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

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		if (this.symL != null) {
			return this.symL.getRegion(span);
		}
		return super.getRegion(span);
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		if (this.symL != null && this.symL.isResidueLoader) {
			return this.symL.getRegionResidues(span);
		}
		Logger.getLogger(QuickLoad.class.getName()).log(
				Level.SEVERE, "Residue loading was called with a non-residue format.");
		return "";
	}

	public SymLoader getSymLoader() {
		return symL;
	}
}

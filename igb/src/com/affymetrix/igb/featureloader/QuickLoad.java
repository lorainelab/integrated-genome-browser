package com.affymetrix.igb.featureloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 * @version $Id$
 */
public final class QuickLoad extends SymLoader {
	private final GenericVersion version;
	private SymLoader symL;	// parser factory

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
		if (extension.endsWith(".bar") || extension.endsWith(".useq") || extension.endsWith(".bgr")
				||extension.endsWith(".chp")
				|| (extension.endsWith(".sin") || extension.endsWith(".egr") || extension.endsWith(".txt") || extension.endsWith("link.psl"))
				|| (extension.endsWith(".gff") || extension.endsWith(".gff3"))) {
			List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
			strategyList.add(LoadStrategy.NO_LOAD);
			strategyList.add(LoadStrategy.GENOME);
			return strategyList;
		}
		return super.getLoadChoices();
	}

	private static String detemineFriendlyName(URI uri){
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String strippedName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
		String friendlyName = strippedName.substring(0, strippedName.indexOf('.'));
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
					+ ((organism_dir != null && !organism_dir.isEmpty()) ? organism_dir + "/"  : "")
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
			throws OutOfMemoryError {

		final SeqMapView gviewer = Application.getSingleton().getMapView();
		if (this.symL != null && this.symL.isResidueLoader) {
			final BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
			return loadResiduesThread(feature.loadStrategy, overlapSpan, seq, gviewer);
		}

		return loadSymmetriesThread(feature, overlapSpan, gviewer);

	}

	private boolean loadSymmetriesThread(
			final GenericFeature feature, final SeqSpan overlapSpan, final SeqMapView gviewer)
			throws OutOfMemoryError {

		SwingWorker<List<? extends SeqSymmetry>, Void> worker = new SwingWorker<List<? extends SeqSymmetry>, Void>() {

			public List<? extends SeqSymmetry> doInBackground() {
				try {
					if (QuickLoad.this.extension.endsWith(".chp")) {
						// special-case chp files, due to their LazyChpSym DAS/2 loading
						QuickLoad.this.getGenome();
						gviewer.setAnnotatedSeq(overlapSpan.getBioSeq(), true, true);
						SeqGroupView.refreshTable();
						return null;
					}
					List<SeqSymmetry> overallResults = loadAndAddSymmetries(feature, overlapSpan);
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
						TrackView.updateDependentData();
						BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
						if (overlapSpan != null && aseq != null) {
							gviewer.setAnnotatedSeq(aseq, true, true);
						} else {
							// This can happen when loading a brand-new genome
							if (QuickLoad.this.version.group != null) {
								GenometryModel.getGenometryModel().setSelectedSeq(QuickLoad.this.version.group.getSeq(0));
							}
						}
					} else 							
					if (GenometryModel.getGenometryModel().getSelectedSeq() == null && QuickLoad.this.version.group != null) {
						// This can happen when loading a brand-new genome
						GenometryModel.getGenometryModel().setSelectedSeq(QuickLoad.this.version.group.getSeq(0));
					}

					SeqGroupView.refreshTable();
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
				}
			}
		};
		ThreadUtils.getPrimaryExecutor(this.version.gServer).execute(worker);
		return true;
	}

	/**
	 * Below are methods normally used by QuickLoad, DAS, DAS/2, etc.
	 */


	private List<SeqSymmetry> loadAndAddSymmetries(GenericFeature feature, final SeqSpan span)
			throws IOException, OutOfMemoryError {

		List<? extends SeqSymmetry> results;
		List<SeqSymmetry> overallResults = new ArrayList<SeqSymmetry>();

		// short-circuit if there's a failure... which may not even be signaled in the code
		results = loadFeature(feature, span);
		if (results == null) {
			return overallResults;
		}
		filterAndAddAnnotations(span, results, overallResults);
		
		return overallResults;
	}

	private static void filterAndAddAnnotations(
			SeqSpan span, List<? extends SeqSymmetry> results, List<SeqSymmetry> overallResults) {
		results = ServerUtils.filterForOverlappingSymmetries(span, results);
		for (Map.Entry<String, List<SeqSymmetry>> entry : SymLoader.splitResultsByTracks(results).entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			SymLoader.addAnnotations(entry.getValue(), span.getBioSeq());
			overallResults.addAll(entry.getValue());
		}
	}


	private List<? extends SeqSymmetry> loadFeature(GenericFeature feature, SeqSpan overlapSpan)
			throws IOException, OutOfMemoryError {

		final LoadStrategy strategy = feature.loadStrategy;
		if (!this.isInitialized) {
			this.init();
		}

		// TODO - not necessarily unique, since the same file can be loaded to multiple tracks for different organisms
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(this.uri.toString(), featureName);
		style.setFeature(feature);

		// TODO - probably not necessary
		style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(featureName, featureName);
		style.setFeature(feature);

		// TODO - no need to check if symL is null.
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



	public boolean loadResiduesThread(final LoadStrategy strategy, final SeqSpan span, final BioSeq seq, final SeqMapView gviewer) {
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

		ThreadUtils.getPrimaryExecutor(this.version.gServer).execute(worker);
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
				GraphSymUtils.setName(graphs, GraphSymUtils.getGraphNameForURL(this.uri.toURL()));
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
				feats = SymLoader.Parse(this.extension, this.uri, bis, this.version.group, this.featureName, null);
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
		if (this.symL != null && this.symL.isResidueLoader) {
			return this.symL.getRegionResidues(span);
		}
		Logger.getLogger(QuickLoad.class.getName()).log(
				Level.SEVERE, "Residue loading was called with a non-residue format.");
		return "";
	}

	public SymLoader getSymLoader(){
		return symL;
	}
}

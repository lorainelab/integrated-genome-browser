package com.affymetrix.igb.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.BrsParser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.Bar;
import com.affymetrix.genometryImpl.symloader.Gr;
import com.affymetrix.genometryImpl.symloader.Sgr;
import com.affymetrix.genometryImpl.symloader.USeq;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.menuitem.OpenGraphAction;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
		this.symL = determineLoader();
	}

	public QuickLoad(GenericVersion version, URI uri) {
		super(uri);
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String strippedName = unzippedName.substring(0, unzippedName.lastIndexOf(this.extension));
		String friendlyName = strippedName.substring(strippedName.lastIndexOf("/") + 1);
		this.featureName = friendlyName;
		this.version = version;
		this.symL = determineLoader();
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
	public String[] getLoadChoices() {
		// If we're using a symloader, return its load choices.
		if (this.symL != null) {
			return this.symL.getLoadChoices();
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

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(GenometryModel.getGenometryModel(), quickloadURL);
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
		final BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();

		SwingWorker<List<? extends SeqSymmetry>, Void> worker = new SwingWorker<List<? extends SeqSymmetry>, Void>() {

			public List<? extends SeqSymmetry> doInBackground() {
				try {
					List<? extends SeqSymmetry> results = loadFeature(strategy, overlapSpan);
					if (results != null && !results.isEmpty()) {
						SimpleSymWithProps requestSym = new SimpleSymWithProps();
						requestSym.setProperty("method", featureName);
						SymLoader.addToRequestSym(
								results,
								requestSym,
								QuickLoad.this.featureName,
								QuickLoad.this.featureName,
								overlapSpan);
						if (strategy == LoadStrategy.CHROMOSOME || strategy == LoadStrategy.VISIBLE) {
							SymLoader.addAnnotations(results, requestSym, seq);
						}
						else if (strategy == LoadStrategy.GENOME) {
							for (BioSeq aseq : QuickLoad.this.version.group.getSeqList()) {
								SymLoader.addAnnotations(results, requestSym, aseq);
							}
						}
					}
					return results;
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
						gviewer.setAnnotatedSeq(seq, true, true);
						//SeqGroupView.refreshTable();
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


	private List<? extends SeqSymmetry> loadFeature(final LoadStrategy strategy, SeqSpan overlapSpan) throws IOException, OutOfMemoryError {
		if (!this.isInitialized) {
			this.init();
		}
		IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(this.uri.toString());
		if (style != null) {
			style.setHumanName(featureName);
		}
		style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(featureName);
		if (style != null) {
			style.setHumanName(featureName);
		}
		if (strategy == LoadStrategy.GENOME) {
			return this.getGenome();
		}
		if (strategy == LoadStrategy.CHROMOSOME) {
			return this.getChromosome(GenometryModel.getGenometryModel().getSelectedSeq());
		}
		if (strategy == LoadStrategy.VISIBLE) {
			return this.getRegion(overlapSpan);
		}
		return null;
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


	@Override
	public List<? extends SeqSymmetry> getGenome() {
		if (this.symL != null) {
			return this.symL.getGenome();
		}

		//TODO: if this is coming from a URL, use the InputStream instead of converting to a file

		if (GraphSymUtils.isAGraphFilename(this.extension)) {
			FileInputStream fis = null;
			try {
				GenometryModel gmodel = GenometryModel.getGenometryModel();
				File f = LocalUrlCacher.convertURIToFile(this.uri);
				fis = new FileInputStream(f);
				List<GraphSym> graphs = GraphSymUtils.readGraphs(fis, this.uri.toString(), gmodel, gmodel.getSelectedSeqGroup(), null);
				GraphSymUtils.setName(graphs, OpenGraphAction.getGraphNameForURL(this.uri.toURL()));
				return graphs;
			} catch (Exception ex) {
				Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(fis);
			}
		}

		List<? extends SeqSymmetry> feats = null;
		try {
			File f = LocalUrlCacher.convertURIToFile(this.uri);
			if (this.extension.endsWith(".chp")) {
				// special-case CHP files. ChpParser only has
				//    a parse() method that takes the file name
				// (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
				// Also cannot handle compressed chp files
				return ChpParser.parse(f.getAbsolutePath());
			}
			InputStream is = null;
			try {
				// This will also unzip the stream if necessary
				is = GeneralUtils.getInputStream(f, new StringBuffer());
				feats = Parse(this.extension, is, this.version, this.featureName);
				return feats;
			} catch (FileNotFoundException ex) {
				Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(is);
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

	/**
	 * Determine the appropriate loader.
	 * @return
	 */
	private SymLoader determineLoader() {
		if (this.extension.endsWith("bam")) {
			if (this.version.group == null) {
				//ErrorHandler.errorPanel(gviewerFrame, "ERROR", MERGE_MESSAGE, null);
			} else {
				return new BAM(this.uri, this.featureName, this.version.group);
			}
		}
		if (this.extension.endsWith("bar")) {
			return new Bar(this.uri, this.featureName, this.version.group);
		}
		if (this.extension.endsWith("gr")) {
			if (this.extension.endsWith("sgr")) {
				return new Sgr(this.uri, this.featureName, this.version.group);
			}
			return new Gr(this.uri, this.featureName, this.version.group);
		}
		if (this.extension.endsWith("useq")) {
			return new USeq(this.uri, this.featureName, this.version.group);
		}
		if (this.extension.endsWith("wig")) {
			return new Wiggle(this.uri, this.featureName, this.version.group);
		}
		return null;
	}


	private static List<? extends SeqSymmetry> Parse(
			String extension, InputStream istr, GenericVersion version, String featureName)
			throws Exception {
		BufferedInputStream bis = new BufferedInputStream(istr);
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		extension = extension.substring(extension.lastIndexOf('.') + 1);	// strip off first .
		if (extension.equals("bar")) {
			return BarParser.parse(bis, gmodel, version.group, featureName, false);
		}
		if (extension.equals("bed")) {
			BedParser parser = new BedParser();
			return parser.parse(bis, gmodel, version.group, false, featureName, false);
		}
		if (extension.equals("bgn")) {
			BgnParser parser = new BgnParser();
			return parser.parse(bis, featureName, version.group, false);
		}
		if (extension.equals("bps")) {
			DataInputStream dis = new DataInputStream(bis);
			return BpsParser.parse(dis, featureName, null, version.group, false, false);
		}
		if (extension.equals("bp1") || extension.equals("bp2")) {
			Bprobe1Parser bp1_reader = new Bprobe1Parser();
			// parsing probesets in bp2 format, also adding probeset ids
			return bp1_reader.parse(bis, version.group, false, featureName, false);
		}
		if (extension.equals("brs")) {
			DataInputStream dis = new DataInputStream(bis);
			return BrsParser.parse(dis, featureName, version.group, false);
		}
		if (extension.equals("cyt")) {
			CytobandParser parser = new CytobandParser();
			return parser.parse(bis, version.group, false);
		}
		if (extension.equals("ead")) {
			ExonArrayDesignParser parser = new ExonArrayDesignParser();
			return parser.parse(bis, version.group, false, featureName);
		}
		if (extension.equals("gff")) {
			GFFParser parser = new GFFParser();
			return parser.parse(bis, ".", version.group, false, false);
		}
		if (extension.equals("link.psl")) {
			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			// annotate _target_ (which is chromosome for consensus annots, and consensus seq for probeset annots
			// why is annotate_target parameter below set to false?
			return parser.parse(bis, featureName, null, version.group, null, false, false, false); // do not annotate_other (not applicable since not PSL3)
		}
		if (extension.equals("psl")) {
			// reference to LoadFileAction.ParsePSL
			PSLParser parser = new PSLParser();
			parser.enableSharedQueryTarget(true);
			DataInputStream dis = new DataInputStream(bis);
			return parser.parse(dis, featureName, null, version.group, null, false, false, false);
		}
		if (extension.equals("sin") || extension.equals("egr") || extension.equals("txt")) {
			ScoredIntervalParser parser = new ScoredIntervalParser();
			parser.parse(bis, featureName, version.group);
			return null;	// TODO: don't annotate by default
		}
		Logger.getLogger(QuickLoad.class.getName()).log(Level.WARNING,
				"ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: " + extension);
		return null;
	}
}

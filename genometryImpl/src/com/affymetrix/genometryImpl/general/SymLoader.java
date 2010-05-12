package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.BgnParser;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.BpsParser;
import com.affymetrix.genometryImpl.parsers.BrptParser;
import com.affymetrix.genometryImpl.parsers.BrsParser;
import com.affymetrix.genometryImpl.parsers.BsnpParser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.FishClonesParser;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.VarParser;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser;
import com.affymetrix.genometryImpl.parsers.gchp.AffyCnChpParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.CntParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.graph.ScoredMapParser;
import com.affymetrix.genometryImpl.parsers.useq.ArchiveInfo;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqRegionParser;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.symloader.BED;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.Bar;
import com.affymetrix.genometryImpl.symloader.Fasta;
import com.affymetrix.genometryImpl.symloader.Gr;
import com.affymetrix.genometryImpl.symloader.Sgr;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.symloader.USeq;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.util.ClientOptimizer;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import org.xml.sax.InputSource;

/**
 *
 * @author jnicol
 * Could be improved with iterators.  But for now this should be fine.
 */
public abstract class SymLoader {
	public final URI uri;
	protected final String extension;	// used for ServerUtils call
	public boolean isResidueLoader = false;	// Let other classes know if this is just residues
	protected volatile boolean isInitialized = false;
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public SymLoader(URI uri) {
        this.uri = uri;
		
		String uriString = uri.toASCIIString().toLowerCase();
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		extension = ParserController.getExtension(unzippedStreamName);
    }

	protected void init() {
		this.isInitialized = true;
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
	public List<BioSeq> getChromosomeList() {
		return Collections.<BioSeq>emptyList();
	}
	
    /**
     * @return List of symmetries in genome
     */
    public List<? extends SeqSymmetry> getGenome() {
		Logger.getLogger(this.getClass().getName()).log(
					Level.SEVERE, "Retrieving genome is not defined");
        return null;
    }

    /**
     * @param seq - chromosome
     * @return List of symmetries in chromosome
     */
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.FINE, "Retrieving chromosome is not optimized");
		List<? extends SeqSymmetry> genomeResults = this.getGenome();
		if (seq == null || genomeResults == null) {
			return genomeResults;
		}

		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (SeqSymmetry sym : genomeResults) {
			BioSeq seq2 = null;
			if (sym instanceof UcscPslSym) {
				seq2 = ((UcscPslSym)sym).getTargetSeq();
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
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
     * @return List of symmetries satisfying requirements
     */
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Retrieving region is not supported.  Returning entire chromosome.");
		List<? extends SeqSymmetry> chrResults = this.getChromosome(overlapSpan.getBioSeq());
		return chrResults;
    }

	/**
     * Get residues in the region of the chromosome.  This is generally only defined for some parsers
     * @param span - span of chromosome
     * @return String of residues
     */
    public String getRegionResidues(SeqSpan span) {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Not supported.  Returning empty string.");
		return "";
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
			results = ServerUtils.filterForOverlappingSymmetries(request.getOverlapSpan(), results);
			if (request.getInsideSpan() != null) {
				results = ServerUtils.specifiedInsideSpan(request.getInsideSpan(), results);
			}
			if (results != null && !results.isEmpty()) {
				request.setProperty("method", this.uri.toString());
				SymLoader.addToRequestSym(results, request, this.uri, featureName, request.getOverlapSpan());
				SymLoader.addAnnotations(results, request, request.getOverlapSpan().getBioSeq());
			}
			overallResults.addAll(results);
		}
		return overallResults;
	}


	protected List<? extends SeqSymmetry> loadFeature(
			SymLoader symL, String featureName, final LoadStrategy strategy, SeqSpan overlapSpan)
			throws IOException, OutOfMemoryError {
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

	public static void addToRequestSym(
			 List<? extends SeqSymmetry> feats, SimpleSymWithProps request_sym, URI id, String name, SeqSpan overlapSpan) {
        if (feats == null || feats.isEmpty()) {
            // because many operations will treat empty FeatureRequestSym as a leaf sym, want to
            //    populate with empty sym child/grandchild
            //    [ though a better way might be to have request sym's span on aseq be dependent on children, so
            //       if no children then no span on aseq (though still an overlap_span and inside_span) ]
            SimpleSymWithProps child = new SimpleSymWithProps();
            child.addChild(new SimpleSymWithProps());
            request_sym.addChild(child);
        } else {
            int feat_count = feats.size();
            System.out.println("parsed query results, annot count = " + feat_count);
            for (SeqSymmetry feat : feats) {
                if (feat instanceof GraphSym) {
                    GraphSymUtils.addChildGraph((GraphSym) feat, id, name, overlapSpan);
                } else {
                    request_sym.addChild(feat);
                }
            }
        }
    }

	public static void addAnnotations(
			List<? extends SeqSymmetry> feats, SimpleSymWithProps request_sym, BioSeq aseq) {
		if (feats != null && !feats.isEmpty()) {
			for (SeqSymmetry feat : feats) {
				if (feat instanceof GraphSym) {
					// if graphs, then adding to annotation BioSeq is handled by addChildGraph() method
					return;
				}
			}
		}

		synchronized (aseq) {
			aseq.addAnnotation(request_sym);
		}
	}

	public static List<FeatureRequestSym> determineFeatureRequestSyms(SymLoader symL, URI uri, String featureName, LoadStrategy strategy, SeqSpan overlapSpan) {
		List<FeatureRequestSym> output_requests = new ArrayList<FeatureRequestSym>();
		if (strategy == LoadStrategy.GENOME && symL != null) {
			for (BioSeq aseq : symL.getChromosomeList()) {
				if (aseq.getID().equals(Constants.GENOME_SEQ_ID)) {
					continue;
				}
				SeqSpan overlap = new SimpleSeqSpan(0, aseq.getLength(), aseq);
				FeatureRequestSym requestSym = new FeatureRequestSym(overlap, null);
				ClientOptimizer.OptimizeQuery(aseq, uri, null, featureName, output_requests, requestSym);
			}
		} else {
			FeatureRequestSym requestSym = new FeatureRequestSym(overlapSpan, null);
			ClientOptimizer.OptimizeQuery(requestSym.getOverlapSpan().getBioSeq(), uri, null, featureName, output_requests, requestSym);
		}
		return output_requests;
	}

	public static List<? extends SeqSymmetry> Parse(
			String extension, URI uri, InputStream istr, AnnotatedSeqGroup group, String featureName)
			throws Exception {
		BufferedInputStream bis = new BufferedInputStream(istr);
		extension = extension.substring(extension.lastIndexOf('.') + 1);	// strip off first .

		// These extensions are overloaded by QuickLoad
		if (extension.equals("bar")) {
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, null, 0, Integer.MAX_VALUE, featureName, false);
		}
		if (extension.equals("bed")) {
			BedParser parser = new BedParser();
			return parser.parse(bis, GenometryModel.getGenometryModel(), group, false, uri.toString(), false);
		}
		if (extension.equals("useq")) {
			//find out what kind of data it is, graph or region, from the ArchiveInfo object
			ZipInputStream zis = new ZipInputStream(bis);
			zis.getNextEntry();
			ArchiveInfo archiveInfo = new ArchiveInfo(zis, false);
			if (archiveInfo.getDataType().equals(ArchiveInfo.DATA_TYPE_VALUE_GRAPH)) {
				USeqGraphParser gp = new USeqGraphParser();
				return gp.parseGraphSyms(zis, GenometryModel.getGenometryModel(), featureName, archiveInfo);
			}
			USeqRegionParser rp = new USeqRegionParser();
			return rp.parse(zis, group, featureName, false, archiveInfo);
		}

		// Not overloaded extensions
		if (extension.equals("bgn")) {
			BgnParser parser = new BgnParser();
			return parser.parse(bis, featureName, group, false);
		}
		if (extension.equals("bps")) {
			DataInputStream dis = new DataInputStream(bis);
			return BpsParser.parse(dis, featureName, null, group, false, false);
		}
		if (extension.equals("bp1") || extension.equals("bp2")) {
			Bprobe1Parser bp1_reader = new Bprobe1Parser();
			// parsing probesets in bp2 format, also adding probeset ids
			return bp1_reader.parse(bis, group, false, featureName, false);
		}
		if (extension.equals("brpt")) {
			List<SeqSymmetry> alist = BrptParser.parse(bis, featureName, group, false);
			Logger.getLogger(SymLoader.class.getName()).log(Level.FINE,
					"total repeats loaded: " + alist.size());
			return alist;
		}
		if (extension.equals("brs")) {
			DataInputStream dis = new DataInputStream(bis);
			return BrsParser.parse(dis, featureName, group, false);
		}
		if (extension.equals("bsnp")) {
			List<SeqSymmetry> alist = BsnpParser.parse(bis, featureName, group, false);
			Logger.getLogger(SymLoader.class.getName()).log(Level.FINE,
					"total snps loaded: " + alist.size());
			return alist;
		}
		if (extension.equals("cnchp") || extension.equals("lohchp")) {
			AffyCnChpParser parser = new AffyCnChpParser();
			return parser.parse(null, bis, featureName, group, false);
		}
		if (extension.equals(".cnt")) {
			CntParser parser = new CntParser();
			return parser.parse(bis, group, false);
		}
		if (extension.equals("cyt")) {
			CytobandParser parser = new CytobandParser();
			return parser.parse(bis, group, false);
		}
		if (extension.equals("das") || extension.equals("dasxml")) {
			DASFeatureParser parser = new DASFeatureParser();
			return (List<? extends SeqSymmetry>) parser.parse(bis, group);
		}
		if (extension.equals(Das2FeatureSaxParser.FEATURES_CONTENT_SUBTYPE)
				|| extension.equals("das2feature")
				|| extension.equals("das2xml")
				|| extension.startsWith("x-das-feature")) {
			Das2FeatureSaxParser parser = new Das2FeatureSaxParser();
			return parser.parse(new InputSource(bis), uri.toString(), group, false);
		}
		if (extension.equals("ead")) {
			ExonArrayDesignParser parser = new ExonArrayDesignParser();
			return parser.parse(bis, group, false, featureName);
		}
		if (extension.equals("." + FishClonesParser.FILE_EXT)) {
			FishClonesParser parser = new FishClonesParser(false);
			return parser.parse(bis, featureName, group);
		}
		if (extension.equals("gff") || extension.equals("gtf")) {
			GFFParser parser = new GFFParser();
			return parser.parse(bis, featureName, group, false, false);
		}
		if (extension.equals("gff3")) {
			/* Force parsing as GFF3 */
			GFF3Parser parser = new GFF3Parser();
			return parser.parse(bis, featureName, group);
		}
		if (extension.equals("link.psl")) {
			PSLParser parser = new PSLParser();
			parser.setIsLinkPsl(true);
			parser.enableSharedQueryTarget(true);
			// annotate _target_ (which is chromosome for consensus annots, and consensus seq for probeset annots
			// why is annotate_target parameter below set to false?
			return parser.parse(bis, featureName, null, group, null, false, false, false); // do not annotate_other (not applicable since not PSL3)
		}
		if (extension.equals("psl") || extension.equals("psl3")) {
			// reference to LoadFileAction.ParsePSL
			PSLParser parser = new PSLParser();
			parser.enableSharedQueryTarget(true);
			DataInputStream dis = new DataInputStream(bis);
			return parser.parse(dis, featureName, null, group, null, false, false, false);
		}
		if (extension.equals("sin") || extension.equals("egr") || extension.equals("txt")) {
			ScoredIntervalParser parser = new ScoredIntervalParser();
			return parser.parse(bis, featureName, group, false);
		}
		if (extension.equals("var")) {
			return VarParser.parse(bis, group);
		}

		Logger.getLogger(SymLoader.class.getName()).log(Level.WARNING,
				"ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: " + extension);
		return null;
	}
}

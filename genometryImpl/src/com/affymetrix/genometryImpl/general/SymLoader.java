package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.ParserController;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 * Could be improved with iterators.  But for now this should be fine.
 */
public abstract class SymLoader {
	protected final URI uri;
	protected final String extension;	// used for ServerUtils call
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


	public static void addToRequestSym(
			 List<? extends SeqSymmetry> feats, SimpleSymWithProps request_sym, String id, String name, SeqSpan overlapSpan) {
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
}

package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.net.URI;
import java.util.ArrayList;
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
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	public List<BioSeq> getChromosomeList() {
		return null;
	}
	
    /**
     * @return List of symmetries in genome
     */
    public List<? extends SeqSymmetry> getGenome() {
		Logger.getLogger(this.getClass().getName()).log(
					Level.WARNING, "Retrieving genome is not defined");
        return null;
    }

	/**
	 * Does this support genome requests?
	 * @return
	 */
	public boolean supportsGenome() {
		return true;
	}

    /**
     * @param seq - chromosome
     * @return List of symmetries in chromosome
     */
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		List<? extends SeqSymmetry> genomeResults = this.getGenome();
		if (seq == null || genomeResults == null) {
			return genomeResults;
		}

		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (SeqSymmetry sym : genomeResults) {
			if (seq.equals(sym.getSpanSeq(0))) {
				results.add(sym);
			}
		}
		return results;
    }

	/**
	 * Does this support chromosome requests?
	 * @return
	 */
	public boolean supportsChromosome() {
		return true;
	}

    /**
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
     * @return List of symmetries satisfying requirements
     */
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		List<? extends SeqSymmetry> chrResults = this.getChromosome(overlapSpan.getBioSeq());
		if (chrResults == null || this.extension == null) {
			return chrResults;
		}
		return ServerUtils.getIntersectedSymmetries(overlapSpan, this.extension, null);
    }

	/**
	 * Does this support region requests?
	 * @return
	 */
	public boolean supportsRegion() {
		return true;
	}


     public static void addToRequestSym(
			 List<? extends SeqSymmetry> feats, SimpleSymWithProps request_sym, String id, String name, SeqSpan overlapSpan) {
        if (feats == null || feats.isEmpty()) {
            // because many operations will treat empty Das2FeatureRequestSym as a leaf sym, want to
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

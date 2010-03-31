package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jnicol
 * Could be improved with iterators.  But for now this should be fine.
 */
public abstract class GenericSymRequest {
	protected final URI uri;
	protected final String extension;	// used for ServerUtils call
	protected volatile boolean isInitialized = false;

	public GenericSymRequest(URI uri) {
        this.uri = uri;
		
		String uriString = uri.toASCIIString().toLowerCase();
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		extension = ParserController.getExtension(unzippedStreamName);
    }

	protected void init() {
		this.isInitialized = true;
	}

    /**
     * @return List of symmetries in genome
     */
    public List<? extends SeqSymmetry> getGenome() {
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
    public List<? extends SeqSymmetry> getRegion(BioSeq seq, SeqSpan overlapSpan) {
		List<? extends SeqSymmetry> chrResults = this.getChromosome(seq);
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
}

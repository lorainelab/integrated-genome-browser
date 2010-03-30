package com.affymetrix.genometryImpl.parsers;

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
	private final URI uri;
	private final String extension;	// used for ServerUtils call

	public GenericSymRequest(URI uri) {
        this.uri = uri;
		
		String uriString = uri.toASCIIString().toLowerCase();
		String unzippedStreamName = GeneralUtils.stripEndings(uriString);
		extension = ParserController.getExtension(unzippedStreamName);
    }

    /**
     * @return List of symmetries in genome
     */
    public List<SeqSymmetry> getGenome() {
        return null;
    }

    /**
     * @param seq - chromosome
     * @return List of symmetries in chromosome
     */
    public List<SeqSymmetry> getChromosome(BioSeq seq) {
		List<SeqSymmetry> genomeResults = this.getGenome();
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
     * Get a region of the chromosome.
     * @param seq - chromosome
     * @param overlapSpan - span of overlap
	 * @param insideSpan - 
     * @return List of symmetries satisfying requirements
     */
    public List<SeqSymmetry> getRegion(BioSeq seq, SeqSpan overlapSpan, SeqSpan insideSpan) {
		List<SeqSymmetry> chrResults = this.getChromosome(seq);
		if (chrResults == null || this.extension == null) {
			return chrResults;
		}
		return ServerUtils.getIntersectedSymmetries(overlapSpan, this.extension, insideSpan);
    }
}

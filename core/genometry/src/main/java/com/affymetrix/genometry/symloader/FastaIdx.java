package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import static com.affymetrix.genometry.symloader.ProtocolConstants.FILE_PROTOCOL;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Optional;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.picard.reference.ReferenceSequenceFile;
import net.sf.picard.reference.ReferenceSequenceFileFactory;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 * For fasta files with a SamTools .fai index created with the "samtools faidx"
 * utility and a .dict file created with the CreateSequenceDictionary.jar
 * utility
 */
public class FastaIdx extends FastaCommon {

    final IndexedFastaSequenceFile fastaFile;
    final SAMSequenceDictionary sequenceDict;

    public FastaIdx(URI uri, Optional<URI> indexUri, String featureName, GenomeVersion genomeVersion) {
        super(uri, indexUri, "", genomeVersion);
        if (!uri.toString().startsWith(FILE_PROTOCOL)) {
            fastaFile = null;
            sequenceDict = null;
            return;
        }
        IndexedFastaSequenceFile tempFile;
        try {
            tempFile = new IndexedFastaSequenceFile(new File(uri));
        } catch (FileNotFoundException x) {
            fastaFile = null;
            sequenceDict = null;
            return;
        }
        fastaFile = tempFile;
        String uriString = uri.toString();
        if (uriString.startsWith(FILE_PROTOCOL)) {
            uriString = uri.getPath();
        }
        ReferenceSequenceFile refSeq = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(uriString));
        sequenceDict = refSeq.getSequenceDictionary();
    }

    /**
     * Get seqids and lengths for all chromosomes.
     */
    @Override
    protected boolean initChromosomes() throws Exception {
        for (SAMSequenceRecord rec : sequenceDict.getSequences()) {
            String seqid = rec.getSequenceName();
            BioSeq seq = genomeVersion.getSeq(seqid);
            int count = rec.getSequenceLength();
            if (seq == null) {
                seq = genomeVersion.addSeq(seqid, count, uri.toString());
            }
            chrSet.add(seq);
        }
        return true;
    }

    @Override
    public String getRegionResidues(SeqSpan span) throws Exception {
        ReferenceSequence sequence = fastaFile.getSubsequenceAt(span.getBioSeq().getId(), span.getMin() + 1, Math.min(span.getMax() + 1, span.getBioSeq().getLength()));
        return new String(sequence.getBases());
    }

    /**
     * @return if this SymLoader is valid, there is a readable fasta index file
     * for the data source
     */
    public boolean isValid() {
        return fastaFile != null && sequenceDict != null;
    }
}

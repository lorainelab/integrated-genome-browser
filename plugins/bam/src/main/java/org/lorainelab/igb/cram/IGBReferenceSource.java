package org.lorainelab.igb.cram;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;

import java.nio.charset.StandardCharsets;

public class IGBReferenceSource implements CRAMReferenceSource {
    @Override
    public byte[] getReferenceBases(SAMSequenceRecord sequenceRecord, boolean tryNameVariants) {
        return getReferenceBasesByRegion(sequenceRecord,0, sequenceRecord.getSequenceLength());
    }

    @Override
    public byte[] getReferenceBasesByRegion(SAMSequenceRecord sequenceRecord, int zeroBasedStart, int requestedRegionLength) {
        final BioSeq bioSeq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
        if(bioSeq!=null){
            byte[] bytes = bioSeq.getResidues(zeroBasedStart,zeroBasedStart+requestedRegionLength).getBytes(StandardCharsets.US_ASCII);
            return bytes;
        }
        return new byte[]{};
    }
}

package org.lorainelab.igb.cram;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
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
        int start = zeroBasedStart;
        int end = zeroBasedStart+requestedRegionLength;
        if(bioSeq!=null){
            byte[] bytes = null;
            if(bioSeq.isAvailable(start, end)){
                bytes = bioSeq.getResidues(start, end).getBytes(StandardCharsets.US_ASCII);
                return bytes;
            }else{
                if(GeneralLoadUtils.loadResidues(bioSeq, start, end)){
                    String residues = bioSeq.getResidues(start, end);
                    bytes = bioSeq.getResidues(start, end).getBytes(StandardCharsets.US_ASCII);
                    return bytes;
                }
            }
        }
        return new byte[]{};
    }
}

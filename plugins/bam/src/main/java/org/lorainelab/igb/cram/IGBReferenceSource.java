package org.lorainelab.igb.cram;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;

import java.nio.charset.StandardCharsets;

public class IGBReferenceSource implements CRAMReferenceSource {
    /**
     *
     * @param sequenceRecord the SAMSequenceRecord identifying the reference being requested
     * @param tryNameVariants if true, attempt to match the requested sequence name against the reference by
     *                       using common name variations, such as adding or removing a leading "chr" prefix
     *                       from the requested name. if false, use exact match
     * @return
     */
    @Override
    public byte[] getReferenceBases(SAMSequenceRecord sequenceRecord, boolean tryNameVariants) {
        return getReferenceBasesByRegion(sequenceRecord,0, sequenceRecord.getSequenceLength());
    }

    /**
     *
     * @param sequenceRecord the SAMSequenceRecord for the reference contig being requested
     * @param zeroBasedStart the zero based offset of the starting reference base, must be >= 0
     * @param requestedRegionLength the length of the requested reference region
     * @return
     */
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
                    bytes = bioSeq.getResidues(start, end).getBytes(StandardCharsets.US_ASCII);
                    return bytes;
                }
            }
        }
        return new byte[]{};
    }
}

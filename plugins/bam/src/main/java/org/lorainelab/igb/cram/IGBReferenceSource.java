package org.lorainelab.igb.cram;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class IGBReferenceSource implements CRAMReferenceSource {
    private static Logger LOG = LoggerFactory.getLogger(IGBReferenceSource.class.getName());
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
            byte[] bases = null;
            try {
                if(bioSeq.isAvailable(start, end)){
                    bases = bioSeq.getResidues(start, end).toUpperCase().getBytes(StandardCharsets.US_ASCII);
                }else{
                    if(GeneralLoadUtils.loadResidues(bioSeq, start, end)){
                        bases = bioSeq.getResidues(start, end).toUpperCase().getBytes(StandardCharsets.US_ASCII);
                    }
                }
            } catch (Exception e) {
                LOG.error("An error occurred while retrieving sequence data from the IGB, returning empty result.");
                return new byte[]{};
            }
            return bases;
        }
        return new byte[]{};
    }
}

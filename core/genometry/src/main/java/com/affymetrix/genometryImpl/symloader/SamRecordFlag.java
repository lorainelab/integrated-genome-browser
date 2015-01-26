package com.affymetrix.genometry.symloader;

import static com.affymetrix.genometry.tooltip.ToolTipConstants.*;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dcnorris code partially borrowed from IGV
 * org.broad.igv.sam.SAMAlignment java class
 * @author jrobinso
 *
 *
 * From Sam Spec: FLAG: bitwise FLAG. Each bit is explained in the following
 *
 * 0x1 template having multiple segments in sequencing
 * 0x2 each segment properly aligned according to the aligner
 * 0x4 segment unmapped
 * 0x8 next segment in the template unmapped
 * 0x10 SEQ being reverse complemented
 * 0x20 SEQ of the next segment in the template being reversed
 * 0x40 the first segment in the template
 * 0x80 the last segment in the template
 * 0x100 secondary alignment
 * 0x200 not passing quality controls
 * 0x400 PCR or optical duplicate
 * 0x800 supplementary alignment
 *
 */
public class SamRecordFlag {

    private static final int READ_PAIRED_FLAG = 0x1;
    private static final int PROPER_PAIR_FLAG = 0x2;
    private static final int READ_UNMAPPED_FLAG = 0x4;
    private static final int MATE_UNMAPPED_FLAG = 0x8;
    private static final int READ_STRAND_FLAG = 0x10;
    private static final int MATE_STRAND_FLAG = 0x20;
    private static final int FIRST_OF_PAIR_FLAG = 0x40;
    private static final int SECOND_OF_PAIR_FLAG = 0x80;
    private static final int NOT_PRIMARY_ALIGNMENT_FLAG = 0x100;
    private static final int READ_FAILS_VENDOR_QUALITY_CHECK_FLAG = 0x200;
    private static final int DUPLICATE_READ_FLAG = 0x400;
    private static final int SUPPLEMENTARY_ALIGNMENT_FLAG = 0x800;

    public static final List<String> MATE_PROPS = ImmutableList.<String>builder()
            .add(UNMAPPED_READ)
            .add(PROPER_PAIR_READ)
            .add(READ_REVERSE_STRAND)
            .add(MATE_REVERSE_STRAND)
            .add(FIRST_IN_PAIR)
            .add(SECOND_IN_PAIR).build();

    private final int flag;

    public SamRecordFlag(int flag) {
        this.flag = flag;
    }

    //read paired
    public boolean isPaired() {
        return (flag & READ_PAIRED_FLAG) != 0;
    }

    //read mapped in proper pair
    public boolean isProperPair() {
        return ((flag & READ_PAIRED_FLAG) != 0) && ((flag & PROPER_PAIR_FLAG) != 0);
    }

    // read unmapped flag inverse
    public boolean isMapped() {
        return (flag & READ_UNMAPPED_FLAG) == 0;
    }

    //mate unmapped
    public boolean isMateUnmapped() {
        return (flag & MATE_UNMAPPED_FLAG) != 0;
    }

    //read reverse strand
    public boolean isReverseStrand() {
        return (flag & READ_STRAND_FLAG) != 0;
    }

    //mate reverse strand
    public boolean isMateReverseStrand() {
        return (flag & MATE_STRAND_FLAG) != 0;
    }

    //first in pair
    public boolean isFirstOfPair() {
        return isPaired() && (flag & FIRST_OF_PAIR_FLAG) != 0;
    }

    //second in pair
    public boolean isSecondOfPair() {
        return isPaired() && (flag & SECOND_OF_PAIR_FLAG) != 0;
    }

    //not primary alignment flag inverse
    public boolean isPrimary() {
        return (flag & NOT_PRIMARY_ALIGNMENT_FLAG) == 0;
    }

    //read fails platform/vendor quality checks
    public boolean isVendorFailedRead() {
        return (flag & READ_FAILS_VENDOR_QUALITY_CHECK_FLAG) != 0;
    }

    //read is PCR or optical duplicate
    public boolean isDuplicate() {
        return (flag & DUPLICATE_READ_FLAG) != 0;
    }

    //supplementary alignment
    public boolean isSupplementary() {
        return (flag & SUPPLEMENTARY_ALIGNMENT_FLAG) != 0;
    }

    public Map<String, String> getFlagProperties() {
        Map<String, String> props = new HashMap<>();
        props.put(PAIRED_READ, isPaired() ? "yes" : "no");
        if (isPaired()) {
            props.put(PROPER_PAIR_READ, isProperPair() ? "yes" : "no");
            props.put(UNMAPPED_READ, isMapped() ? "no" : "yes");
            props.put(READ_REVERSE_STRAND, isReverseStrand() ? "yes" : "no");
            props.put(MATE_REVERSE_STRAND, isMateReverseStrand() ? "yes" : "no");
            props.put(FIRST_IN_PAIR, isFirstOfPair() ? "yes" : "no");
            props.put(SECOND_IN_PAIR, isSecondOfPair() ? "yes" : "no");
        }
        props.put(DUPLICATE, isDuplicate() ? "yes" : "no");
        props.put(SUPPLEMENTARY, isSupplementary() ? "yes" : "no");
        props.put(FAILED_QC, isSecondOfPair() ? "yes" : "no");
        return props;
    }

}

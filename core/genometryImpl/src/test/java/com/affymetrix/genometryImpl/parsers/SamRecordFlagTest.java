package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.symloader.SamRecordFlag;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class SamRecordFlagTest {

    @Test
    public void isPaired() {
        SamRecordFlag flag = new SamRecordFlag(65);
        assertTrue(flag.isPaired());
        flag = new SamRecordFlag(1);
        assertTrue(flag.isPaired());
    }

    @Test
    public void isProperPair() {
        SamRecordFlag flag = new SamRecordFlag(2);
        assertTrue(flag.isProperPair());
        flag = new SamRecordFlag(3203);
        assertTrue(flag.isProperPair());
        flag = new SamRecordFlag(51);
        assertTrue(flag.isProperPair());
    }

    @Test
    public void isMapped() {
        SamRecordFlag flag = new SamRecordFlag(51);
        assertTrue(flag.isMapped());
        flag = new SamRecordFlag(12);
        assertTrue(!flag.isMapped());
    }

    @Test
    public void isMateUnmapped() {
        SamRecordFlag flag = new SamRecordFlag(41);
        assertTrue(flag.isMateUnmapped());
        flag = new SamRecordFlag(9);
        assertTrue(flag.isMateUnmapped());
    }

    @Test
    public void firstOfPair() {
        SamRecordFlag flag = new SamRecordFlag(65);
        assertTrue(flag.isFirstOfPair());
        flag = new SamRecordFlag(3905);
        assertTrue(flag.isFirstOfPair());
    }

    @Test
    public void isReverseStrand() {
        SamRecordFlag flag = new SamRecordFlag(49);
        assertTrue(flag.isReverseStrand());
    }

    @Test
    public void isSecondOfPair() {
        SamRecordFlag flag = new SamRecordFlag(131);
        assertTrue(flag.isSecondOfPair());
    }

    @Test
    public void isDuplicate() {
        SamRecordFlag flag = new SamRecordFlag(1089);
        assertTrue(flag.isDuplicate());
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometry.seq;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sgblanch
 */
public class CompositeNegSeqTest {	
	static final CompositeNegSeq seq         = new CompositeNegSeq("seq");
	static final CompositeNegSeq seq_len     = new CompositeNegSeq("len", 500);
	static final CompositeNegSeq seq_pos_pos = new CompositeNegSeq("pos_pos", 250, 500);
	static final CompositeNegSeq seq_neg_pos = new CompositeNegSeq("neg_pos", -500, 500);
	static final CompositeNegSeq seq_neg_neg = new CompositeNegSeq("neg_neg", -500, -250);

    public CompositeNegSeqTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	/**
	 * Test if CompositeNegSeq(String) will accept a null string.
	 */
	@Test
	public void testConstructor1() {
		constructorTest(null, "Constructor accepted a null id");
	}

	/**
	 * Test if CompositeNegSeq(String, int) will accept a null string.
	 */
	@Test
	public void testConstructor2() {
		constructorTest(null, 42, "Constructor accepted a null id");
	}

	/**
	 * Test if CompositeNegSeq(String, int, int) will accept a null string.
	 */
	@Test
	public void testConstructor3() {
		constructorTest(null, -42, 42, "Constructor accepted a null id");
	}

	/**
	 * Test if CompositeNegSeq(String, len) will accept a negative length.
	 */
	@Test
	public void testConstructor4() {
		constructorTest("neg_len", -500, "Constructor accepted a negative length");
	}

	/**
	 * Test if CompositeNegSeq(String, int) will accept a zero length.
	 */
	@Test
	public void testConstructor5() {
		constructorTest("zero_len", 0, "Constructor accepted a zero length");
	}

	/**
	 * Test if CompositeNegSeq(String, int, int) will accept min == max.
	 */
	@Test
	public void testConstructor6() {
		constructorTest("same_min_max", 42, 42, "Constructor accepted min == max");
	}

	/**
	 * Test if CompositeNegSeq(String, int, int) will accept min > max.
	 */
	@Test
	public void testConstructor7() {
		constructorTest("min_gt_max_pos", 42, 17, "Constructor accepted min > max (positive values)");
		constructorTest("min_gt_max_neg", -14, -42, "Constructor accepted min > max (negative values)");
	}

	/**
	 * private function to aid in testing constructor
	 */
	private void constructorTest(String id, String err_msg) {
		CompositeNegSeq testseq;
		try {
			testseq = new CompositeNegSeq(id);
			fail(err_msg);
		} catch (IllegalArgumentException e) { }
	}

	/**
	 * private function to aid in testing constructor
	 */
	private void constructorTest(String id, int len, String err_msg) {
		CompositeNegSeq testseq;
		try {
			testseq = new CompositeNegSeq(id, len);
			fail(err_msg);
		} catch (IllegalArgumentException e) { }
	}

	/**
	 * private function to aid in testing constructor
	 */
	private void constructorTest(String id, int min, int max, String err_msg) {
		CompositeNegSeq testseq;
		try {
			testseq = new CompositeNegSeq(id, min, max);
			fail(err_msg);
		} catch (IllegalArgumentException e) { }
	}

	/**
     * Test getResidues(SeqSpan, char, SeqSymmetry, char[], int)
     */
    @Test
    public void testGetResidues() {
        fail("Test is not implemented");
    }

    /**
     * Test getResidues(int, int, char)
     */
    @Test
    public void testGetResidues2() {
        fail("Test is not implemented");
    }

    /**
     * Test isComplete(int, int)
     */
    @Test
    public void testIsComplete() {
        fail("Test is not implemented");
    }

}

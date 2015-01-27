package com.affymetrix.genometry.comparator;

import org.junit.Test;
import static org.junit.Assert.*;
import com.affymetrix.genometry.symmetry.impl.MutableSingletonSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;


/**
 *
 * @author jnicol
 */
public class SeqSymIdComparatorTest {
	@Test
	public void TestIdComparator() {
		SeqSymIdComparator comp = new SeqSymIdComparator();

		SeqSymmetry sym1 = new MutableSingletonSeqSymmetry("blah", 0, 0, null);

		assertEquals(0,comp.compare(sym1, sym1));

		SeqSymmetry sym2 = new MutableSingletonSeqSymmetry("foo", 0, 0, null);

		assertTrue(comp.compare(sym1, sym2) < 0);
		assertTrue(comp.compare(sym2, sym1) > 0);

	}

	@Test
	public void TestReverseIdComparator() {
		SeqSymReverseIdComparator comp = new SeqSymReverseIdComparator();

		SeqSymmetry sym1 = new MutableSingletonSeqSymmetry("bab", 0, 0, null);

		assertEquals(0,comp.compare(sym1, sym1));

		SeqSymmetry sym2 = new MutableSingletonSeqSymmetry("aac", 0, 0, null);

		assertTrue(comp.compare(sym1, sym2) < 0);
		assertTrue(comp.compare(sym2, sym1) > 0);

		sym2 = new MutableSingletonSeqSymmetry("baa", 0, 0, null);

		assertTrue(comp.compare(sym1, sym2) > 0);
		assertTrue(comp.compare(sym2, sym1) < 0);
	}
}

package com.affymetrix.genometryImpl.operator.comparator;

import com.affymetrix.genometryImpl.general.ID;

/**
 *
 * @author hiralv
 */
public interface MathComparisonOperator extends ID {
	public boolean operate(int i1, int i2);
	public boolean operate(long l1, long l2);
	public boolean operate(float f1, float f2);
	public boolean operate(double d1, double d2);
}

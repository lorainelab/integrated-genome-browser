package com.affymetrix.genometryImpl.operator.comparator;

import com.affymetrix.genometryImpl.general.ID;

/**
 *
 * @author hiralv
 */
public abstract class MathComparisonOperator implements ID {
	public abstract boolean operate(int i1, int i2);
	public abstract boolean operate(long l1, long l2);
	public abstract boolean operate(float f1, float f2);
	public abstract boolean operate(double d1, double d2);
		
	@Override
	public String toString(){
		return getDisplay();
	}
}

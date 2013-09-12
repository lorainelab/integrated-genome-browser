package com.affymetrix.genometryImpl.operator.comparator;

/**
 *
 * @author hiralv
 */
public interface ComparatorI {
	public boolean operate(int i1, int i2);
	public boolean operate(long l1, long l2);
	public boolean operate(float f1, float f2);
	public boolean operate(double d1, double d2);
	public String getSymbol();
}

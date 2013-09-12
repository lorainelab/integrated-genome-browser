package com.affymetrix.genometryImpl.operator.comparator;

/**
 *
 * @author hiralv
 */
public abstract class MathComparisonOperator implements Comparable<MathComparisonOperator>{
	public abstract boolean operate(int i1, int i2);
	public abstract boolean operate(long l1, long l2);
	public abstract boolean operate(float f1, float f2);
	public abstract boolean operate(double d1, double d2);
	public abstract String getSymbol();
	
	@Override
	public String toString(){
		return getSymbol();
	}
	
	@Override
	public int compareTo(MathComparisonOperator mco){
		return getSymbol().compareTo(mco.getSymbol());
	}
}

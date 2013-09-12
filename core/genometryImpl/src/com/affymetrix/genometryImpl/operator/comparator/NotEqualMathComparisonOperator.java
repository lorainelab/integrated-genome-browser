package com.affymetrix.genometryImpl.operator.comparator;

/**
 *
 * @author hiralv
 */
public class NotEqualMathComparisonOperator extends MathComparisonOperator {
	
	@Override
	public boolean operate(int i1, int i2){
		return i1 != i2;
	}
	
	@Override
	public boolean operate(long l1, long l2){
		return l1 != l2;
	}
	
	@Override
	public boolean operate(float f1, float f2){
		return Float.compare(f1, f2) != 0;
	}
	
	@Override
	public boolean operate(double d1, double d2){
		return Double.compare(d1, d2) != 0;
	}
	
	@Override
	public String getSymbol(){
		return "\u2260";
	}
}

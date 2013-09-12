package com.affymetrix.genometryImpl.operator.comparator;

/**
 *
 * @author hiralv
 */
public class GreaterThanEqualMathComparisonOperator extends MathComparisonOperator {
	GreaterThanMathComparisonOperator greaterThanOperator = new GreaterThanMathComparisonOperator();
	EqualMathComparisonOperator equalOperator = new EqualMathComparisonOperator();
	
	@Override
	public boolean operate(int i1, int i2){
		return greaterThanOperator.operate(i1, i2) || equalOperator.operate(i1, i2);
	}
	
	@Override
	public boolean operate(long l1, long l2){
		return greaterThanOperator.operate(l1, l2) || equalOperator.operate(l1, l2);
	}
	
	@Override
	public boolean operate(float f1, float f2){
		return greaterThanOperator.operate(f1, f2) || equalOperator.operate(f1, f2);
	}
	
	@Override
	public boolean operate(double d1, double d2){
		return greaterThanOperator.operate(d1, d2) || equalOperator.operate(d1, d2);
	}
	
	@Override
	public String getSymbol(){
		return "\u2265";
	}
}

package com.affymetrix.genometryImpl.operator.comparator;

/**
 *
 * @author hiralv
 */
public class LessThanEqualMathComparisonOperator extends MathComparisonOperator {
	LessThanMathComparisonOperator lessThanOperator = new LessThanMathComparisonOperator();
	EqualMathComparisonOperator equalOperator = new EqualMathComparisonOperator();
	
	@Override
	public boolean operate(int i1, int i2){
		return lessThanOperator.operate(i1, i2) || equalOperator.operate(i1, i2);
	}
	
	@Override
	public boolean operate(long l1, long l2){
		return lessThanOperator.operate(l1, l2) || equalOperator.operate(l1, l2);
	}
	
	@Override
	public boolean operate(float f1, float f2){
		return lessThanOperator.operate(f1, f2) || equalOperator.operate(f1, f2);
	}
	
	@Override
	public boolean operate(double d1, double d2){
		return lessThanOperator.operate(d1, d2) || equalOperator.operate(d1, d2);
	}
	
	@Override
	public String getName() {
		return "less_than_or_equal";
	}

	@Override
	public String getDisplay() {
		return "\u2264";
	}
}

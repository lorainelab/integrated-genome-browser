package com.affymetrix.genometryImpl.operator;

import java.util.List;

import com.affymetrix.genometryImpl.GenometryConstants;

public class SumOperator extends AbstractGraphOperator implements Operator, Operator.Order{

	@Override
	public String getName() {
		return "sum";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}

	@Override
	protected String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		float total = 0;
		for (Float f : operands) {
			total += f.floatValue();
		}
		return total;
	}
	
	@Override
	public int getOrder() {
		return 1;
	}
}

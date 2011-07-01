package com.affymetrix.genometryImpl.operator.graph;

import java.util.List;

public class MinOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Min";
	}

	@Override
	public String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		float min = Float.MAX_VALUE;
		for (Float f : operands) {
			min = Math.min(min, f);
		}
		return min;
	}

	@Override
	public int getOperandCountMin() {
		return 2;
	}

	@Override
	public int getOperandCountMax() {
		return Integer.MAX_VALUE;
	}

}

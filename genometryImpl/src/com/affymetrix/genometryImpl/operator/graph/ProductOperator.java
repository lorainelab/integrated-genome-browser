package com.affymetrix.genometryImpl.operator.graph;

import java.util.List;

public class ProductOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Product";
	}

	@Override
	public String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		float total = 1;
		for (Float f : operands) {
			total *= f.floatValue();
		}
		return total;
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

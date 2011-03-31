package com.affymetrix.igb.graph.operator;

import java.util.List;

public class DiffOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Diff";
	}

	@Override
	public String getSymbol() {
		return "-";
	}

	@Override
	public float operate(List<Float> operands) {
		return operands.get(0).floatValue() - operands.get(1).floatValue();
	}

	@Override
	public int getOperandCountMin() {
		return 2;
	}

	@Override
	public int getOperandCountMax() {
		return 2;
	}
}

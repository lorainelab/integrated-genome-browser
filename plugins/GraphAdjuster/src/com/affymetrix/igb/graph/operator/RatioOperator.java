package com.affymetrix.igb.graph.operator;

import java.util.List;

public class RatioOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Ratio";
	}

	@Override
	public String getSymbol() {
		return "/";
	}

	@Override
	public float operate(List<Float> operands) {
		if (operands.get(1).floatValue() == 0.0) {
			return 0.0f;
		}
		return operands.get(0).floatValue() / operands.get(1).floatValue();
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

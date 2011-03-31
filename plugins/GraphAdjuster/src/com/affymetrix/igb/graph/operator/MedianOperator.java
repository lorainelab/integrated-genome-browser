package com.affymetrix.igb.graph.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MedianOperator implements GraphOperator {
	@Override
	public String getName() {
		return "Median";
	}

	@Override
	public String getSymbol() {
		return null;
	}

	@Override
	public float operate(List<Float> operands) {
		List<Float> sortOperands = new ArrayList<Float>(operands);
		Collections.sort(sortOperands);
		float median = (operands.size() % 2 == 0) ?
			(float)((sortOperands.get((operands.size() / 2) - 1) + sortOperands.get(operands.size() / 2)) / 2.0) :
			sortOperands.get((operands.size() - 1) / 2);
		return median;
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

package com.affymetrix.genometryImpl.operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class ComboChainOperator implements Operator {
	private static final String BASE_NAME = "chain";
	private final List<Operator> operators;
	public ComboChainOperator(Operator ... operators_) {
		super();
		this.operators = (operators_ == null) ? new ArrayList<Operator>() : Arrays.asList(operators_);
		checkAllCompatible();
	}

	private boolean checkAllCompatible() {
		boolean isCompatible = true;
		for (int i = 1; i < operators.size(); i++) {
			isCompatible &= checkCompatible(operators.get(i - 1), operators.get(i - 1));
		}
		return isCompatible;
	}

	private boolean checkCompatible(Operator before, Operator after) {
		FileTypeCategory category = before.getOutputCategory();
		boolean isCompatible = true;
		for (FileTypeCategory checkCategory : FileTypeCategory.values()) {
			int categoryCount = (checkCategory == category) ? 1 : 0;
			boolean ok = after.getOperandCountMin(checkCategory) <= categoryCount;
			if (!ok) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "incompatible operands, " + before.getName() + " cannot pass output to " + after.getName());
			}
			isCompatible &= ok;
		}
		return isCompatible;
	}

	@Override
	public String getName() {
		StringBuffer name = new StringBuffer(BASE_NAME);
		for (Operator operator : operators) {
			if (BASE_NAME.equals(name.toString())) {
				name.append(" ");
			}
			else {
				name.append(",");
			}
			name.append(operator.getName());
		}
		return name.toString();
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (!checkAllCompatible()) {
			return null;
		}
		SeqSymmetry resultSym = null;
		for (Operator operator : operators) {
			if (resultSym == null) {
				resultSym = operator.operate(aseq, symList);
			}
			else {
				List<SeqSymmetry> inputSymList = new ArrayList<SeqSymmetry>();
				inputSymList.add(resultSym);
				resultSym = operator.operate(aseq, inputSymList);
			}
		}
		return resultSym;
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return operators.size() == 0 ? 0 : operators.get(0).getOperandCountMin(category);
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return operators.size() == 0 ? 0 : operators.get(0).getOperandCountMax(category);
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		for (Operator operator : operators) {
			parameters.putAll(operator.getParameters());
		}
		return parameters;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		for (Operator operator : operators) {
			operator.setParameters(parms);
		}
		return true;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return operators.size() == 0 ? null :  operators.get(operators.size() - 1).getOutputCategory();
	}

	@Override
	public boolean supportsTwoTrack() {
		boolean support = true;
		for (Operator operator : operators) {
			support &= operator.supportsTwoTrack();
		}
		return support;
	}
}

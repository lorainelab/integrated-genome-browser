package com.gene.searchmodelength;

import java.util.Arrays;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public class SymmetryFilterLength implements SymmetryFilterI {
	private static final List<Character> OPERATORS = Arrays.asList(new Character[]{'=','>','<'});
	private Object param;
	private char op;
	private int length = -1;

	@Override
	public String getName() {
		return "length";
	}

	@Override
	public Object getParam() {
		return param;
	}

	@Override
	public boolean setParam(Object param) {
		this.param = param;
		length = -1;
		if (!(param instanceof String)) {
			return false;
		}
		String search_text = (String)param;
		if (search_text.length() < 2) {
			return false;
		}
		op = search_text.charAt(0);
		if (!OPERATORS.contains(op)) {
			return false;
		}
		try {
			length = Integer.parseInt(search_text.substring(1).trim());
		}
		catch (NumberFormatException x) {
			return false;
		}
		return true;
	}

	@Override
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
		boolean passes = false;
		if (length == -1 || sym.getSpanCount() == 0 || sym.getSpan(seq) == null) {
			return false;
		}
		SeqSpan span = sym.getSpan(seq);
		int symLength = span.getLength();
		switch(op) {
		case '=':
			if (symLength == length) {
				passes = true;
			}
			break;
		case '<':
			if (symLength < length) {
				passes = true;
			}
			break;
		case '>':
			if (symLength > length) {
				passes = true;
			}
			break;
		default:
			break;	
		}
		return passes;
	}
}

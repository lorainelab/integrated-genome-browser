package com.affymetrix.genometryImpl.operator;

import java.util.List;

/**
 *
 *  A simple interface for arbitrary operations of float values.
 *  Primarily intended for operations of GraphSym y values.
 *  
 */
public interface GraphOperator  {
	public String getName();
	public String getSymbol();
	public float operate(List<Float> operands);
	public int getOperandCountMin();
	public int getOperandCountMax();
}

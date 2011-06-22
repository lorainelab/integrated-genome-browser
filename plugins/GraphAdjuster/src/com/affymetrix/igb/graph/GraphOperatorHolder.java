package com.affymetrix.igb.graph;

import java.util.TreeSet;

import com.affymetrix.genometryImpl.operator.GraphOperator;

public interface GraphOperatorHolder {
	public TreeSet<GraphOperator> getGraphOperators();
}

package com.affymetrix.igb.graph;

import java.util.TreeSet;

import com.affymetrix.genometryImpl.operator.graph.GraphOperator;

public interface GraphOperatorHolder {
	public TreeSet<GraphOperator> getGraphOperators();
}

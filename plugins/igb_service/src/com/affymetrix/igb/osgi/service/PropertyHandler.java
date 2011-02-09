package com.affymetrix.igb.osgi.service;

import javax.swing.JComponent;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSymmetry;

public interface PropertyHandler {
	public String[][] getPropertiesRow(SeqSymmetry sym, JComponent seqMap);
	public String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, JComponent seqMap);
}

package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface PropertyHandler {
	public String[][] getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder);
	public String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder);
	public void showGraphProperties(GraphSym sym, int x, PropertyHolder propertyHolder);
}

package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSymmetry;

public interface PropertyHandler {
	public String[][] getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder);
	public String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder);
}

package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public interface PropertyHandler {
	public String[][] getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder);
	public String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder);
	public void showGraphProperties(GraphSym sym, int x, PropertyHolder propertyHolder);

	// The general order these fields should show up in.
	public static String[] prop_order = new String[]{
		"gene name",
		"description",
		"name",
		"id",
		"chromosome",
		"start",
		"end",
		"length",
		"strand",
		"min score",
		"max score",
		"type",
		"same orientation",
		"query length",
		"# matches",
		"# target inserts",
		"# target bases inserted",
		"# query bases inserted",
		"# query inserts",
		"seq id",
		"cds min",
		"cds start",
		"cds max",
		"cds end",
		"loadmode",
		"feature url"
	};

	public static String[] tooltip_order = new String[]{
		"id",
		"start",
		"end",
		"length",
		"strand",
		"residues",
		"feature type"
	};
		
	public static String[] graph_tooltip_order = new String[]{
		"id",
		"strand",
		"x coord",
		"y coord",
		"y total",
		"min score",
		"max score",
		"A",
		"T",
		"G",
		"C",
		"N"
	};
	
}

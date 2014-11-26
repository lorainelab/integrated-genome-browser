package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.util.Map;

public interface PropertyHandler {
	public Map<String, Object> getPropertiesRow(SeqSymmetry sym, PropertyHolder propertyHolder);
	public Map<String, Object> getGraphPropertiesRowColumn(GraphSym sym, int x, PropertyHolder propertyHolder);
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
		"score",
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
		"gene name",
		"description",
		"name",
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

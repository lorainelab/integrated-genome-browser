package com.affymetrix.genometry.parsers.graph;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometry.symmetry.impl.GraphSym;

public class GraphParserUtil {
	private static final GraphParserUtil instance = new GraphParserUtil();
	private GraphParserUtil() { super(); }
	public static GraphParserUtil getInstance() {
		return instance;
	}

	public List<GraphSym> wrapInList(GraphSym gsym) {
		List<GraphSym> grafs = null;
		if (gsym != null) {
			grafs = new ArrayList<>();
			grafs.add(gsym);
		}
		return grafs;
	}
}

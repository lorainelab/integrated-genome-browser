package com.affymetrix.genometryImpl.style;

import java.util.HashMap;
import java.util.Map;


/**Contributed by Ido Tamir.*/
public enum GraphType {
	LINE_GRAPH("Line"),
	EMPTY_BAR_GRAPH("Bar"),
	DOT_GRAPH("Dot"),
	MINMAXAVG("Min_Max_Avg"),
	STAIRSTEP_GRAPH("Stairstep"),
	HEAT_MAP("HeatMap"),
	FILL_BAR_GRAPH("Fill Bar");
	
	private String humanReadable;
	private final static Map<String,GraphType> humanReadable2number;

	static {
		humanReadable2number = new HashMap<>();
		for( GraphType type : values()){
			humanReadable2number.put(type.humanReadable, type);
		}
	}


	private GraphType(String humanReadable){
		this.humanReadable = humanReadable;
	}

	public static GraphType fromString(String humanReadable){
		GraphType nr = humanReadable2number.get(humanReadable);
		if(nr != null){
			return nr;
		}
		return GraphType.LINE_GRAPH;
	}

	@Override
	public String toString(){
		return humanReadable;
	}
}

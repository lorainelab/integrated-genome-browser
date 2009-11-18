package com.affymetrix.genometryImpl.style;

import java.util.HashMap;
import java.util.Map;


/**Contributed by Ido Tamir.*/
public enum GraphType {
	LINE_GRAPH( "Line", 1),
		BAR_GRAPH("Bar", 2), 
		DOT_GRAPH("Dot", 3), 
		MINMAXAVG("Min/Max/Avg", 4),
		STAIRSTEP_GRAPH("Stairstep", 5),
		HEAT_MAP("Heat Map", 12);

	private String humanReadable;
	private int number;
	private static Map<String,Integer> humanReadable2number;

	static {
		humanReadable2number = new HashMap<String,Integer>();
		for( GraphType type : values()){
			humanReadable2number.put(type.humanReadable, type.number);
		}
	}


	GraphType(String humanReadable, int number){
		this.humanReadable = humanReadable;
		this.number = number;

	}

	public String getHumanReadable(){
		return humanReadable;
	}

	public int getNumber(){
		return number;
	}

	public static int fromString(String humanReadable){
		Integer nr = humanReadable2number.get(humanReadable);
		if(nr != null){
			return nr;
		}
		//		  ErrorHandler
		return 1;  //TODO: call ErrorHandler, throw exception
	}


}

package com.affymetrix.genometry.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class OrderComparator implements Comparator<String>{
	final Map<String, Integer> orderMap;
	
	public OrderComparator(String[] order){
		orderMap = new HashMap<>();
		int i = 0;
		for(String o : order){
			orderMap.put(o, i++);
		}
	}
	
	public OrderComparator(List<String> order){
		orderMap = new HashMap<>();
		int i = 0;
		for(String o : order){
			orderMap.put(o, i++);
		}
	}

	public int compare(String o1, String o2) {
		Integer o11 = orderMap.get(o1);
		Integer o22 = orderMap.get(o2);
		
		if(o11 == null && o22 == null){
			return 0;
		}
		
		if(o11 == null && o22 != null){
			return 1;
		}
		
		if(o11 != null && o22 == null){
			return -1;
		}
		
		return o11.compareTo(o22);
	}
	
}

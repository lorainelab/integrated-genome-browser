package com.affymetrix.genometryImpl;

import java.util.HashMap;
import java.util.Map;

public enum ResiduesChars {

	A(new char[]{'A','a'},0),
	T(new char[]{'T','t'},1),
	G(new char[]{'G','g'},2),
	C(new char[]{'C','c'},3),
	N(new char[]{'N','n', 'D', 'd', '_'},4);

	final static Map<Character,Integer> rcMap;

	static{
		rcMap = new HashMap<Character, Integer>(5);
		for(ResiduesChars rc : values()){
			for(char ch : rc.chars){
				rcMap.put(ch, rc.value);
			}
		}
	}

	char[] chars;
	int value;

	private ResiduesChars(char[] chars, int value){
		this.chars = chars;
		this.value = value;
	}

	public int getValue(){
		return value;
	}

	@Override
	public String toString(){
		return String.valueOf(chars[0]);
	}

	public static int getValue(char ch){
		Integer val = rcMap.get(ch);
		if(val != null){
			return val.intValue();
		}
		return -1;
	}

}

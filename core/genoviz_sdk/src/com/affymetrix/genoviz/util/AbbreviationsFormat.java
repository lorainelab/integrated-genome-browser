package com.affymetrix.genoviz.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class AbbreviationsFormat extends DecimalFormat {
	//http://en.wikipedia.org/wiki/Tera-
	private static final char[] units = new char[]{' ','k','M','G','T','P','E','Z','Y'};
	private static final int unit_multiple = 1000;
	private final boolean strict;
	
	public AbbreviationsFormat(){
		this(false);
	}
	
	public AbbreviationsFormat(boolean strict){
		this.strict = strict;
	}
	
	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		return format(number, toAppendTo, pos, 0);
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		return format(number, toAppendTo, pos, 0);
	}
	
	
	private StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos, int unit) {
		if (((strict && number > unit_multiple)  || (0 == number % unit_multiple && 0 != number)) && unit < units.length - 1) {
			return format(number /= 1000, toAppendTo, pos, unit += 1);
		}
		return super.format(number, toAppendTo, pos).append(units[unit]);
	}
}

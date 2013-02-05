package com.affymetrix.genoviz.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class AbbreviationsFormat extends DecimalFormat {

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		if (0 == number % 1000 && 0 != number) {
			return format((int)Math.round(number), toAppendTo, pos);
		}
		return super.format(number, toAppendTo, pos);

	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		if (0 == number % 1000 && 0 != number) {
			number /= 1000;
			if (0 == number % 1000) {
				number /= 1000;
				if (0 == number % 1000) {
					number /= 1000;
					if (0 == number % 1000) {
						return super.format(number, toAppendTo, pos).append("T");
					}
					return super.format(number, toAppendTo, pos).append("G");
				}
				return super.format(number, toAppendTo, pos).append("M");
			}
			return super.format(number, toAppendTo, pos).append("k");
		}
		return super.format(number, toAppendTo, pos);
	}
}

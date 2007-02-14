package com.affymetrix.igb.util;

public class StringUtils {

  public static boolean isAllDigits(CharSequence cseq) {
    int char_count = cseq.length();
    boolean all_digits = true;
    for (int i=0; i<char_count; i++) {
      char ch = cseq.charAt(i);
      if (! Character.isDigit(ch)) {
	all_digits = false;
	break;
      }
    }
    return all_digits;
  }

}

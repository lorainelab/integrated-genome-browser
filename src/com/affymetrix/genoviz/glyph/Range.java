package com.affymetrix.genoviz.glyph;

public class Range {
  public int min;
  public int max;
  public Range(int start, int end) {
    if (start <= end) {
      min = start;
      max = end;
    } else {
      min = end;
      max = start;
    }
  }
}

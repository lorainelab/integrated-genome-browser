package com.affymetrix.genoviz.comparator;

import java.util.Comparator;
import com.affymetrix.genoviz.bioviews.GlyphI;

public final class GlyphMinXComparator implements Comparator<GlyphI> {

  public int compare(GlyphI g1, GlyphI g2) {
    if (g1.getCoordBox().x < g2.getCoordBox().x) { return -1; }
    else if (g1.getCoordBox().x > g2.getCoordBox().x) { return 1; }
    else { return 0; }
  }

}

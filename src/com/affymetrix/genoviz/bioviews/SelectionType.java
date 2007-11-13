package com.affymetrix.genoviz.bioviews;

public enum SelectionType {
  /**
   * Do not distinguish selected glyphs
   * from non-selected glyphs.
   */
  SELECT_NONE,
  /**
   * Distinguish selected glyphs
   * by outlining them with selection color.
   */
  SELECT_OUTLINE,
  /**
   * Distinguish selected glyphs
   * by filling them with selection color.
   */
  SELECT_FILL,
  /**
   * Distinguish selected glyphs
   * by filling rectangles behind them with selection color.
   */
  BACKGROUND_FILL,
  /**
   * Distinguish selected glyphs
   * by reversing forground and background colors.
   */
  SELECT_REVERSE

}

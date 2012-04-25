
package com.affymetrix.igb.shared;

/**
 *
 * @author hiralv
 */
public interface ScrollableViewModeGlyph {
	public void setOffset(int offset);
	public int getOffset();
	public double getChildHeight();
	public boolean isScrollingAllowed();
}

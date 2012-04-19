
package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *
 * @author hiralv
 */
public interface ScrollableViewModeGlyph {
	public void setOffset(int offset, ViewI view);
	public int getOffset();
}

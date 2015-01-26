/**
 *   Copyright (c) 2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.style;

import com.affymetrix.genometry.color.ColorProviderI;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.general.GenericFeature;
import java.awt.Color;

public interface ITrackStyleExtended extends ITrackStyle {
	public static final int NO_THRESHOLD = -1;
	void setUrl(String url);
	String getUrl();
	
	/** Controls a parameter of the GenericAnnotGlyphFactory. */
	void setGlyphDepth(int i);
	/** Returns a parameter used by the GenericAnnotGlyphFactory. */
	int getGlyphDepth();

	/** Controls whether plus and minus strands will be drawn separately. */
	void setSeparate(boolean b);
	boolean getSeparate();
        
       
	void setShowAsPaired(boolean b);
	boolean isShowAsPaired();

	/** Controls whether strands can be separated */
	void setSeparable(boolean b);
	boolean getSeparable();
	
	/** Determines which data field in the symmetries will be used to pick the labels. */
	void setLabelField(String s);
	String getLabelField();
	
	public void setFeature(GenericFeature f);
	public GenericFeature getFeature();

	public boolean drawCollapseControl();
	
	void setForwardColor(Color c);
	Color getForwardColor();
	
	void setReverseColor(Color c);
	Color getReverseColor();
			
	int getDirectionType();
	public void setDirectionType(int ordinal);

	void setForwardHeight(double theNewHeight);
	double getForwardHeight();
	void setReverseHeight(double theNewHeight);
	double getReverseHeight();
	
	/**
	 * How high annotations can be stacked on the forward strand.
	 * The rest should overlay each other.
	 */
	void setForwardMaxDepth(int theNewDepth);
	int getForwardMaxDepth();
	/**
	 * How high annotations can be stacked on the reverse strand.
	 * The rest should overlay each other.
	 */
	void setReverseMaxDepth(int theNewDepth);
	int getReverseMaxDepth();

	boolean isFloatTier();
	void setFloatTier(boolean b);
	boolean getJoin();
	void setJoin(boolean b);
	public void setTrackNameSize(float font_size);
	public float getTrackNameSize();
	public Color getLabelForeground();
	public Color getLabelBackground();
	public void setLabelForeground(Color c);
	public void setLabelBackground(Color c);
	public int getSummaryThreshold();
	public void setSummaryThreshold(int level);
	public void setColorProvider(ColorProviderI cp);
	public ColorProviderI getColorProvider();
	public void setFilter(SymmetryFilterI filter);
	public SymmetryFilterI getFilter();
	public boolean getShowResidueMask();
	public void setShowResidueMask(boolean showResidueMask);
	public boolean getShadeBasedOnQualityScore();
	public void setShadeBasedOnQualityScore(boolean shadeBasedOnQualityScore);
	
}

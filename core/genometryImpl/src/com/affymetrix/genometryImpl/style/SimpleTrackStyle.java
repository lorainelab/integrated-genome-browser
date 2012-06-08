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

package com.affymetrix.genometryImpl.style;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import java.awt.Color;

public final class SimpleTrackStyle extends DefaultTrackStyle implements ITrackStyleExtended {
	
	/** for height on the reverse strand. To help with track resizing. */
	private double reverseHeight;
	@Override
	public void setReverseHeight(double theNewHeight) {
		this.reverseHeight = theNewHeight;
	}
	@Override
	public double getReverseHeight() {
		return this.reverseHeight;
	}
	@Override
	public void setForwardHeight(double theNewHeight) {
		super.setHeight(theNewHeight);
	}
	@Override
	public double getForwardHeight() {
		return super.getHeight();
	}
		
	/** for maximum depth of stacked glyphs on the reverse strand. To help with resizing. */
	private int reverseMaxDepth = 0;
	@Override
	public void setReverseMaxDepth(int theNewDepth) {
		this.reverseMaxDepth = theNewDepth;
	}
	@Override
	public int getReverseMaxDepth() {
		return this.reverseMaxDepth;
	}
	@Override
	public void setForwardMaxDepth(int theNewDepth) {
		int rd = this.getMaxDepth();
		this.setMaxDepth(theNewDepth);
		this.reverseMaxDepth = rd;
	}
	@Override
	public int getForwardMaxDepth() {
		return this.getMaxDepth();
	}

	@Override
	public void setHeight(double theNewHeight) {
		super.setHeight(theNewHeight);
		this.reverseHeight = super.getHeight();
	}

	// Should be called only from within package or from StateProvider.
	public SimpleTrackStyle(String name, boolean is_graph) {
		super(name, is_graph);
		this.reverseHeight = super.getHeight();
	}

	String url;
	@Override
	public void setUrl(String url) {this.url = url;}
	@Override
	public String getUrl() { return url; }

	boolean colorByScore = false;
	@Override
	public void setColorByScore(boolean b) {this.colorByScore = b;}
	@Override
	public boolean getColorByScore() { return this.colorByScore;}

	/** Default implementation returns the same as {@link #getColor()}. */
	@Override
	public Color getScoreColor(float f) { return getForeground(); }

	int depth=2;
	@Override
	public void setGlyphDepth(int i) {this.depth = i;}
	@Override
	public int getGlyphDepth() {return this.depth;}

	boolean separate = true;
	@Override
	public void setSeparate(boolean b) { this.separate = b; }
	@Override
	public boolean getSeparate() { return this.separate; }

	String labelField = "id";
	@Override
	public void setLabelField(String s) { this.labelField = s; }
	@Override
	public String getLabelField() { return labelField; }

	@Override
		public void copyPropertiesFrom(ITrackStyle g) {
			super.copyPropertiesFrom(g);  
			if (g instanceof ITrackStyleExtended) {
				ITrackStyleExtended as = (ITrackStyleExtended) g;
				setUrl(as.getUrl());
				setColorByScore(as.getColorByScore());
				setGlyphDepth(as.getGlyphDepth());
				setSeparate(as.getSeparate());
				setLabelField(as.getLabelField());
			}
		}

	public boolean drawCollapseControl() { return getExpandable(); }

	@Override
	public GenericFeature getFeature() {
		return null;
	}

	private String view_mode = null;
	@Override
	public String getViewMode() {
		return view_mode;
	}
	@Override
	public void setViewMode(String view_mode) {
		this.view_mode = view_mode;
	}
	@Override
	public String getOperator() {
		return null;
	}
	@Override
	public void setOperator(String o) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setFeature(GenericFeature f) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getFileType() {
		return "";
	}

	@Override
	public FileTypeCategory getFileTypeCategory(){
		return null;
	}

	@Override
	public int getDirectionType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setDirectionType(int ordinal) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setForwardColor(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Color getForwardColor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setReverseColor(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Color getReverseColor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public final boolean getFloatTier() {
		return false;
	}
	@Override
	public final void setFloatTier(boolean b) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setTrackNameSize(float font_size) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public float getTrackNameSize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Color getLabelForeground() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Color getLabelBackground() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setLabelForeground(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setLabelBackground(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getSummaryThreshold() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setSummaryThreshold(int level) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}

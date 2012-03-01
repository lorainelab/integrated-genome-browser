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
import java.awt.Color;

public final class SimpleTrackStyle extends DefaultTrackStyle implements ITrackStyleExtended {
	
	/** for height on the reverse strand. To help with track resizing. */
	private double reverseHeight;
	public void setReverseHeight(double theNewHeight) {
		this.reverseHeight = theNewHeight;
	}
	public double getReverseHeight() {
		return this.reverseHeight;
	}
	public void setForwardHeight(double theNewHeight) {
		super.setHeight(theNewHeight);
	}
	public double getForwardHeight() {
		return super.getHeight();
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
	public void setUrl(String url) {this.url = url;}
	public String getUrl() { return url; }

	boolean colorByScore = false;
	public void setColorByScore(boolean b) {this.colorByScore = b;}
	public boolean getColorByScore() { return this.colorByScore;}

	/** Default implementation returns the same as {@link #getColor()}. */
	public Color getScoreColor(float f) { return getForeground(); }

	int depth=2;
	public void setGlyphDepth(int i) {this.depth = i;}
	public int getGlyphDepth() {return this.depth;}

	boolean separate = true;
	public void setSeparate(boolean b) { this.separate = b; }
	public boolean getSeparate() { return this.separate; }

	String labelField = "id";
	public void setLabelField(String s) { this.labelField = s; }
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

	public GenericFeature getFeature() {
		return null;
	}
	
	public void setFeature(GenericFeature f) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getFileType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getDirectionType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setForwardColor(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Color getForwardColor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setReverseColor(Color c) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Color getReverseColor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setViewMode(String s) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getViewMode() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setOperator(String o) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getOperator() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}

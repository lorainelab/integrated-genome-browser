/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.AbstractTierGlyph;
import java.awt.geom.Rectangle2D;
import java.util.*;


/**
 * An implementation of graphs for NeoMaps,
 * capable of rendering graphs in a variety of styles.
 * Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph}
 * and improved from there.
 * <p><em><strong>This is only meant for graphs on horizontal maps.</strong></em>
 * </p>
 */
public class GraphTierGlyph extends AbstractTierGlyph{
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		temp.put("y_axis", Boolean.class);
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

	public GraphTierGlyph(ITrackStyleExtended style) {
		super();
		setStyle(style);
	}
							
	@Override
	public void setPreferredHeight(double height, ViewI view) {
		GlyphI child = getChild(0);
		Rectangle2D.Double  c = child.getCoordBox();
		child.setCoords(c.x, c.y, c.width, height);
		//Note : Fix to handle height in a view mode.
		// But this also causes minor change in height while switching back to default view mode.
		setCoords(getCoordBox().x, getCoordBox().y, getCoordBox().width, height + 2 * getSpacing());
		this.style.setHeight(height + 2 * getSpacing());
		child.pack(view);
	}

	/**
	 * Determine how short a glyph can be so we can avoid empty vertical space.
	 * Originally implemented for annotation tracks.
	 * Here we hope for a {@link GraphSym} as the glyph's info.
	 * If we don't find one, we return the answer from the super class.
	 * Subclasses can specialize this, of course.
	 * TODO Do we want y max? or |y max - y min| or [y max|?
	 *      or even max(|y min|, [y max|)?
	 *      The old basic graph glyph used to flip y values
	 *      because pixels start at 0 and go negative.
	 * @param theView limits the data to consider.
	 * @return How tall the glyph must be to show all the data in view.
	 *         Cannot be negative?
	 */
	@Override
	public int getSlotsNeeded(ViewI theView) {
		if (null == theView) {
			throw new IllegalArgumentException("theView cannot be null.");
		}
		Object o = this.getInfo();
		if (null != o) {
			if (o instanceof GraphSym) {
				GraphSym model = (GraphSym) o;
				// Figure it out.
				float[] bounds = getRangeInView(model, theView);
				assert bounds[0] <= bounds[1];
				float answer = bounds[1] - bounds[0];
				if (answer <= 0) {
					return 0;
				}
				if (Integer.MAX_VALUE <= answer) {
					return Integer.MAX_VALUE;
				}
				return (int) answer;
			}
		}
		return super.getSlotsNeeded(theView);
	}

	@Override
	protected boolean shouldDrawToolBar(){
		return this.getChildCount() > 1;
	}
	
	@Override
	public Map<String, Class<?>> getPreferences() {
		return new HashMap<String, Class<?>>(PREFERENCES);
	}

	@Override
	public void setPreferences(Map<String, Object> preferences) {
	}
	
}


/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.util.*;
import java.awt.geom.Rectangle2D;
import javax.swing.JScrollBar;

/**
 * Supports compositions of widgets into a more complex widget.
 *
 * @version $Id$
 */
public abstract class NeoContainerWidget extends NeoAbstractWidget {

	// pixelblur is the amount of pixel space leeway given when finding overlaps
	protected int pixelblur = 2;

	protected static Hashtable<String,Color> colormap = GeneralUtils.getColorMap();

	protected Vector<NeoAbstractWidget> widgets;

	public NeoContainerWidget() {
		super();
		//    setOpaque(true);
		//    setDoubleBuffered(true);

		widgets = new Vector<NeoAbstractWidget>();
	}

	public void addWidget(NeoAbstractWidget widget) {
		widgets.addElement(widget);
	}

	public void destroy() {
		for ( int i = 0; i < widgets.size(); i++ ) {
			widgets.elementAt(i).destroy();
		}
		widgets.removeAllElements();
		this.removeAll();
	}

	public Vector<? extends NeoAbstractWidget> getWidgets() {
		return widgets;
	}

	public void updateWidget() {
		updateWidget(false);
	}

	public void updateWidget(boolean full_update) {
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		NeoAbstractWidget widg;
		while (e.hasMoreElements()) {
			widg = e.nextElement();
			widg.updateWidget(full_update);
		}
	}

	public void stretchToFit(boolean xstretch, boolean ystretch) {
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			e.nextElement().stretchToFit(xstretch,ystretch);
		}
	};

	//********************************************************

	/**
	 * sets the background color for all the contained Widgets.
	 */
	public void setBackground(Color theColor) {
		super.setBackground(theColor);
		for (Enumeration<NeoAbstractWidget> e = widgets.elements();
				e.hasMoreElements();
			) {
			NeoAbstractWidget w = e.nextElement();
			if (w != this) {
					w.setBackground(theColor);
			}
			}
	}

	//********************************************************

	public void setRubberBandBehavior(boolean activate) {
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			e.nextElement().setRubberBandBehavior(activate);
		}
	}

	//********************************************************

	public Rectangle2D.Double getCoordBounds(GlyphI gl) {
		return gl.getCoordBox();
	}

	public void setPixelFuzziness(int blur) {
		if (blur < 0) {
			throw new IllegalArgumentException("puxel fuzziness cannot be negative.");
		}
		pixelblur = blur;
	}

	public int getPixelFuzziness() {
		return pixelblur;
	}

	// trying to modify setVisibility to go through scene method
	//  (and thus trigger damage propogation)
	public void setVisibility(GlyphI gl, boolean isVisible) {
		NeoAbstractWidget widg = getWidget(gl);
		if (widg == null) { return; }
		widg.setVisibility(gl, isVisible);
	}

	public void setVisibility(Vector<GlyphI> glyphs, boolean isVisible) {
		for (int i=0; i<glyphs.size(); i++) {
			setVisibility(glyphs.elementAt(i), isVisible);
		}
	}


	/****************************************/
	/** Methods for dealing with selection **/
	/****************************************/

	public void select(GlyphI gl) {
		if (gl == null) { return; }
		NeoAbstractWidget widg = getWidget(gl);
		if (widg != null) {
			widg.select(gl);
		}
		selected.addElement(gl);
	}

	public void deselect(GlyphI gl) {
		if (gl == null) {
			return;
		}
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			e.nextElement().deselect(gl);
		}
		selected.removeElement(gl);
	}

	public void setSelectionAppearance(int behavior) {
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			e.nextElement().setSelectionAppearance(behavior);
		}
	}
	public int getSelectionAppearance() {
		return widgets.firstElement().getSelectionAppearance();
	}

	public void setSelectionColor(Color col) {
		for (NeoAbstractWidget w : widgets) {
			w.setSelectionColor(col);
		}
	}

	public Color getSelectionColor() {
		return widgets.firstElement().getSelectionColor();
	}

	/**
	 * this stub should be overridden by subclasses
	 * that allow sub selection.
	 */
	public void setSubSelectionAllowed(boolean allowed) {
	}
	/**
	 * this stub should be overridden by subclasses
	 * that allow sub selection.
	 */
	public boolean isSubSelectionAllowed() {
		return false;
	}

	/**
	 * sets the expansion policy for the widgets in this container.
	 * It just passes the news on to each contained widget.
	 *
	 * @see NeoAbstractWidget#setExpansionBehavior
	 */
	public void setExpansionBehavior(int axisid, int behavior) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set behavior for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for "+ axisid);
		if (!(NeoAbstractWidget.EXPAND == behavior || NeoAbstractWidget.NO_EXPAND == behavior))
			throw new IllegalArgumentException(
					"Can only set behavior to EXPAND or NO_EXPAND");
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setExpansionBehavior(axisid, behavior);
		}
	}

	/**
	 * Gets the expansion policy for the first widget in this container.
	 *
	 * @see NeoAbstractWidget#getExpansionBehavior
	 */
	public int getExpansionBehavior(int axisid) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can get behavior for X ("+NeoAbstractWidget.X+") or Y ("
					+NeoAbstractWidget.Y+") axis. "
					+"Not for "+ axisid);
		// The following will throw an exception if widgets is empty.
		// This will do until we find out (and formalize) the default.
		return widgets.elementAt(0).getExpansionBehavior(axisid);
	}

	/** Subclasses must define getWidget() method. */
	public abstract NeoAbstractWidget getWidget(int location);

	/** Subclasses must define getLocation() method. */
	public abstract int getLocation(NeoAbstractWidget widget);

	public Vector<GlyphI> getItems(double xcoord, double ycoord, int location) {
		// getWidget() is defined in subclasses
		NeoAbstractWidget widg = getWidget(location);
		if (widg == this) {
			throw new RuntimeException("Widget contains itself?");
		}
		else if (widg instanceof NeoWidget) {
			return ((NeoWidget)widg).getItems(xcoord, ycoord);
		}
		else if (widg instanceof NeoMap) {
			return ((NeoMap)widg).getItems(xcoord, ycoord);
		}
		else if (widg instanceof NeoContainerWidget) {
			return ((NeoContainerWidget)widg).getItems(xcoord, ycoord, location);
		}
		/*
		   else if (widg instanceof NeoWidget) {
		   return ((NeoWidget)widg).getItems(xcoord, ycoord);
		   }
		   else if (widg instanceof NeoMap) {
		   return ((NeoMap)widg).getItems(xcoord, ycoord);
		   }
		   else if (widg instanceof NeoContainerWidget) {
		   return ((NeoContainerWidget)widg).getItems(xcoord, ycoord, location);
		   }
		   */

		throw new RuntimeException(
				"Widget contains a " + widg.getClass().getName());
	}



	// Here we implement the zooming portion of the NeoAbstractWidget interface.
	// Perhaps it should be a separate interface like "Zoomable". -- Eric
	// For each setter we just pass it on to all the contained widgets.
	// For each getter we just get the setting for the first widget.
	// Note that these have been compiled, but not yet excercized.

	/**
	 * sets a scale constraint along the given axis
	 * for all NeoWidgets (maps) in this NeoContainerWidget.
	 *
	 * <p> Scale constraints are currently only considered
	 *     during zooming with zoomer[] adjustables
	 *
	 * @see NeoAbstractWidget#setScaleConstraint
	 */
	public void setScaleConstraint(int axisid, int constraint) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can constrain scale for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for " + axisid);
		if (!(INTEGRAL_PIXELS == constraint
					|| INTEGRAL_COORDS == constraint
					|| INTEGRAL_ALL == constraint)) {
			throw new IllegalArgumentException(
					"Can constrain scale to INTEGRAL_PIXELS ("+NeoAbstractWidget.INTEGRAL_PIXELS
					+") or INTEGRAL_COORDS ("+NeoAbstractWidget.INTEGRAL_COORDS
					+") or INTEGRAL_ALL ("+NeoAbstractWidget.INTEGRAL_ALL+"). "
					+"Not to " + constraint);
					}
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setScaleConstraint(axisid, constraint);
		}
	}

	/**
	 * @see NeoAbstractWidget#setZoomBehavior
	 */
	public void setZoomBehavior(int axisid, int constraint) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set zoom behavior for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for " + axisid);
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setZoomBehavior(axisid, constraint);
		}
	}

	/**
	 * @see NeoAbstractWidget#setZoomBehavior
	 */
	public void setZoomBehavior(int axisid, int constraint, double coord) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set zoom behavior for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for " + axisid);
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setZoomBehavior(axisid, constraint, coord);
		}
	}

	/**
	 * @see NeoAbstractWidget#setZoomer
	 */
	//  public void setZoomer(int axisid, NeoAdjustable adj) {
	public void setZoomer(int axisid, Adjustable adj) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set zoomer for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for " + axisid);
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setZoomer(axisid, adj);
		}
	}

	/**
	 * @see NeoAbstractWidget#setScroller
	 */
	//  public void setScroller(int axisid, NeoAdjustable adj) {
	public void setScroller(int axisid, JScrollBar adj) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set Scroller for X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not for " + axisid);
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.setScroller(axisid, adj);
		}
	}

	/**
	 * @see NeoAbstractWidget#zoom
	 */
	public void zoom(int axisid, double zoom_scale) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can zoom along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		Enumeration e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = (NeoAbstractWidget)e.nextElement();
			w.zoom(axisid, zoom_scale);
		}
	}

	/**
	 * @see NeoAbstractWidget#setMaxZoom
	 */
	public void setMaxZoom(int axisid, double limit) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set max zoom along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		Enumeration e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = (NeoAbstractWidget)e.nextElement();
			w.setMaxZoom(axisid, limit);
		}
	}

	/**
	 * @see NeoAbstractWidget#setMinZoom
	 */
	public void setMinZoom(int axisid, double limit) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set min zoom along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		Enumeration e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = (NeoAbstractWidget)e.nextElement();
			w.setMaxZoom(axisid, limit);
		}
	}

	/**
	 * @see NeoAbstractWidget#setMaxZoom
	 */
	public double getMaxZoom(int axisid) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can get max zoom along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		// The following will throw an exception if widgets is empty.
		// This will do until we find out (and formalize) the default.
		return widgets.elementAt(0).getMaxZoom(axisid);
	}

	/**
	 * @see NeoAbstractWidget#getMinZoom
	 */
	public double getMinZoom(int axisid) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can get min zoom along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		// The following will throw an exception if widgets is empty.
		// This will do until we find out (and formalize) the default.
		return widgets.elementAt(0).getMinZoom(axisid);
	}

	/*--------  End of Zooming Implementation --------*/


	/**
	 * @see NeoAbstractWidget#scroll
	 */
	public void scroll(int axisid, double value) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid))
			throw new IllegalArgumentException(
					"Can set scroll along X ("+NeoAbstractWidget.X
					+") or Y ("+NeoAbstractWidget.Y+") axis. "
					+"Not " + axisid);
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		while (e.hasMoreElements()) {
			NeoAbstractWidget w = e.nextElement();
			w.scroll(axisid, value);
		}
	}

	public Vector<GlyphI> getItems(Object datamodel) {
		Object result = model_hash.get(datamodel);
		if (result instanceof Vector) {
			return (Vector)result;
		} else {
			Vector<GlyphI> vec = new Vector<GlyphI>();
			vec.addElement((GlyphI)result);
			return vec;
		}
	}

	/**
	 *  If there is more than one glyph associated with the datamodel,
	 *  then return glyph that was most recently associated
	 */
	public GlyphI getItem(Object datamodel) {
		Object result = model_hash.get(datamodel);
		if (result instanceof GlyphI) {
			return (GlyphI)result;
		}
		else if (result instanceof Vector && ((Vector)result).size() > 0) {
			Vector vec = (Vector)result;
			return (GlyphI)vec.elementAt(vec.size()-1);
		}
		else {
			return null;
		}
	}

	public NeoAbstractWidget getWidget(GlyphI gl) {
		Scene glyph_scene, widg_scene;
		NeoAbstractWidget widg;
		for (int i=0; i<widgets.size(); i++) {
			widg = widgets.elementAt(i);
			if (widg.getWidget(gl) == widg) { return widg; }
		}
		return null;
	}

	public void removeItem(GlyphI gl) {
		NeoAbstractWidget widg = this.getWidget(gl);
		if (widg != null) {
			widg.removeItem(gl);
		}
	}

	public void removeItem(Vector<GlyphI> vec) {
		Vector<GlyphI> glyphs = (Vector)vec.clone();
		for (int i=0; i<glyphs.size(); i++) {
			removeItem(glyphs.elementAt(i));
		}
	}

	public void clearWidget() {
		super.clearWidget();
		Enumeration<NeoAbstractWidget> e = widgets.elements();
		NeoAbstractWidget widg;
		while (e.hasMoreElements()) {
			widg = e.nextElement();
			widg.clearWidget();
		}

		// BEGIN bug fix for bug #185, 5-28-98
		glyph_hash.clear();
		model_hash.clear();
		selected.removeAllElements();
		// END bug fix for bug #185, 5-28-98
	}

	public boolean supportsSubSelection(GlyphI gl) {
		return gl.supportsSubSelection();
	}

	public void toFrontOfSiblings(GlyphI glyph) {
		NeoAbstractWidget widg = this.getWidget(glyph);
		if (widg != null) {
			widg.toFrontOfSiblings(glyph);
		}
	}

	public void toBackOfSiblings(GlyphI glyph) {
		NeoAbstractWidget widg = getWidget(glyph);
		if (widg != null) {
			widg.toBackOfSiblings(glyph);
		}
	}

	public void setDamageOptimized(boolean optimize) {
		Vector<? extends NeoAbstractWidget> widgvec = getWidgets();
		for (int i=0; i<widgvec.size(); i++) {
			Object widg = widgvec.elementAt(i);
			if (widg instanceof NeoMap) {
				((NeoMap)widg).setDamageOptimized(optimize);
			}
			else if (widg instanceof NeoContainerWidget) {
				((NeoContainerWidget)widg).setDamageOptimized(optimize);
			}
		}
	}

	public void setScrollingOptimized(boolean optimize) {
		Vector<? extends NeoAbstractWidget> widgvec = getWidgets();
		for (int i=0; i<widgvec.size(); i++) {
			NeoAbstractWidget widg = widgvec.elementAt(i);
			if (widg instanceof NeoMap) {
				((NeoMap)widg).setScrollingOptimized(optimize);
			}
			else if (widg instanceof NeoContainerWidget) {
				((NeoContainerWidget)widg).setScrollingOptimized(optimize);
			}
		}
	}

	public void heardMouseEvent(MouseEvent evt) {
		if (! (evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent e = (NeoMouseEvent)evt;
		Object source = e.getSource();
		if (! (source instanceof NeoAbstractWidget)) { return; }
		int id = e.getID();
		int x = e.getX();
		int y = e.getY();

		int location = NeoConstants.UNKNOWN;
		location = getLocation((NeoAbstractWidget)source);

		NeoMouseEvent nevt =
			new NeoMouseEvent(e, this, location, e.getCoordX(), e.getCoordY());
		// translating from internal widget pixel location to
		//     NeoContainerWidget pixel location
		Rectangle bnds = ((Component)source).getBounds();
		nevt.translatePoint(bnds.x, bnds.y);

		if (mouse_listeners.size() > 0) {
			// trying to debug problem with JPopupMenus -- GAH 3-31-99
			// problem is with a JPopup adding itself (or rather its Grabber)
			// as a mouse listener to ALL containers/children down
			// the hierarchy from the component its show is called on.
			// But this happens through the mousePressed call _here_,
			// so the listener loop as it was written would end up notifying
			// the Grabber of a mouse pressed, which would be interpreted
			// as wanting to hide the popup (when it was really caused by the
			// mouse press that created the popup in the first place).
			// Therefore trying to prevent this situation by only calling those
			// listeners that were registered as listeners _before_ the loop was started.
			// (current approach could however get screwed up by additional
			//  adding/removing of mouse listeners in response to mouse events...)
			int last_listener = mouse_listeners.size()-1;
			for (int i=0;
					(i <= last_listener) && (i < mouse_listeners.size());
					i++) {
				MouseListener ml = mouse_listeners.elementAt(i);
				if (id == MouseEvent.MOUSE_CLICKED) { ml.mouseClicked(nevt); }
				else if (id == MouseEvent.MOUSE_ENTERED) { ml.mouseEntered(nevt); }
				else if (id == MouseEvent.MOUSE_EXITED) { ml.mouseExited(nevt); }
				else if (id == MouseEvent.MOUSE_PRESSED) { ml.mousePressed(nevt); }
				else if (id == MouseEvent.MOUSE_RELEASED) { ml.mouseReleased(nevt); }
					}
		}
		if (mouse_motion_listeners.size() > 0) {
			for (int i=0; i<mouse_motion_listeners.size(); i++) {
				MouseMotionListener mml = mouse_motion_listeners.elementAt(i);
				if (id == MouseEvent.MOUSE_DRAGGED) { mml.mouseDragged(nevt); }
				else if (id == MouseEvent.MOUSE_MOVED) { mml.mouseMoved(nevt); }
			}
		}
	}

	}

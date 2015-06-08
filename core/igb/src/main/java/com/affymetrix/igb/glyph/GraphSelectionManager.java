/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.glyph;

import com.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphDragger;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoGlyphDragEvent;
import com.affymetrix.genoviz.event.NeoGlyphDragListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.ThreshGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoWidget;
import com.lorainelab.igb.genoviz.extensions.GraphGlyphUtils;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JPopupMenu;

/**
 * This class provides a popup menu for a SeqMapView to allow certain graph
 * manipulations. It is mostly an obsolete class, since most of the
 * manipulations are now done without using pop-up menus.
 *
 * @version $Id: GraphSelectionManager.java 8192 2011-05-25 17:25:14Z hiralv $
 */
public class GraphSelectionManager implements MouseListener,
        MouseMotionListener, NeoGlyphDragListener {

    final static boolean DEBUG = false;

    private GraphGlyph graph_to_scale = null;

    boolean scaling_graph = false;
    private double start_graph_height;
    private double start_mouse_ycoord;
    private GlyphDragger dragger;
    private SeqMapView gviewer;

    public GraphSelectionManager(SeqMapView smv) {
        gviewer = smv;
    }

    /**
     * Does nothing. Formerly this was used to bring-up a pop-up menu, but that
     * could cause conflicts with the other pop-up menu which is opened by the
     * SeqMapViewMouseListener. Thus now instead of opening our own pop-up, we
     * use the routine {@link #popupNotify(JPopupMenu, List, SeqSymmetry)}
     * provided by the interface ContextualPopupListener to add to the pop-up
     * menu which the SeqMapView itself constructs.
     */
    @Override
    public void mouseClicked(MouseEvent evt) {
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
    }

    @Override
    public void mouseExited(MouseEvent evt) {
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (evt instanceof NeoMouseEvent) {
            NeoMouseEvent nevt = (NeoMouseEvent) evt;
            List<GlyphI> selected = nevt.getItems();
            for (int i = selected.size() - 1; i >= 0; i--) {
                GlyphI gl = selected.get(i);
                // only allow dragging and scaling if graph is contained within an ancestor PixelFloaterGlyph...
                if (gl instanceof GraphGlyph && GraphGlyphUtils.hasFloatingAncestor(gl)) {
                    GraphGlyph gr = (GraphGlyph) gl;
                    if (nevt.isShiftDown() || nevt.isAltDown()) {
                        scaleGraph(gr, nevt);
                        break;
                    } else {
                        dragGraph(gr, nevt);
                        break;
                    }
                } else if (gl.getParent() instanceof GraphGlyph) {
                    if (DEBUG) {
                    }
                }
            }
        }
    }

    // only used for graph scaling (not for graph dragging)
    @Override
    public void mouseReleased(MouseEvent evt) {
        if (evt instanceof NeoMouseEvent) {
            NeoMouseEvent nevt = (NeoMouseEvent) evt;
            scaling_graph = false;
            graph_to_scale = null;
            ((Component) nevt.getSource()).removeMouseMotionListener(this);
        }
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
    }

    // only used for graph scaling
    //   (not for graph dragging or thresholding, those are managed by a GlyphDragger)
    @Override
    public void mouseDragged(MouseEvent evt) {
        if (!(evt instanceof NeoMouseEvent)) {
            return;
        }
        NeoMouseEvent nevt = (NeoMouseEvent) evt;
        NeoAbstractWidget widg = (NeoAbstractWidget) nevt.getSource();
        if (scaling_graph) {
            Rectangle2D.Double bbox = graph_to_scale.getCoordBox();
            double coord_diff = start_mouse_ycoord - nevt.getCoordY();
            //      System.out.println(coord_diff);
            double graph_center = bbox.y + (bbox.height / 2);
            double new_graph_height = start_graph_height + coord_diff;
            if (new_graph_height >= 0) {
                graph_to_scale.setCoords(bbox.x, graph_center - (new_graph_height / 2),
                        bbox.width, new_graph_height);
                widg.updateWidget();
            }
        }
    }

    //  public void dragGraph(GraphGlyph gl, NeoMouseEvent nevt) {
    public void dragGraph(GlyphI gl, NeoMouseEvent nevt) {
        NeoWidget widg = (NeoWidget) nevt.getSource();
        if (widg instanceof NeoMap) {
            ((NeoMap) widg).toFront(gl);
        } else {
            // toFront() is specific to NeoMap, try toFrontOfSiblings() instead
            widg.toFrontOfSiblings(gl);
        }

        dragger = new GlyphDragger((NeoAbstractWidget) nevt.getSource());
        dragger.setUseCopy(false);

        LinearTransform trans = new LinearTransform();
        LinearTransform vtrans = widg.getView().getTransform();
        //    gl.getGlobalChildTransform(widg.getView(), trans);
        if (gl instanceof ThreshGlyph) {
            gl.getParent().getGlobalTransform(widg.getView(), trans);
        } else {
            gl.getGlobalTransform(widg.getView(), trans);
        }

        trans.setTransform(vtrans.getScaleX(), 0, 0, trans.getScaleY(), vtrans.getTranslateX(), trans.getTranslateY());
        dragger.setConstraint(NeoConstants.HORIZONTAL, true);

        dragger.addGlyphDragListener(this);
        dragger.addGlyphDragListener(gviewer.getMouseListener());
        dragger.startDrag(gl, nevt, trans, false);

    }

    public void scaleGraph(GraphGlyph gl, NeoMouseEvent nevt) {

// The mouse motion listener is added here, and removed in heardGlpyhDrag()
        ((Component) nevt.getSource()).addMouseMotionListener(this);
        if (DEBUG) {
            System.out.println("trying to scale graph");
        }
        scaling_graph = true;
        graph_to_scale = gl;
        start_mouse_ycoord = nevt.getCoordY();
        start_graph_height = gl.getCoordBox().height;
    }

    @Override
    public void heardGlyphDrag(NeoGlyphDragEvent evt) {
        int id = evt.getID();
        Object src = evt.getSource();
        if (id == NeoGlyphDragEvent.DRAG_IN_PROGRESS) {
            GlyphI gl = evt.getGlyph();
            if (gl.getParent() instanceof GraphGlyph && src instanceof NeoWidget) {
                NeoWidget widg = (NeoWidget) src;
                ViewI view = widg.getView();
                GlyphI threshgl = gl;
                GraphGlyph graphgl = (GraphGlyph) threshgl.getParent();
                Rectangle2D.Double tbox = threshgl.getCoordBox();
                float new_threshold = graphgl.getGraphValue(view, tbox.y);
                if (graphgl.getThresholdDirection() == GraphState.THRESHOLD_DIRECTION_GREATER) {
                    graphgl.setMinScoreThreshold(new_threshold);
                } else {
                    graphgl.setMaxScoreThreshold(new_threshold);
                }
            }
        } else if (id == NeoGlyphDragEvent.DRAG_ENDED) {
            dragger.removeGlyphDragListener(this);

            GlyphI gl = evt.getGlyph();
            if (gl instanceof GraphGlyph && src instanceof AffyTieredMap) {
                if (((GraphGlyph) gl).getAnnotStyle().isFloatTier()) {
                    gviewer.getFloaterGlyph().checkBounds(gl, ((AffyTieredMap) src).getView());
                }
            }
        }
        // otherwise it must be DRAG_STARTED event, which can be ignored
        //   since this class called dragger.dragStart to begin with...
    }

}

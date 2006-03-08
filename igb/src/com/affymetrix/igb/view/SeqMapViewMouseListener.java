/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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
package com.affymetrix.igb.view;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.tiers.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import javax.swing.*;

/**
 *  A MouseListener for the SeqMapView.
 *
 *  This handles selection by clicking, section by rubber-banding, and the
 *  decision about when to pop-up a menu.
 *
 *  It was necessary to deviate somewhat from "best-practice" standards about
 *  how to check for the pop-up trigger and whether things happen on
 *  mousePressed() or mouseReleased() and detection of "right" mouse clicks.
 *  This is because the GenoViz SDK RubberBand interferes with some possibilities.
 *  
 *  For example, we always show the popup during mouseReleased(), never
 *  mousePressed(), because that would interfere with the rubber band.
 *  For Windows users, this is the normal behavior anyway.  For Mac and Linux
 *  users, it is not standard, but should be fine.
 */
public class SeqMapViewMouseListener implements MouseListener, NeoRubberBandListener {


  // This flag determines whether selection events are processed on
  //  mousePressed() or mouseReleased().
  //
  // Users normally expect something to happen on mousePressed(), but
  // if updateWidget() is done in mousePressed(), it can occasionally make 
  // the rubber band draw oddly.
  //
  // A solution is to move all mouse event processing into mouseReleased(),
  // as was done in earlier versions of IGB.  But since most applications
  // respond to mousePressed(), users expect something to happen then.
  //
  // A better solution would be to fix the rubber band drawing routines
  // so that they respond properly after updateWidget()
  //
  // The program should work perfectly fine with this flag true or false, 
  // the rubber band simply looks odd sometimes (particularly with a fast drag) 
  // if this flag is true.
  private boolean SELECT_ON_MOUSE_PRESSED = false;

  SeqMapView smv;
  AffyTieredMap map;

  SeqMapViewMouseListener(SeqMapView smv) {
    this.smv = smv;
    this.map = smv.seqmap;
  }

  public void mouseEntered(MouseEvent evt) { }

  public void mouseExited(MouseEvent evt) { }

  public void mouseClicked(MouseEvent evt) { 
    // reset rubber_band_start here?
  }

  public void mousePressed(MouseEvent evt) {
    // turn OFF autoscrol in mousePressed()
    if (smv.map_auto_scroller != null) {
      smv.toggleAutoScroll();
    }

    // process selections in mousePressed() or mouseReleased()
    if (SELECT_ON_MOUSE_PRESSED) processSelections(evt);
  }

  public void mouseReleased(MouseEvent evt) {
    // process selections in mousePressed() or mouseReleased()
    if (! SELECT_ON_MOUSE_PRESSED) processSelections(evt);

    //  do popup in mouseReleased() so it doesn't interfere with rubber band
    if (isOurPopupTrigger(evt)) {
      smv.showPopup((NeoMouseEvent) evt);
    }
    
    // if the GraphSelectionManager is also trying to control popup menus,
    // then there needs to be code here to prevent both this and that from
    // trying to do a popup at the same time.  But it is tricky.  So for
    // now we let ONLY this class trigger the pop-up.
  }

  void processSelections(MouseEvent evt) {
    
    if (! (evt instanceof NeoMouseEvent)) { return; }
    NeoMouseEvent nevt = (NeoMouseEvent)evt;

    Point2D.Double zoom_point = new Point2D.Double(nevt.getCoordX(), nevt.getCoordY());

    GlyphI topgl = null;
    if (! nevt.getItems().isEmpty()) {
      topgl = (GlyphI) nevt.getItems().lastElement();
      topgl = zoomCorrectedGlyphChoice(topgl, zoom_point);
    }

    // If drag began in the axis tier, then do NOT do normal selection stuff, 
    // because we are selecting sequence instead.
    // (This only really matters when SELECT_ON_MOUSE_PRESSED is false.
    //  If SELECT_ON_MOUSE_PRESSED is true, topgl will already be null
    //  because a drag can only start when you begin the drag on blank space.)
    if (startedInAxisTier()) {
      topgl = null;
    }

    // Normally, clicking will clear previons selections before selecting new things.
    // but we preserve the current selections if:
    //  shift (Add To) or alt (Toggle) or pop-up (button 3) is being pressed
    boolean preserve_selections = 
      (isAddToSelectionEvent(nevt) || isToggleSelectionEvent(nevt) || isOurPopupTrigger(nevt));

    // Special case:  if pop-up button is pressed on top of a single item and
    // that item is not already selected, then do not preserve selections
    if (topgl != null && isOurPopupTrigger(nevt)) {
        if (isAddToSelectionEvent(nevt)) {
          // This particular special-special case is really splitting hairs....
          // It would be ok to get rid of it.
          preserve_selections = true;
        } else if (! map.getSelected().contains(topgl)) {
          // This is the important special case.  Needs to be kept.
          preserve_selections = false;
        }
    }

    if ( ! preserve_selections) {
      smv.clearSelection(); // Note that this also clears the selected sequence        
    }

    // seems no longer needed
    //map.removeItem(match_glyphs);  // remove all match glyphs in match_glyphs vector

    if (topgl != null) {
      if (isToggleSelectionEvent(nevt) && map.getSelected().contains(topgl)) {
        map.deselect(topgl);
      }
      else {
        map.select(topgl);
      }
    }

    boolean nothing_changed = (preserve_selections && (topgl == null));
    boolean selections_changed = ! nothing_changed;
        
    if (smv.show_edge_matches && selections_changed)  {
      smv.doEdgeMatching(map.getSelected(), false);
    }
    smv.setZoomSpotX(zoom_point.getX());
    smv.setZoomSpotY(zoom_point.getY());

    map.updateWidget(); 

    if (selections_changed) {
      smv.postSelections();
    }
  }

  /**
   *  Tries to determine the glyph you really wanted to choose based on the
   *  one you clicked on.  Usually this will be the glyph you clicked on,
   *  but when the zoom level is such that the glyph is very small, this
   *  assumes you probably wanted to pick the parent glyph rather than
   *  one of its children.
   *
   *  @param topgl a Glyph
   *  @param zoom_point  the location where you clicked; if the returned glyph
   *   is different from the given glyph, the returned zoom_point will be
   *   at the center of that returned glyph, otherwise it will be unmodified.
   *   This parameter should not be supplied as null. 
   *  @return a Glyph, and also modifies the value of zoom_point
   */
  GlyphI zoomCorrectedGlyphChoice(GlyphI topgl, java.awt.geom.Point2D.Double zoom_point) {
    if (topgl == null) { return null; }
    // trying to do smarter selection of parent (for example, transcript)
    //     versus child (for example, exon)
    // calculate pixel width of topgl, if <= 2, and it has no children,
    //   and parent glyphs has pixel width <= 10, then select parent instead of child..
    Rectangle pbox = new Rectangle();
    Rectangle2D cbox = topgl.getCoordBox();
    map.getView().transformToPixels(cbox, pbox);

    if (pbox.width <= 2) {
      // if the selection is very small, move the x_coord to the center
      // of the selection so we can zoom-in on it.
      zoom_point.x = cbox.x + cbox.width/2;
      zoom_point.y = cbox.y + cbox.height/2;
    }

    if ((pbox.width <= 2) && (topgl.getChildCount() == 0) && (topgl.getParent() != null) ) {
      // Watch for null parents:
      // The reified Glyphs of the FlyweightPointGlyph made by OrfAnalyzer2 can have no parent
      cbox = topgl.getParent().getCoordBox();
      map.getView().transformToPixels(cbox, pbox);
      if (pbox.width <= 10) {
        topgl = topgl.getParent();
        if (pbox.width <= 2) { // Note: this pbox has new values than those tested above
          // if the selection is very small, move the x_coord to the center
          // of the selection so we can zoom-in on it.
          zoom_point.x = cbox.x + cbox.width/2;
          zoom_point.y = cbox.y + cbox.height/2;
        }
      }
    }

    return topgl;
  }


  /** Checks whether the mouse event is something that we consider to be
   *  a pop-up trigger.  (This has nothing to do with MouseEvent.isPopupTrigger()).
   *  Checks for isMetaDown() and isControlDown() to try and
   *  catch right-click simulation for one-button mouse operation on Mac OS X.
   */
  static boolean isOurPopupTrigger(MouseEvent evt) {
    if (evt == null) {return false;}
    else if (isToggleSelectionEvent(evt)) return false;
    else return (evt.isControlDown() ||  evt.isMetaDown() || 
         ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0 ));
  }

  /** Checks whether this the sort of mouse click that should preserve 
      and add to existing selections.  */
  static boolean isAddToSelectionEvent(MouseEvent evt) {
    return (evt != null && (evt.isShiftDown()));
  }

  /** Checks whether this the sort of mouse click that should toggle selections. */
  static boolean isToggleSelectionEvent(MouseEvent evt) {
    //Make sure this does not conflict with pop-up trigger
    boolean b = (evt != null && evt.isControlDown() && evt.isShiftDown());
    return (b);
  }

  private transient MouseEvent rubber_band_start = null;

  public void rubberBandChanged(NeoRubberBandEvent evt) {        
    /*
     * Note that because using SmartRubberBand, rubber banding will only happen
     *   (and NeoRubberBandEvents will only be received) when the orginal mouse press to
     *    start the rubber band doesn't land on a hitable glyph
     */

    if (isOurPopupTrigger(evt)) { 
      return;
      // This doesn't stop the rubber band from being drawn, because you would
      // have to do that inside the SmartRubberBand itself.  But if you don't
      // have this return statement here, it is possible for the selections
      // reported in the pop-up menu to differ from what appears to be selected
      // visually.  This is because the mouseReleased event can get processed
      // before the selection happens here through the rubber-band methods
    }

    if (evt.getID() == NeoRubberBandEvent.BAND_START) {
      rubber_band_start = evt;
    }
    if (evt.getID() == NeoRubberBandEvent.BAND_END) {
      Rectangle2D cbox = new Rectangle2D();
      Rectangle pbox = evt.getPixelBox();
      map.getView().transformToCoords(pbox, cbox);

      // setZoomSpot is best if done before updateWidget
      smv.setZoomSpotX(cbox.x + cbox.width);
      smv.setZoomSpotY(cbox.y + cbox.height);

      if (startedInAxisTier()) {
        // started in axis tier: user is trying to select sequence residues

        if (pbox.width >= 2 && pbox.height >=2) {
          int seq_select_start = (int)Math.round(cbox.x);
          int seq_select_end = (int)Math.round(cbox.x + cbox.width);

          SeqSymmetry new_region = new SingletonSeqSymmetry(seq_select_start, seq_select_end, smv.aseq);
          smv.setSelectedRegion(new_region, true);
        }
        else {
          // This is optional: clear selected region if drag is very small distance
          smv.setSelectedRegion(null, true);
        }

      } else {
        // started outside axis tier: user is trying to select glyphs

        doTheSelection(map.getItemsByCoord(cbox), rubber_band_start);
      }

      rubber_band_start = null; // for garbage collection
    }
  }

  // did the most recent drag start in the axis tier?
  boolean startedInAxisTier() {
    TierGlyph axis_tier = smv.getAxisTier();
    boolean started_in_axis_tier = (rubber_band_start != null ) &&
      (axis_tier != null) &&
      axis_tier.inside(rubber_band_start.getX(), rubber_band_start.getY());
    return started_in_axis_tier;
  }
  
  boolean isInAxisTier(GlyphI g) {
    TierGlyph axis_tier = smv.getAxisTier();
    if (axis_tier == null) return false;
    
    GlyphI p = g;
    while ( p != null) {
      if (p == axis_tier) return true;
      p = p.getParent();
    }
    return false;
  }
  
  void doTheSelection(Vector glyphs, MouseEvent evt) {

    boolean something_changed = true;

    // Remove any children of the axis tier (like contigs) from the selections.
    // Selecting contigs is something you usually do not want to do.  It is
    // much more likely that if someone dragged across the axis, they want to
    // select glyphs in tiers above and below but not IN the axis.
    ListIterator li = glyphs.listIterator();
    while (li.hasNext()) {
      GlyphI g = (GlyphI) li.next();
      if (isInAxisTier(g)) {
        li.remove();
      }
    }
    
    // Now correct for the fact that we might be zoomed way-out.  In that case
    // select only the parent glyphs (RNA's), not all the little children (Exons).
    Point2D.Double zoom_point = new Point2D.Double(0,0); // dummy variable, value not used
    Vector corrected = new Vector(glyphs.size());
    for (int i=0; i<glyphs.size(); i++) {
      GlyphI g = (GlyphI) glyphs.get(i);
      GlyphI zc = zoomCorrectedGlyphChoice(g, zoom_point);
      if (! corrected.contains(zc)) {corrected.add(zc);}
    }
    glyphs = corrected;
    
    if (isToggleSelectionEvent(evt)) {
      if (glyphs.isEmpty()) {
        something_changed = false;
      }
      toggleSelections(map, glyphs);
    } else if (isAddToSelectionEvent(evt)) {
      if (glyphs.isEmpty()) {
        something_changed = false;
      }
      map.select(glyphs);
    } else {
      something_changed = true;
      smv.clearSelection();
      map.select(glyphs);
    }
    if (smv.show_edge_matches && something_changed) {
      smv.doEdgeMatching(map.getSelected(), false);
    }
    map.updateWidget();
    
    if (something_changed) {
      smv.postSelections();
    }
  }

  void toggleSelections(NeoMap map, Collection glyphs) {
    java.util.List current_selections = map.getSelected();
    Iterator iter = glyphs.iterator();
    while (iter.hasNext()) {
      GlyphI g = (GlyphI) iter.next();
      if (current_selections.contains(g)) {
        map.deselect(g);
      } else {
        map.select(g);
      }
    }
  }

}

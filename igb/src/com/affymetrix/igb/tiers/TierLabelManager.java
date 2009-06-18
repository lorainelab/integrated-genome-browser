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

package com.affymetrix.igb.tiers;

import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.igb.glyph.GraphGlyph;

public final class TierLabelManager {

  AffyLabelledTierMap tiermap;
  AffyTieredMap labelmap;
  JPopupMenu popup;

  int xoffset_pop = 10;
  int yoffset_pop = 0;

  /**
   *  Determines whether selecting a tier label of a tier that contains only
   *  GraphGlyphs should cause the graphs in that tier to become selected.
   */
  boolean do_graph_selections = false;
  
  public TierLabelManager(AffyLabelledTierMap map) {
    tiermap = map;
    popup = new JPopupMenu();

    labelmap = tiermap.getLabelMap();
    labelmap.addMouseListener(this.mouse_listener);

    labelmap.getScene().setSelectionAppearance(SceneI.SELECT_OUTLINE);
    labelmap.setPixelFuzziness(0); // there are no gaps between tiers, need no fuzziness
  }
  
  /** Returns a list of TierGlyph items representing the selected tiers. */
  public List<TierGlyph> getSelectedTiers() {
    List<GlyphI> selected_labels = getSelectedTierLabels();
    int sel_count = selected_labels.size();
    List<TierGlyph> selected_tiers = new ArrayList<TierGlyph>();

    for (int i=0; i<sel_count; i++) {
      // TierGlyph should be data model for tier label, access via lable.getInfo()
      GlyphI tlg = selected_labels.get(i);
      TierGlyph tier = (TierGlyph)tlg.getInfo();
      selected_tiers.add(tier);
    }
    return selected_tiers;
  }
  
  /** Returns a list of selected TierLabelGlyph items. */
  public List getSelectedTierLabels() {
    return labelmap.getSelected();
  }

  /** Returns a list of all TierLabelGlyph items. */
  public List<TierLabelGlyph> getAllTierLabels() {
    return tiermap.getTierLabels();
  }

  /** Selects all non-hidden tiers. */
  public void selectAllTiers()  {
    List labels = getAllTierLabels();
    int tiercount = labels.size();
    for (int i=0; i<tiercount; i++) {
      TierLabelGlyph tierlabel = (TierLabelGlyph) labels.get(i);
      if (tierlabel.getReferenceTier().getAnnotStyle().getShow()) {
        labelmap.select(tierlabel);
      }
    }
    doGraphSelections(labelmap);
    //labelmap.updateWidget();
    tiermap.updateWidget(); // make sure selections becomes visible
  }
  
  /**
   *  Determines whether selecting a tier label of a tier that contains only
   *  GraphGlyphs should cause the graphs in that tier to become selected.
   */
  public void setDoGraphSelections(boolean b) {
    do_graph_selections = b;
  }
  
  void doGraphSelections(AffyTieredMap labelmap) {
    if (! do_graph_selections) {
      return;
    }

    List<TierLabelGlyph> labels = getAllTierLabels();
    boolean selections_changed = false;
    
    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    
    ArrayList<SeqSymmetry> symmetries = new ArrayList<SeqSymmetry>();
    symmetries.addAll(gmodel.getSelectedSymmetriesOnCurrentSeq());

    for (int j = 0; j<getAllTierLabels().size(); j++) {
      TierLabelGlyph tierlabel = labels.get(j);
      TierGlyph tg = tierlabel.getReferenceTier();
      int child_count = tg.getChildCount();
      if (child_count > 0 && tg.getChild(0) instanceof GraphGlyph) {
        // It would be nice if we could assume that a tier contains only
        // GraphGlyph's or only non-GraphGlyph's, but that is not true.
        // 
        // When graph thresholding is turned on, there can be one or
        // two other EfficientFillRectGlyphs that are a child of the tier glyph 
        // but are not instances of GraphGlyph.  They can be ignored.
        // (I would like to change them to be children of the GraphGlyph, but
        // haven't done it yet.)
        
        // Assume that if first child is a GraphGlyph, then so are all others
        for (int i=0; i<child_count; i++) {
          Object ob = tg.getChild(i);
          if (! (ob instanceof GraphGlyph)) {
            // ignore the glyphs that are not GraphGlyph's
            continue;
          }
          GraphGlyph child = (GraphGlyph) ob;
          SeqSymmetry sym = (SeqSymmetry) child.getInfo();
          // sym will be a GraphSym, but we don't need to cast it
          if (tierlabel.isSelected()) {
            if (! symmetries.contains(sym)) {
              symmetries.add(sym);
              selections_changed = true;
            }
          } else if (symmetries.contains(sym)) {
            symmetries.remove(sym);
            selections_changed = true;
          }
        }
      }
    }

    //if (selections_changed) {
      gmodel.setSelectedSymmetries(symmetries, tiermap);
    //}
  }

  /** Gets all the GraphGlyph objects inside the given list of TierLabelGlyph's. */
  public static List<GraphGlyph> getContainedGraphs(List<TierLabelGlyph> tier_label_glyphs) {
    List<GraphGlyph> result = new ArrayList<GraphGlyph>();
		for (TierLabelGlyph tlg : tier_label_glyphs) {
      result.addAll(getContainedGraphs(tlg));
    }
    return result;
  }
  
  /** Gets all the GraphGlyph objects inside the given TierLabelGlyph. */
  public static List<GraphGlyph> getContainedGraphs(TierLabelGlyph tlg) {
    ArrayList<GraphGlyph> result = new ArrayList<GraphGlyph>();
    TierGlyph tier = (TierGlyph) tlg.getInfo();
    IAnnotStyle style = tier.getAnnotStyle();
    int child_count = tier.getChildCount();
    if ( child_count > 0 && tier.getChild(0) instanceof GraphGlyph) {
      for (int j=0; j<child_count; j++) {
        result.add((GraphGlyph) tier.getChild(j));
      }
    }
    return result;
  }
  
  /** Gets the index of a given tier. Note that is a TierGlyph, not a TierLabelGlyph. */
  public int getTierIndex(TierGlyph atier) {
    return tiermap.getTierIndex(atier);
  }
  
  /** Restores multiple hidden tiers and then repacks.
   *  @param tier_labels  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
   *  @param full_repack  Whether to do a full repack
   *  @param fit_y  Whether to change the zoom to fit all the tiers in the view
   *  @see #repackTheTiers(boolean, boolean)
   */
  public void showTiers(List<TierLabelGlyph> tier_labels, boolean full_repack, boolean fit_y) {
    Iterator<TierLabelGlyph> iter = tier_labels.iterator();
    while (iter.hasNext()) {
      TierLabelGlyph g = iter.next();
      if (g.getInfo() instanceof TierGlyph) {
        TierGlyph tier = (TierGlyph) g.getInfo();
        tier.getAnnotStyle().setShow(true);
      }
    }

    repackTheTiers(full_repack, fit_y);
  }

  /** Hides multiple tiers and then repacks.
   *  @param tier_labels  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
   *  @param fit_y  Whether to change the zoom to fit all the tiers in the view
   */
  public void hideTiers(List tier_labels, boolean full_repack, boolean fit_y) {
    Iterator iter = tier_labels.iterator();
    while (iter.hasNext()) {
      GlyphI g = (GlyphI) iter.next();
      if (g.getInfo() instanceof TierGlyph) {
        TierGlyph tier = (TierGlyph) g.getInfo();
        tier.getAnnotStyle().setShow(false);
      }
    }

    repackTheTiers(full_repack, fit_y);
  }
  
  void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed, boolean full_repack, boolean fit_y) {
    for (int i=0; i<tier_labels.size(); i++) {
      TierLabelGlyph tlg = tier_labels.get(i);
      IAnnotStyle style = tlg.getReferenceTier().getAnnotStyle();
      if (style.getExpandable()) {
        style.setCollapsed(collapsed);

        // When collapsing, make them all be the same height as the tier.
        // (this is for simplicity in figuring out how to draw things.)
        if (collapsed) {
          List<GraphGlyph> graphs = getContainedGraphs(tlg);
          double tier_height = style.getHeight();
          for (int j=0; j<graphs.size(); j++) {
            GraphGlyph graph = graphs.get(j);
            graph.getGraphState().getTierStyle().setHeight(tier_height);
          }
        }
        
        for (int j=0; j<tlg.getReferenceTier().getScene().getViews().size(); j++) {
          tlg.getReferenceTier().pack(tlg.getReferenceTier().getScene().getViews().get(j));
        }
      }
    }

    repackTheTiers(full_repack, fit_y);
  }

  public void finishDragging(TierLabelGlyph glyph) {
    sortTiers();
  }

  /**
   *  Sorts all tiers and then calls packTiers() and updateWidget().
   */
  public void sortTiers() {
    List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
    orderTierLabels(label_glyphs);
    orderTiersByLabels(label_glyphs);

    // then repack of course (tiermap repack also redoes labelmap glyph coords...)
    tiermap.packTiers(false, true, false);
    tiermap.updateWidget();
  }
  
  /** Comparator class needed to sort tiers based on label placement. */
  public final class MinYSorter implements Comparator<GlyphI> {
    public int compare(GlyphI glyph1, GlyphI glyph2) {
      Rectangle2D box1 = glyph1.getCoordBox();
      Rectangle2D box2 = glyph2.getCoordBox();
      if (box1.y < box2.y) { return -1; }
      else if (box1.y > box2.y) { return 1; }
      else { return 0; }
    }
  }
  
  Comparator<GlyphI> ysorter = new MinYSorter();
  
  Comparator<GlyphI> tier_sorter = ysorter;
  
  /**
   *  Set a Comparator to be used to re-sort tiers after the user drags a tier.
   *  The default Comparator, which is probably good for all cases, sorts based
   *  on the y-position of the top of each tier after the mouse is released.
   *  The actual sorting happens in {@link #orderTierLabels(List)}.
   */
  public void setTierSorter(Comparator<GlyphI> c) {
    if (c == null) {
      tier_sorter = null;
    } else {
      tier_sorter = c;
    }
  }
  
  /**
   *  Called after a tier label has been dragged, this can be used to
   *  re-sort the given List of Tier Label Glyphs.  The List is
   *  sorted in-place.  Typically, do not use this directly, but
   *  call {@link #sortTiers()} instead, which will call this and then
   *  call {@link #orderTiersByLabels(List)}.
   */
  public void orderTierLabels(List<TierLabelGlyph> label_glyphs) {
    if (tier_sorter != null) {
      Collections.sort(label_glyphs, tier_sorter);
    }
  }

  /** Re-orders the map tiers based on the order of the label tier 
   *  glyphs in the given list.  Will be called after orderTierLabels() 
   *  has determined the label order.  Can also be called from an external class.
   */
  public void orderTiersByLabels(List<TierLabelGlyph> label_glyphs) {
      // mucking directly with tiermap's tier Vector, which is not
      //     the cleanest way to do this, but is efficient...
      int tierCount = label_glyphs.size();
      Vector<TierGlyph> tiervec = tiermap.getAllTiers();
      tiervec.removeAllElements();
      for (int i=0; i<tierCount; i++) {
        GlyphI label = label_glyphs.get(i);
        TierGlyph tier = (TierGlyph)label.getInfo();
        tiervec.add(tier);
      }
  }

  /**
   *  Repacks tiers.  Should be called after hiding or showing tiers or
   *  changing their heights.
   */
  public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
    tiermap.repackTheTiers(full_repack, stretch_vertically);
  }
  
  List<PopupListener> popup_listeners = new ArrayList<PopupListener>();
  
  public void addPopupListener(PopupListener p) {
    popup_listeners.add(p);
  }
  
  public void removePopupListener(PopupListener p) {
    popup_listeners.remove(p);
  }

  /** Removes all elements from the popup, then notifies all {@link TierLabelManager.PopupListener}
   *  objects (which may add items to the menu), then displays the popup
   *  (if it isn't empty).
   */
  public void doPopup(MouseEvent e) {
    popup.removeAll();
    
    for (int i=popup_listeners.size() -1 ; i >= 0; i--) {
      PopupListener pl = popup_listeners.get(i);
      pl.popupNotify(popup, this);
    }
    
    if (popup.getComponentCount() > 0) {
      popup.show(labelmap, e.getX() + xoffset_pop, e.getY() + yoffset_pop);
    }
  }  
  

  /** An interface that lets listeners modify the popup menu before it is shown. */
  public interface PopupListener {
    
    /** Called before the {@link TierLabelManager} popup menu is displayed.
     *  The listener may add elements to the popup menu before it gets displayed.
     */
    public void popupNotify(JPopupMenu popup, TierLabelManager handler);
  }

  
  MouseListener mouse_listener = new MouseListener() {
    TierLabelGlyph dragging_label = null;

    public void mouseEntered(MouseEvent evt) { }
    public void mouseExited(MouseEvent evt) { }

    /** Tests whether the mouse event is due to the 3rd button.
     *  (For the sake of Macintosh, considers Meta key and Control key as
     *  simulation of 3rd button.)
     */
    boolean isOurPopupTrigger(MouseEvent evt) {
      int mods = evt.getModifiers();
      return ( evt.isMetaDown() || evt.isControlDown() ||
           ((mods & InputEvent.BUTTON3_MASK) != 0)  );
    }

    public void mouseClicked(MouseEvent evt) {}

    public void mousePressed(MouseEvent evt) {
      if (evt instanceof NeoMouseEvent && evt.getSource() == labelmap) {
        NeoMouseEvent nevt = (NeoMouseEvent)evt;
        Vector selected_glyphs = nevt.getItems();
        GlyphI topgl = null;
        if (!selected_glyphs.isEmpty()) {
          topgl = (GlyphI) selected_glyphs.lastElement();
        }
        // Normally, clicking will clear previons selections before selecting new things.
        // but we preserve the current selections if:
        //  1. shift or alt key is pressed, or
        //  2. the pop-up key is being pressed
        //     2a. on top of nothing
        //     2b. on top of something previously selected
        boolean preserve_selections = false;
        if (nevt.isAltDown() || nevt.isShiftDown()) {
          preserve_selections = true;
        }
        else if (topgl != null && isOurPopupTrigger(nevt)) {
          if (labelmap.getSelected().contains(topgl)) {
            preserve_selections = true;
          }
        }
        if (! preserve_selections ) {
          labelmap.clearSelected();
        }
        Vector selected = nevt.getItems();
        labelmap.select(selected);
        doGraphSelections(labelmap);
//        labelmap.updateWidget();
        tiermap.updateWidget(); // make sure selections becomes visible
        if ( isOurPopupTrigger(evt)  ) {
          doPopup(evt);
        }
        else if (selected.size() > 0) {
          // take glyph at end of selected, just in case there is more
          //    than one -- the last one should be on top...
          TierLabelGlyph gl = (TierLabelGlyph) selected.elementAt(selected.size()-1);
          labelmap.toFront(gl);
          dragLabel(gl, nevt);
        }
      }
    }

    // if a tier has been dragged, then try to sort out rearrangement of tiers
    //    in tiermap based on new positions of labels in labelmap
    public void mouseReleased(MouseEvent evt) {
      if (evt.getSource() == labelmap && dragging_label != null) {
        finishDragging(dragging_label);
        dragging_label = null;
      }      
    }

    void dragLabel(TierLabelGlyph gl, NeoMouseEvent nevt) {
      dragging_label = gl;
      GlyphDragger dragger = new GlyphDragger((NeoWidgetI)nevt.getSource());
      dragger.setUseCopy(false);
      dragger.startDrag(gl, nevt);
      dragger.setConstraint(NeoWidgetI.HORIZONTAL, true);
    }
  }; // end of mouse listener class
}

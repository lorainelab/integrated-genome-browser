/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.TypedSym;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.genometry.NibbleBioSeq;
import com.affymetrix.igb.genometry.Versioned;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.util.CharIterator;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.util.WebBrowserControl;
import com.affymetrix.igb.util.UnibrowControlUtils;
import com.affymetrix.igb.util.ObjectUtils;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SeqSymStartComparator;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SmartAnnotBioSeq;
import com.affymetrix.igb.genometry.TypeContainerAnnot;
import com.affymetrix.igb.event.SeqSelectionListener;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SeqModifiedListener;
import com.affymetrix.igb.event.SeqSelectionEvent;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.SeqModifiedEvent;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.das2.Das2FeatureRequestSym;

public class OverView extends JPanel
  implements AnnotatedSeqViewer, SeqSelectionListener,
	     GroupSelectionListener, SeqModifiedListener, SymSelectionListener {

  static final boolean DIAGNOSTICS = false;
  static final boolean DEBUG_TIERS = false;

  //static Color almost_black = new Color(20, 20, 20);
  public static final String PREF_DEFAULT_ANNOT_COLOR = "Default annotation color";
  public static final String PREF_DEFAULT_BACKGROUND_COLOR = "Default background color";
  /** Name of a boolean preference for whether the hairline lable should be on. */
  public static final String PREF_HAIRLINE_LABELED = "Hairline Label On";
  public static final Color default_axis_color = Color.BLACK;


  SingletonGenometryModel gmodel = IGB.getGenometryModel();
  AnnotatedBioSeq aseq;
  /**
   *  a virtual sequence that maps the AnnotatedBioSeq aseq to the map coordinates.
   *  if the mapping is identity, then:
   *     vseq == aseq OR
   *     vseq.getComposition().getSpan(aseq) = SeqSpan(0, aseq.getLength(), aseq)
   *  if the mapping is reverse complement, then:
   *     vseq.getComposition().getSpan(aseq) = SeqSpan(aseq.getLength(), 0, aseq);
   *
   */
  BioSeq viewseq;
  // mapping of annotated seq to virtual "view" seq
  MutableSeqSymmetry seq2viewSym;
  SeqSymmetry[] transform_path;

  /** If true, remove empty tiers from map, but not from method2ftier and method2rtier,
   *  when changing sequence.  Thus generally remembers the relative ordering of tiers.
   */
  boolean remember_tiers = true;
  boolean hairline_is_labeled = false;
  MapViewGlyphFactoryI default_glyph_factory = new GenericAnnotGlyphFactory();
  JFrame frm;
  AffyTieredMap seqmap;
  UnibrowHairline hairline = null;

  /** Hash of method names (lower case) to forward tiers */
  Map method2ftier = new HashMap();
  /** Hash of method names (lower case) to reverse tiers */
  Map method2rtier = new HashMap();
  /** hash of GraphStates to TierGlyphs,
      ( for those GraphStates where state.getFloatGraph() = false))
  */
  Map gstate2tier = new HashMap();
  Map meth2factory = (Map)IGB.getIGBPrefs().get(XmlPrefsParser.MATCH_FACTORIES);
  Map regex2factory = (Map)IGB.getIGBPrefs().get(XmlPrefsParser.REGEX_FACTORIES);
  Map graf2factory = new HashMap();   // hash of graph syms to graph factories
  TierLabelManager tier_manager;

  /** Constructor. By default, does not add popup menu items. */
  public OverView() {
    this(false);
  }

  /**
   * Constructor.
   */
  public OverView(boolean add_popups) {
    seqmap = new AffyLabelledTierMap(false, false);
    NeoMap label_map = ((AffyLabelledTierMap)seqmap).getLabelMap();
    label_map.setSelectionAppearance( SceneI.SELECT_OUTLINE );
    Color bg = Color.BLACK;
    seqmap.setMapColor(bg);
    //    map.setScrollingOptimized(true);
    seqmap.getNeoCanvas().setDoubleBuffered(false);
    //    seqmap.setReshapeBehavior(seqmap.X, seqmap.NONE);

    tier_manager = new TierLabelManager((AffyLabelledTierMap)seqmap);
    if (add_popups) {
      tier_manager.addPopupListener(new TierArithmetic(tier_manager, this));
      tier_manager.addPopupListener(new SeqMapViewPopup(tier_manager, this));
    }
    seqmap.setSelectionAppearance( SceneI.SELECT_OUTLINE );
    SmartRubberBand srb = new SmartRubberBand(seqmap);
    seqmap.setRubberBand(srb);
    //  seqmap.addRubberBandListener(mouse_listener);
    srb.setColor(new Color(100, 100, 255));

    //    GraphSelectionManager graph_manager = new GraphSelectionManager(this);
    //    seqmap.addMouseListener(graph_manager);
    this.setLayout(new BorderLayout());
    //    this.add("North", overviewCB);
    this.add("Center", seqmap);
    //    UnibrowPrefsUtil.getTopNode().addPreferenceChangeListener(pref_change_listener);
  }

  public void setFrame(JFrame frm) { this.frm = frm; }
  public JFrame getFrame() { return frm; }

  public TierLabelManager getTierManager() { return tier_manager; }

  Map getFactoryHash() { return meth2factory; }
  Map getForwardTierHash() { return method2ftier; }
  Map getReverseTierHash() { return method2rtier; }

  /** A Map of GraphState to TierGlyph */
  public Map getGraphStateTierHash() { return gstate2tier; }
  public Map getGraphFactoryHash() { return graf2factory; }

  TransformTierGlyph axis_tier;
  public TransformTierGlyph getAxisTier() { return axis_tier; }

  /** Set up a tier with fixed pixel height and place axis in it. */
  TransformTierGlyph addAxisTier(int tier_index) {
    return null;
  }

  public void clear() {
    seqmap.clearWidget();
    aseq = null;
    method2rtier = new HashMap();
    method2ftier = new HashMap();
    gstate2tier = new HashMap();
    seqmap.updateWidget();
    GenericGraphGlyphFactory.clear();
  }

  /**
   *  Clears the graphs, and reclaims some memory.
   */
  public void clearGraphs() {
    if (aseq instanceof MutableAnnotatedBioSeq) {
      MutableAnnotatedBioSeq mseq = (MutableAnnotatedBioSeq)aseq;
      int acount = mseq.getAnnotationCount();
      for (int i=acount-1; i>=0; i--) {
	SeqSymmetry annot = mseq.getAnnotation(i);
	if (annot instanceof GraphSym) {
	  mseq.removeAnnotation(annot);
	}
      }
    }
    else {
      System.err.println("Current annotated seq is not mutable, cannot call SeqMapView.clearGraphs()!");
    }
    //Make sure the graph is un-selected in the genometry model, to allow GC
    gmodel.clearSelectedSymmetries(this);
    setAnnotatedSeq(aseq, false, true);
  }

  /** Sets the sequence; if null, has the same effect as calling clear(). */
  public void setAnnotatedSeq(AnnotatedBioSeq seq) {
    setAnnotatedSeq(seq, false, false);
  }

  /**
   *   Sets the sequence.
   *   If sequence is null, has the same effect as calling clear().
   *
   *</pre>
   *   @param preserve_selection  if true, then try and keep same selection
   *   @param preserve_view  if true, then try and keep same scroll and zoom / scale and offset...
   */
  public void setAnnotatedSeq(AnnotatedBioSeq seq, boolean preserve_selection, boolean preserve_view) {
    if (seq == null) {
      clear();
      return;
    }
    com.affymetrix.genoviz.util.Timer tim = new com.affymetrix.genoviz.util.Timer();
    com.affymetrix.genoviz.util.Memer mem = new com.affymetrix.genoviz.util.Memer();
    tim.start();
    boolean same_seq = ((seq == this.aseq) && (seq != null));

    ArrayList temp_tiers = null;
    int axis_index = 0;
    boolean axis_was_hidden = false;
    java.util.List old_selections = Collections.EMPTY_LIST;

    if (same_seq || remember_tiers) {
      // stash annotation tiers for proper state restoration after resetting for same seq
      //    (but presumably added / deleted / modified annotations...)
      temp_tiers = new ArrayList();
      // copying map tiers to separate list to avoid problems when removing tiers
      //   (and thus modifying map.getTiers() list -- could probably deal with this
      //    via iterators, but feels safer this way...)
      ArrayList cur_tiers = new ArrayList(seqmap.getTiers());
      for (int i=0; i<cur_tiers.size(); i++) {
        TierGlyph tg = (TierGlyph)cur_tiers.get(i);
        if (tg == axis_tier) {
          if (DEBUG_TIERS)  { System.out.println("removing axis tier from temp_tiers"); }
          axis_index = i;
          axis_was_hidden = (axis_tier.getState() == TierGlyph.HIDDEN);
        }
        else {
          tg.removeAllChildren();
          temp_tiers.add(tg);
          if (DEBUG_TIERS)  { System.out.println("removing tier from map: " + tg.getLabel()); }
          seqmap.removeTier(tg);
        }
      }
    } else {
      method2rtier = new HashMap();
      method2ftier = new HashMap();
      gstate2tier = new HashMap();
    }

    seqmap.clearWidget();
    seqmap.clearSelected(); // may already be done by map.clearWidget()

    aseq = seq;

    viewseq = aseq;
    seq2viewSym = null;
    transform_path = null;
    seqmap.setMapRange(0, aseq.getLength());

    // The hairline needs to be among the first glyphs added,
    // to keep it from interfering with selection of other glyphs.
    if (hairline != null) { hairline.destroy(); }
    hairline = new UnibrowHairline(seqmap);
    hairline.getShadow().setLabeled(hairline_is_labeled);

    // add back in previous annotation tiers (with all children removed)
    if (temp_tiers != null) {
      if (DEBUG_TIERS)  {
	System.out.println("same seq, trying to add back old tiers (after removing children)");
      }
      for (int i=0; i<temp_tiers.size(); i++) {
	TierGlyph tg = (TierGlyph)temp_tiers.get(i);
	if (DEBUG_TIERS)  {
	  System.out.println("adding back tier: " + tg.getLabel() + ", scene = " + tg.getScene());
	}
        // Reset tier properties: this is mainly needed to reset the background color
        if (tg.getAnnotStyle() != null) {tg.setStyle(tg.getAnnotStyle());}
        seqmap.addTier(tg);
      }
    }
    temp_tiers.clear(); // redundant hint to garbage collection

    TransformTierGlyph at = addAxisTier(axis_index);
    if (at != null && axis_was_hidden) { at.setState(TierGlyph.HIDDEN); }
    addAnnotationTiers();
    //    removeEmptyTiers();
    seqmap.repack();

    // notifyPlugins();
    seqmap.stretchToFit(true, true);
    seqmap.updateWidget();
    if (DIAGNOSTICS) {
      System.out.println("Time to convert models to display: " + tim.read()/1000f);
    }
  }


  void addAnnotationTiers() {
    int annotCount = aseq.getAnnotationCount();
    for (int i=0; i<annotCount; i++) {
      SeqSymmetry annotSym = aseq.getAnnotation(i);
      if (annotSym instanceof SymWithProps) {
	addAnnotationGlyphs(annotSym);
      }
    }

    if (aseq instanceof CompositeBioSeq &&
	((CompositeBioSeq)aseq).getComposition() != null) {
      // muck with aseq, seq2viewsym, transform_path to trick addAnnotationTiers(),
      //   addLeafsToTier(), addToTier(), etc. into mapping from compositon sequences
      AnnotatedBioSeq cached_aseq = aseq;
      MutableSeqSymmetry cached_seq2viewSym = seq2viewSym;
      SeqSymmetry[] cached_path = transform_path;

      SeqSymmetry comp = ((CompositeBioSeq)aseq).getComposition();
      // assuming a two-level deep compositioin hierarchy for now...
      //   need to make more recursive at some point...
      //   (or does recursive call to addAnnotationTiers already give us full recursion?!!)
      int scount = comp.getChildCount();
      for (int i=0; i<scount; i++) {
	SeqSymmetry csym = comp.getChild(i);
	// return seq in a symmetry span that _doesn't_ match aseq
	BioSeq cseq = SeqUtils.getOtherSeq(csym, cached_aseq);
	if (cseq instanceof AnnotatedBioSeq) {
	  aseq = (AnnotatedBioSeq)cseq;
	  if (cached_seq2viewSym == null) {
	    transform_path = new SeqSymmetry[1];
	    transform_path[0] = csym;
	  }
	  else {
	    transform_path = new SeqSymmetry[2];
	    transform_path[0] = csym;
	    transform_path[1] = cached_seq2viewSym;
	  }
	  addAnnotationTiers();
	}
      }

      // restore aseq and seq2viewsym afterwards...
      aseq = cached_aseq;
      seq2viewSym = cached_seq2viewSym;
      transform_path = cached_path;
    }
  }

  /**
   *  Finds the "method" for a SeqSymmetry.
   *  Looks for the "method" in these places, in order:
   *   (1) the property "method", (2) the property "type",
   *   (3) TypedSym.getType().
   *  If no method is found, returns null.
   */
  public static String determineMethod(SeqSymmetry sym) {
    String meth = null;
    if (sym instanceof SymWithProps)  {
      SymWithProps psym = (SymWithProps)sym;
      meth = (String)psym.getProperty("method");
      if (meth == null) { meth = (String) psym.getProperty("type"); }
    }
    if (meth == null) {
      if (sym instanceof TypedSym) {
        meth = ((TypedSym)sym).getType();
      }
    }
    return meth;
  }

  public void addAnnotationGlyphs(SeqSymmetry annotSym) {
    // Map symmetry subclass or method type to a factory, and call factory to make glyphs
    MapViewGlyphFactoryI factory = null;
    String meth = null;
    if (annotSym instanceof GraphSym) {
      factory =	(MapViewGlyphFactoryI)graf2factory.get(annotSym);
      if (factory == null) {
	//	factory = new GenericGraphGlyphFactory(this);
	factory = new GenericGraphGlyphFactory(this.getSeqMap());
	graf2factory.put(annotSym, factory);
      }
    }
    else {
      meth = determineMethod(annotSym);
      if (meth != null) {
        factory = (MapViewGlyphFactoryI)meth2factory.get(meth);
        if (factory == null) {
          Vector keyset = new Vector(regex2factory.keySet());

          // Look for a matching pattern, going backwards, so that the
          // patterns from the last preferences read take precedence over the
          // first ones read (such as the default prefs).  Within a single
          // file, the last matching pattern will trump any earlier ones.
          for (int j=keyset.size()-1 ; j >= 0 && factory == null; j--) {
            java.util.regex.Pattern regex = (java.util.regex.Pattern) keyset.get(j);
            if (regex.matcher(meth).find()) {
              factory = (MapViewGlyphFactoryI) regex2factory.get(regex);
              // Put (a clone of?) the factory in meth2factory to speed things up next time through.
              // (A clone would let us later modify the color, etc. of that copy)
              meth2factory.put(meth, factory);
            }
          }

        }
        if (factory == null) {
          factory = default_glyph_factory;
          // Again, a clone might be better.
          meth2factory.put(meth, default_glyph_factory);
        }
      }
    }
    if (factory == null) { factory = default_glyph_factory; }

    // factory.createGlyph(annotSym, this);

    // do "middleground" shading for tracks loaded via DAS/2
    if ((meth != null) &&
	(annotSym instanceof TypeContainerAnnot) &&
	(annotSym.getChildCount() > 0)  &&
	(annotSym.getChild(0) instanceof Das2FeatureRequestSym) ) {
      int child_count = annotSym.getChildCount();
      TierGlyph fortier = (TierGlyph) getForwardTierHash().get(meth.toLowerCase());
      TierGlyph revtier = (TierGlyph) getReverseTierHash().get(meth.toLowerCase());
      for (int i=0; i<child_count; i++) {
	SeqSymmetry csym = annotSym.getChild(i);
	if (csym instanceof Das2FeatureRequestSym) {
	  Das2FeatureRequestSym dsym = (Das2FeatureRequestSym)csym;
	  SeqSpan ospan = dsym.getOverlapSpan();
	  // System.out.println("DAS FEATURE SYM: " + SeqUtils.spanToString(csym.getSpan(0)));
	  if (fortier != null) {
	    GlyphI mglyph = new EfficientFillRectGlyph();
	    //	    mglyph.setColor(Color.lightGray);  this is done in TierGlyph for now...
	    mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
	    fortier.addMiddleGlyph(mglyph);
	  }
	  if (revtier != null) {
	    GlyphI mglyph = new EfficientFillRectGlyph();
	    //	    mglyph.setColor(Color.lightGray);  this is done in TierGlyph for now...
	    mglyph.setCoords(ospan.getMin(), 0, ospan.getMax() - ospan.getMin(), 0);
	    revtier.addMiddleGlyph(mglyph);
	  }
	}
      }
    }

  }


  public AnnotatedBioSeq getAnnotatedSeq() {
    return aseq;
  }


  /**
   *  Gets the view seq.
   *  Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
   *  different BioSeq's !
   *  This allows for reverse complement, coord shifting, seq slicing, etc.
   *  Returns BioSeq that is the SeqMapView's _view_ onto the
   *     AnnotatedBioSeq returned by getAnnotatedSeq()
   *  @see #getTransformPath()
   */
  public BioSeq getViewSeq() {
    return viewseq;
  }

  /**
   *  Returns the series of transformations that can be used to map
   *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
   *  {@link #getViewSeq()}.
   */
  public SeqSymmetry[] getTransformPath() { return transform_path; }

  /** Returns a transformed copy of the given symmetry based on
   *  {@link #getTransformPath()}.  If no transform is necessary, simply
   *  returns the original symmetry.
   */
  public SeqSymmetry transformForViewSeq(SeqSymmetry insym) {
    SeqSymmetry result_sym = insym;
    if (getAnnotatedSeq() != getViewSeq()) {
      MutableSeqSymmetry tempsym = SeqUtils.copyToDerived(insym);
      SeqUtils.transformSymmetry(tempsym, getTransformPath());
      result_sym = tempsym;
    }
    return result_sym;
  }

  public AffyTieredMap getSeqMap() {
    return seqmap;
  }

  //  void select(java.util.List sym_list);
  //  void select(java.util.List sym_list, boolean add_to_previous,
  //		     boolean call_listeners, boolean update_widget) {
  //  void postSelections() {
  //  public void setSelectedRegion(SeqSymmetry region_sym, boolean update_widget) {
  //  public SeqSymmetry getSelectedRegion() {


  /**
   *  return a SeqSpan representing the visible bounds of the view seq
   */
  public SeqSpan getVisibleSpan() {
    Rectangle2D vbox = seqmap.getView().getCoordBox();
    SeqSpan vspan = new SimpleSeqSpan((int)vbox.x,
				      (int)(vbox.x+vbox.width),
				      viewseq);
    return vspan;
  }

  /** Toggles the hairline between labeled/unlabled and returns true
   *  if it ends-up labeled.
   */
  public boolean toggleHairlineLabel() {
    hairline_is_labeled = ! hairline_is_labeled;
    if (hairline != null) {
      Shadow s = hairline.getShadow();
      s.setLabeled(hairline_is_labeled);
      seqmap.updateWidget();
    }
    return hairline_is_labeled;
  }

  public boolean isHairlineLabeled() {
    return hairline_is_labeled;
  }


  /**
   *  Returns a forward and reverse tier for the given method, creating them if they don't
   *  already exist.
   *  Generally called by the Glyph Factory.
   *  Note that this can create empty tiers.  But if the tiers are not filled with
   *  something, they will later be removed automatically by {@link SeqMapView#setAnnotatedSeq(AnnotatedBioSeq)}.
   *  @param meth  The tier name; it will be treated as case-insensitive.
   *  @param next_to_axis Do you want the Tier as close to the axis as possible?
   *  @param style  a non-null instance of IAnnotStyle; tier label and other properties
   *   are determined by the IAnnotStyle.
   *  @return an array of two Tiers, one forward (or mixed-direction), one reverse;
   *    If you want to treat the first one as mixed-direction, then place all
   *    the glyphs in it; the second tier will not be displayed if it remains empty.
   */
  public TierGlyph[] getTiers(String meth, boolean next_to_axis, IAnnotStyle style) {
      if (style == null) {
        throw new NullPointerException();
      }

      // Always returns two tiers.  Could change to return only one tier if
      // that is what the style suggests.

      AffyTieredMap map = this.getSeqMap();

      // try to match up method with tier...
      // have meth2forward, meth2reverse hashtables to map
      //    method name to forward and reverse tier hashtables
      Map method2ftier = this.getForwardTierHash();
      Map method2rtier = this.getReverseTierHash();

      TierGlyph fortier = (TierGlyph)method2ftier.get(meth.toLowerCase());
      TierGlyph revtier = (TierGlyph)method2rtier.get(meth.toLowerCase());

      TierGlyph axis_tier = this.getAxisTier();
      if (fortier == null) {
        fortier = new TierGlyph(style);
        // setUpTierPacker(fortier, true);
        method2ftier.put(meth.toLowerCase(), fortier);
      }
      if (fortier != null) {
        String label;
        if (style instanceof AnnotStyle) {
          if (((AnnotStyle) style).getSeparate()) {
            //fortier.setDirection(TierGlyph.DIRECTION_FORWARD);
            label = style.getHumanName() + " (+)";
          } else {
            //fortier.setDirection(TierGlyph.DIRECTION_NONE);
            label = style.getHumanName() + " (+/-)";
          }
        } else { // may be an instance of graph annot style
          //fortier.setDirection(TierGlyph.DIRECTION_FORWARD);
          //label = meth + " (+)";
          label = meth;
        }
        fortier.setLabel(label);
      }
      if (map.getTierIndex(fortier) == -1) {
        if (next_to_axis)  {
          int axis_index = map.getTierIndex(axis_tier);
          map.addTier(fortier, axis_index);
        }
        else { map.addTier(fortier, true); }
      }

      if (revtier == null)  {
        revtier = new TierGlyph(style);
        //revtier.setDirection(TierGlyph.DIRECTION_REVERSE);
        // setUpTierPacker(revtier, false);
        method2rtier.put(meth.toLowerCase(), revtier);
      }
      if (revtier != null) {
        if (style instanceof AnnotStyle) {
          revtier.setLabel(style.getHumanName() + " (-)");
        } else { // style is a graph style or is null (this may not even be possible)
          revtier.setLabel(meth + " (-)");
        }
      }
      if (map.getTierIndex(revtier) == -1) {
        if (next_to_axis)  {
          int axis_index = map.getTierIndex(axis_tier);
          map.addTier(revtier, axis_index+1);
        }
        else { map.addTier(revtier, false); }
      }

      TierGlyph[] tiers = {fortier, revtier};
      return tiers;
  }


  public void groupSelectionChanged(GroupSelectionEvent evt)  {
    AnnotatedSeqGroup group = evt.getSelectedGroup();
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("SeqMapView received seqGroupSelected() call: " + group.getID() + ",  " + group);
    }
    else {
      if (IGB.DEBUG_EVENTS)  { System.out.println("SeqMapView received seqGroupSelected() call, but group = null"); }
    }

    if ((aseq != null) && (aseq instanceof SmartAnnotBioSeq) &&
        (((SmartAnnotBioSeq)aseq).getSeqGroup() == group) ) {
      // don't clear if seq belongs to seq group...
    }
    else  {
      clear();
    }
  }

  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("SeqMapView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    final AnnotatedBioSeq newseq = evt.getSelectedSeq();
    // Don't worry if newseq is null, setAnnotatedSeq can handle that
    // (It can also handle the case where newseq is same as old seq.)
    setAnnotatedSeq(newseq);
  }

  public void seqModified(SeqModifiedEvent evt) {
    SmartAnnotBioSeq modseq = evt.getModifiedSeq();
    if (modseq == this.getAnnotatedSeq()) {
      System.out.println("SeqMapView received a seqModified event, re-rendering via setAnnotatedSeq() call");
      setAnnotatedSeq(modseq, true, true);
    }
    else {
      System.out.println("ERROR: SeqMapView received a seqModified event for a sequence that is " +
			 "not the sequence it is currently viewing");
    }
  }

  /**
   *  SymSelectionListener interface
   */
  public void symSelectionChanged(SymSelectionEvent evt) {
    Object src = evt.getSource();
    String src_id = ObjectUtils.objString(src);
    if ((src instanceof AltSpliceView) || (src instanceof SeqMapView))  {
      // catching SeqMapView as source of event because currently sym selection events actually originating
      //    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
      if (IGB.DEBUG_EVENTS) {System.out.println("SeqMapView received selection event from another SeqMapView: " + src_id);}
    }
    else {
      if (IGB.DEBUG_EVENTS) {System.out.println("SeqMapView received selection event originating from: " + src_id);}
      java.util.List symlist = evt.getSelectedSyms();
      //      select(symlist, false, false, false);
      //      zoomToSelections();
    }
  }

}


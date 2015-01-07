package com.affymetrix.igb.view;

import aQute.bnd.annotation.component.Activate;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.AxisPopupListener;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.PropertyHandler;
import com.affymetrix.genometryImpl.event.PropertyHolder;
import com.affymetrix.genometryImpl.event.SeqMapRefreshed;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FloaterGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.AutoScroll;
import com.affymetrix.igb.glyph.GlyphEdgeMatcher;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPWidget;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.SeqMapViewPopup;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.load.AutoLoadThresholdHandler;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewExtendedI;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

/**
 *
 * @author dcnorris
 */
public interface SeqMapViewI extends GroupSelectionListener, PropertyHolder, SeqMapViewExtendedI, SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, JRPWidget {

    
    String PREF_EDGE_MATCH_COLOR = "Edge match color";
    String PREF_EDGE_MATCH_FUZZY_COLOR = "Edge match fuzzy color";
    /**
     * Name of a boolean preference for whether to show properties in tooltip.
     */
    String PREF_SHOW_TOOLTIP = "Show properties in tooltip";
    /**
     * Name of a string preference define which resize behavior to use.
     */
    String PREF_TRACK_RESIZING_BEHAVIOR = "Track resizing behavior";
    Font axisFont = NeoConstants.default_bold_font;
    boolean default_auto_change_view = false;
    //public static final Color default_edge_match_color = Color.RED;
    Color default_edge_match_color = new Color(204, 0, 255);
    Color default_edge_match_fuzzy_color = new Color(200, 200, 200); // light gray
    boolean default_show_prop_tooltip = true;

    @Activate
    void activate(boolean add_popups, String theId, JFrame frame);

    void addAxisPopupListener(AxisPopupListener listener);

    void addPopupListener(ContextualPopupListener listener);

    void addToPixelFloaterGlyph(GlyphI glyph);

    void addToRefreshList(SeqMapRefreshed smr);

    void adjustEdgeMatching(int bases);

    /**
     * Center at the hairline.
     */
    void centerAtHairline();

    boolean consecutiveOK();

    void dataRemoved();

    void deselectAll();

    Map<String, Object> determineProps(SeqSymmetry sym);

    void disableToolTip();

    /**
     * Do edge matching. If query_glyphs is empty, clear all edges.
     *
     * @param query_glyphs
     * @param update_map
     */
    void doEdgeMatching(List<GlyphI> query_glyphs, boolean update_map);

    List<GlyphI> doTheSelection(Rectangle2D.Double coordrect);

    List<GlyphI> getAllSelectedTiers();

    BioSeq getAnnotatedSeq();

    AutoLoadThresholdHandler getAutoLoadAction();

    AutoScroll getAutoScroll();

    TierGlyph getAxisTier();

    GlyphEdgeMatcher getEdgeMatcher();

    FloaterGlyph getFloaterGlyph();

    List<GraphGlyph> getFloatingGraphGlyphs();

    String getId();

    @SuppressWarnings(value = "unchecked")
    <G extends GlyphI> G getItemFromTier(Object datamodel);

    MapRangeBox getMapRangeBox();

    SeqMapViewMouseListener getMouseListener();

    JRPButton getPartial_residuesButton();

    double getPixelsToCoord(double smin, double smax);

    SeqMapViewPopup getPopup();

    @SuppressWarnings(value = "unchecked")
    List<Map<String, Object>> getProperties();

    GenericAction getRefreshDataAction();

    List<GraphGlyph> getSelectedFloatingGraphGlyphs();

    /**
     * Determines which SeqSymmetry's are selected by looking at which Glyph's
     * are currently selected. The list will not include the selected sequence
     * region, if any. Use getSelectedRegion() for that.
     *
     * @return a List of SeqSymmetry objects, possibly empty.
     */
    List<SeqSymmetry> getSelectedSyms();

    AffyTieredMap getSeqMap();

    SeqSymmetry getSeqSymmetry();

    boolean getShowPropertiesTooltip();

    boolean getShrinkWrap();

    TierLabelManager getTierManager();

    /**
     * Returns a tier for the given style and direction, creating them if they
     * don't already exist. Generally called by the Glyph Factory. Note that
     * this can create empty tiers. But if the tiers are not filled with
     * something, they will later be removed automatically.
     *
     * @param sym The SeqSymmetry (data model) for the track
     * @param style a non-null instance of IAnnotStyle; tier label and other
     * properties are determined by the IAnnotStyle.
     * @param tier_direction the direction of the track (FORWARD, REVERSE, or
     * BOTH)
     */
    TierGlyph getTrack(ITrackStyleExtended style, StyledGlyph.Direction tier_direction);

    /**
     * Returns the series of transformations that can be used to map a
     * SeqSymmetry from {@link #getAnnotatedSeq()} to {@link #getViewSeq()}.
     */
    SeqSymmetry[] getTransformPath();

    /**
     * Gets the view seq. Note: {@link #getViewSeq()} and
     * {@link #getAnnotatedSeq()} may return different BioSeq's ! This allows
     * for reverse complement, coord shifting, seq slicing, etc. Returns BioSeq
     * that is the SeqMapView's _view_ onto the BioSeq returned by
     * getAnnotatedSeq()
     *
     * @see #getTransformPath()
     */
    BioSeq getViewSeq();

    /**
     * Get the span of the symmetry that is on the seq being viewed.
     */
    SeqSpan getViewSeqSpan(SeqSymmetry sym);

    /**
     * return a SeqSpan representing the visible bounds of the view seq
     */
    SeqSpan getVisibleSpan();

    void groupSelectionChanged(GroupSelectionEvent evt);

    void horizontalClamp(boolean clamp);

    boolean isGenomeSequenceSupported();

    boolean isPopupActive();

    void loadSession();

    /**
     * Figures out which symmetries are currently selected and then calls
     * {@link GenometryModel#setSelectedSymmetries(List, List, Object)}.
     */
    @SuppressWarnings(value = "unchecked")
    void postSelections();

    void preserveSelectionAndPerformAction(AbstractAction action);

    void redoEdgeMatching();

    void removeAxisPopupListener(AxisPopupListener listener);

    void removePopupListener(ContextualPopupListener listener);

    GlyphI removeSym(SeqSymmetry sym);

    void repackTheTiers(boolean full_repack, boolean stretch_vertically);

    void saveSession();

    List<GlyphI> searchForRegexInResidues(boolean forward, Pattern regex, String residues, int residue_offset, Color hitColor);

    void select(List<SeqSymmetry> sym_list, boolean normal_selection);

    void select(GlyphI glyph);

    void selectAll(FileTypeCategory... category);

    //	@Override
    //	public void setDataModelFromOriginalSym(GlyphI g, SeqSymmetry sym) {
    //		seqmap.setDataModelFromOriginalSym(g, sym);
    //	}
    void selectAllGraphs();

    /**
     * Select the parents of the current selections
     */
    void selectParents();

    void selectTrack(TierGlyph tier, boolean selected);

    void seqMapRefresh();

    void seqSelectionChanged(SeqSelectionEvent evt);

    /**
     * Sets the sequence; if null, has the same effect as calling clear().
     */
    void setAnnotatedSeq(BioSeq seq);

    /**
     * Sets the sequence. If null, has the same effect as calling clear().
     *
     * @param preserve_selection if true, then try and keep same selections
     * @param preserve_view if true, then try and keep same scroll and zoom /
     * scale and offset in // both x and y direction. [GAH: temporarily changed
     * to preserve scale in only the x direction]
     */
    void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view);

    //   want to optimize for several situations:
    //       a) merging newly loaded data with existing data (adding more annotations to
    //           existing BioSeq) -- would like to avoid recreation and repacking
    //           of already glyphified annotations
    //       b) reverse complementing existing BioSeq
    //       c) coord shifting existing BioSeq
    //   in all these cases:
    //       "new" BioSeq == old BioSeq
    //       existing glyphs could be reused (in (b) they'd have to be "flipped")
    //       should preserve selection
    //       should preserve view (x/y scale/offset) (in (b) would preserve "flipped" view)
    //   only some of the above optimization/preservation are implemented yet
    //   WARNING: currently graphs are not properly displayed when reverse complementing,
    //               need to "genometrize" them
    //            currently sequence is not properly displayed when reverse complementing
    //
    void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y);

    void setBackGroundProvider(ViewI.BackGroundProvider bgp, ViewI.BackGroundProvider labelbgp);

    void setPropertyHandler(PropertyHandler propertyHandler);

    void setRegion(int start, int end, BioSeq seq);

    // assumes that region_sym contains a span with span.getBioSeq() ==  current seq (aseq)
    void setSelectedRegion(SeqSymmetry region_sym, GlyphI seq_glyph, boolean update_widget);

    void setShrinkWrap(boolean b);

    /**
     * Sets tool tip from given glyphs.
     *
     * @param glyphs
     */
    void setToolTip(MouseEvent evt, GlyphI glyph);

    /**
     * Sets tool tip from graph glyph.
     *
     * @param glyph
     */
    void setToolTip(MouseEvent evt, int x, GraphGlyph glyph);

    /**
     * Sets the hairline position and zoom center to the given spot. Does not
     * call map.updateWidget()
     */
    void setZoomSpotX(double x);

    /**
     * Sets the hairline position to the given spot. Does not call
     * map.updateWidget()
     */
    void setZoomSpotY(double y);

    boolean shouldAddCytobandGlyph();

    void showProperties(int x, GraphGlyph glyph);

    void split(GlyphI glyph);

    /**
     * SymSelectionListener interface
     */
    void symSelectionChanged(SymSelectionEvent evt);

    void toggleHorizontalClamp();

    void trackstylePropertyChanged(EventObject eo);

    /**
     * Returns a transformed copy of the given symmetry based on
     * {@link #getTransformPath()}. If no transform is necessary, simply returns
     * the original symmetry.
     */
    SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare);

    SeqSymmetry transformForViewSeq(SeqSymmetry insym, MutableSeqSymmetry result, BioSeq seq_to_compare);

    void updateCdsEnd(int end, SeqSymmetry sym, boolean select);

    void updateCdsStart(int start, SeqSymmetry sym, boolean select);

    void updateEnd(int end, SeqSymmetry sym);

    /**
     * Update the widget in this panel. Putting this awkward idiom here to try
     * to contain its spread. It is here for backward compatability.
     */
    void updatePanel(boolean preserveViewX, boolean preserveViewY);

    /**
     * Update the widget in this panel.
     */
    void updatePanel();

    void updateStart(int start, SeqSymmetry sym);

    void zoomTo(SeqSpan span);

    void zoomTo(double smin, double smax);

    void zoomToGlyphs(List<GlyphI> glyphs);

    /**
     * Zoom to a region including all the currently selected Glyphs.
     */
    void zoomToSelections();

}

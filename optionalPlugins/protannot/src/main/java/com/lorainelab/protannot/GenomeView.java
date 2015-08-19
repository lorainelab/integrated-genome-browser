package com.lorainelab.protannot;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.TieredNeoMap;
import com.affymetrix.genoviz.widget.VisibleRange;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import com.affymetrix.igb.swing.JRPTabbedPane;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.lorainelab.protannot.ProtAnnotPreferencesService.Panel;
import com.lorainelab.protannot.event.PreferenceChangeEvent;
import com.lorainelab.protannot.event.ZoomInEvent;
import com.lorainelab.protannot.event.ZoomOutEvent;
import com.lorainelab.protannot.model.InterProScanTableModel;
import com.lorainelab.protannot.model.ProtannotParser;
import com.lorainelab.protannot.view.LineContainerDashGlyph;
import com.lorainelab.protannot.view.LineContainerProtAnnotGlyph;
import com.lorainelab.protannot.view.ProtAnnotMapTierGlyph;
import com.lorainelab.protannot.view.TabPanelComponent;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class displays the main view of transcripts, the conserved motifs (protein annotations) they encode, and an exon
 * summary that shows how the transcript structures vary.
 */
@Component(provide = GenomeView.class, factory = "genome.view.factory.provider")
public class GenomeView extends JPanel implements MouseListener, ComponentListener {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GenomeView.class);

    JPopupMenu popup;

    private static final boolean DEBUG_GENOMIC_ANNOTS = false;
    private static final boolean DEBUG_TRANSCRIPT_ANNOTS = false;
    private static final boolean DEBUG_PROTEIN_ANNOTS = false;
    // the map that displays the transcripts and protein annotations
    private TieredNeoMap seqmap;
    // the map that shows the sequence and axis
    private NeoMap axismap;
    private NeoMap[] maps;
    private ModPropertySheet table_view;
    private AdjustableJSlider xzoomer;
    private AdjustableJSlider yzoomer;
    private BioSeq gseq;
    private BioSeq vseq;
    private List<GlyphI> exonGlyphs = null;
    private List<SeqSymmetry> exonList = new ArrayList<>();
    private Map<String, Color> prefs_hash;
    private boolean showhairline = true;
    private boolean showhairlineLabel = true;
    private Shadow hairline, axishairline;
    private JSplitPane split_pane;

    private final Color col_sequence = Color.black;
    private final Color col_axis_bg = Color.lightGray;

    private List<GlyphI> selected = new ArrayList<>();
    private List<GlyphI> storeSelected;
    private VisibleRange zoomPoint = new VisibleRange();

    // size constants - these are needed to control the layout
    // of different elements within each map.
    private static final int axis_pixel_height = 20;
    private static final int seq_pixel_height = 10;
    private static final int upper_white_space = 5;
    private static final int middle_white_space = 2;
    private static final int lower_white_space = 2;
    private static final int divider_size = 8;
    private static final int table_height = 100;
    private static final int seqmap_pixel_height = 500;
    private static final double zoomRatio = 30.0;
    private static final int X_OFFSET_POPUP = 45;
    private static final int Y_OFFSET_POPUP = 65;
    private JRPTabbedPane tabbedPane;

    private ComponentFactory propertiesTabPanelFactory;

    private ComponentFactory interProScanTabPanelFactory;
    private Map<String, Object> properties;

    private InterProScanResultSheet ipsTable;
    private InterProScanTableModel ipsTableModel;
    private ProtAnnotEventService eventService;

    private ProtAnnotPreferencesService protAnnotPreferencesService;

    @Reference
    public void setProtAnnotPreferencesService(ProtAnnotPreferencesService protAnnotPreferencesService) {
        this.protAnnotPreferencesService = protAnnotPreferencesService;
    }

    @Reference
    public void setEventService(ProtAnnotEventService eventService) {
        this.eventService = eventService;
    }

    public void setIpsTableModel(InterProScanTableModel ipsTableModel) {
        this.ipsTableModel = ipsTableModel;
    }

    @Deactivate
    public void deactivate() {
        protAnnotPreferencesService.unregisterEventListener(this);
    }
    
    @Subscribe
    public void preferenceChangeEventListener(PreferenceChangeEvent event) {
        updatePreferences(protAnnotPreferencesService.getAllColorPreferences());
    }
    
    @Activate
    public void activate(Map<String, Object> properties) {
        this.properties = properties;
        protAnnotPreferencesService.registerEventListener(this);
        initPrefs(loadPrefs());
        popup = new JPopupMenu();
        seqmap = new TieredNeoMap(true, false);
        seqmap.enableDragScrolling(true);
        seqmap.setReshapeBehavior(NeoAbstractWidget.X, NeoAbstractWidget.FITWIDGET);
        seqmap.setReshapeBehavior(NeoAbstractWidget.Y, NeoAbstractWidget.FITWIDGET);
        seqmap.setMapOffset(0, seqmap_pixel_height);
        axismap = new NeoMap(false, false);
        axismap.setMapColor(col_axis_bg);
        axismap.setMapOffset(0, axis_pixel_height + seq_pixel_height
                + upper_white_space + middle_white_space
                + lower_white_space);

        xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
        xzoomer.setBackground(Color.white);
        yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
        yzoomer.setBackground(Color.white);

        seqmap.setZoomer(NeoMap.X, xzoomer);
        seqmap.setZoomer(NeoMap.Y, yzoomer);

        axismap.setZoomer(NeoMap.X, seqmap.getZoomer(TieredNeoMap.X));

        seqmap.getScroller(NeoMap.X).addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                axismap.getScroller(NeoMap.X).setValue(seqmap.getScroller(NeoMap.X).getValue());
            }
        });

        seqmap.getZoomer(NeoMap.X).addAdjustmentListener(new AdjustmentListener() {

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                axismap.getScroller(NeoMap.X).setValue(seqmap.getScroller(NeoMap.X).getValue());
            }

        });

        this.setLayout(new BorderLayout());

        JPanel p = initPanel();
        initPropertiesTab();
        initInterProScanTab();
        initListerners();

        EventBus eventBus = eventService.getEventBus();
        eventBus.register(this);
    }

    @Subscribe
    public synchronized void updateInterProScanTableModel(InterProScanModelUpdateEvent e) {
        ipsTable.showTableData(ipsTableModel);
        ipsTableModel.fireTableDataChanged();
    }

    public void initAxis() {
        axismap.setSize(seqmap.getSize().width, upper_white_space
                + middle_white_space + lower_white_space
                + axis_pixel_height + seq_pixel_height);
        maps = new NeoMap[2];
        maps[0] = seqmap;
        maps[1] = axismap;

        zoomPoint = new VisibleRange();
        hairline = new Shadow(this.seqmap);
        axishairline = new Shadow(this.axismap);
        zoomPoint.addListener(hairline);
        zoomPoint.removeListener(axishairline);

        hairline.setUseXOR(true);
        hairline.setLabeled(showhairlineLabel);
    }

    public void initSplitPane(JPanel p) {
        split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, p, tabbedPane);
        this.add(split_pane);
    }

    public JPanel initPanel() {
        JScrollBar y_scroller = new JScrollBar(JScrollBar.VERTICAL);
        seqmap.setOffsetScroller(y_scroller);
        JPanel mapPanel = new JPanel();

        mapPanel.setLayout(new BorderLayout());
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.addComponentListener(this);
        top.add("South", axismap);
        mapPanel.add("North", top);
        seqmap.setPreferredSize(new Dimension(100, seqmap_pixel_height));
        seqmap.setBackground(new Color(protAnnotPreferencesService.getPanelRGB(Panel.BACKGROUND)));
        mapPanel.add("Center", seqmap);
        JPanel right = new JPanel();
        right.setLayout(new GridLayout(1, 2));
//        right.add(y_scroller);
        right.add(getYZoomPanel());
        int maps_height = axis_pixel_height + seq_pixel_height
                + upper_white_space + middle_white_space + lower_white_space
                + divider_size + seqmap_pixel_height;

        JPanel p = new JPanel();
        p.addComponentListener(this);
        p.setPreferredSize(new Dimension(seqmap.getWidth(), maps_height));
        p.setLayout(new BorderLayout());
        p.add("East", y_scroller);
        p.add("Center", mapPanel);
        p.add("North", getXZoomPanel());
        p.add("West", right);

        tabbedPane = new JRPTabbedPane(GenomeView.class.getName());
        Dimension minimumSize = new Dimension(0, 500);
        p.setMinimumSize(minimumSize);

        initSplitPane(p);

        initAxis();
        return p;
    }

    private JPanel getXZoomPanel() {
        JPanel xZoomPanelWrapper = new JPanel(new MigLayout("fillx, ins 0"));
        JPanel xZoomPanel = new JPanel(new MigLayout("fillx, ins 2"));
        JButton xZoomOutBtn = new JButton(new ZoomOutEvent(xzoomer));
        JButton xZoomInBtn = new JButton(new ZoomInEvent(xzoomer));
        xZoomPanel.add(xZoomOutBtn, "width 20!, height 20!");
        xZoomPanel.add(xzoomer, "width 96%, height 20!");
        xZoomPanel.add(xZoomInBtn, "width 20!, height 20!");
        xZoomPanelWrapper.add(xZoomPanel, "width 60%, center");
        return xZoomPanelWrapper;
    }

    private JPanel getYZoomPanel() {
        JPanel yZoomPanelWrapper = new JPanel(new MigLayout("filly, ins 0"));
        JPanel yZoomPanel = new JPanel(new MigLayout("filly, ins 2"));
        JButton yZoomOutBtn = new JButton(new ZoomOutEvent(yzoomer));
        JButton xZoomInBtn = new JButton(new ZoomInEvent(yzoomer));
        yZoomPanel.add(yZoomOutBtn, new CC().width("20!").height("20!").alignY("top").alignX("center").wrap());
        yZoomPanel.add(yzoomer, new CC().grow().pushY().wrap().alignX("center"));
        yZoomPanel.add(xZoomInBtn, new CC().width("20!").height("20!").alignY("bottom").alignX("center"));
        yZoomPanelWrapper.add(yZoomPanel, "growy");
        return yZoomPanelWrapper;
    }

    public void initListerners() {
        seqmap.addMouseListener(this);
        seqmap.setSelectionEvent(TieredNeoMap.NO_SELECTION);

        seqmap.setSelectionAppearance(Scene.SELECT_OUTLINE);
        axismap.addMouseListener(this);
        axismap.setSelectionEvent(NeoMap.NO_SELECTION);
    }

    public void initInterProScanTab() {
        final Properties ipsProps = new Properties();
        ipsProps.put("id", properties.get("id"));
        ipsProps.put("protannotService", properties.get("protannotService"));
        TabPanelComponent ipsTab = (TabPanelComponent) interProScanTabPanelFactory.newInstance(ipsProps).getInstance();
        ipsTable = (InterProScanResultSheet) ipsTab.getComponent();
        ipsTableModel = new InterProScanTableModel();
        ipsTableModel.setEventService(eventService);
        ipsTable.showTableData(ipsTableModel);
        tabbedPane.add(ipsTable.getTitle(), ipsTable);
    }

    public void initPropertiesTab() {
        final Properties props = new Properties();
        props.setProperty("seqmap.width", seqmap.getWidth() + "");
        props.setProperty("table.height", table_height + "");
        ComponentInstance instance = propertiesTabPanelFactory.newInstance(props);
        TabPanelComponent tb = (TabPanelComponent) instance.getInstance();
        table_view = (ModPropertySheet) tb.getComponent();
        tabbedPane.add(tb.getName(), table_view);
    }

    @Reference(target = "(component.factory=properties.tab.factory.provider)")
    public void setPropertiesTabPanelFactory(final ComponentFactory propertiesTabPanelFactory) {
        this.propertiesTabPanelFactory = propertiesTabPanelFactory;
    }

    @Reference(target = "(component.factory=interproscan.tab.factory.provider)")
    public void setInterProScanTabPanelFactory(final ComponentFactory interProScanTabPanelFactory) {
        this.interProScanTabPanelFactory = interProScanTabPanelFactory;
    }

    /**
     * Removes currently loaded data by clearing maps.
     */
    void no_data() {
        seqmap.clearWidget();
        axismap.clearWidget();
        seqmap.updateWidget();
        axismap.updateWidget();
//        table_view.showProperties(new Properties[0]);
    }

    private Map<String, Color> loadPrefs() {
        return protAnnotPreferencesService.getAllColorPreferences();
    }

    /**
     * Sets up the layout for the maps and the other elements that are part of the application.
     *
     * @param phash Color perferences stored in hashtable to setup the layout.
     */
    public GenomeView() {

    }

    /**
     * Initialized GenomeView colors with preferences provided in the parameter phash
     *
     * @param phash Map providing color preferences for GenomeView
     */
    private void initPrefs(Map<String, Color> phash) {
        prefs_hash = phash;
    }


    /**
     * Add mouse listener to maps so that the application can detect user interactions with the display.
     *
     * @param listener Listener that is to be added to maps.
     */
    public void addMapListener(MouseListener listener) {
        seqmap.addMouseListener(listener);
        axismap.addMouseListener(listener);
    }

    void clearGenomeView() {
        seqmap.clearWidget();
        axismap.clearWidget();
    }

    /**
     * Set the data model - the BioSeq object - that the application will display.
     *
     * @param gseq the data model representing the transcripts, their annotations, and their meta-data properties, such
     * as their ids in external databases.
     * @param is_new whether or not this BioSeq object has not been displayed previously. This allows ProtAnnot to
     * redraw the Glyphs using a new color scheme without changing the zoom level.
     * @see com.affymetrix.genometryImpl.BioSeq
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genometryImpl.util.SeqUtils
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genoviz.widget.Shadow
     * @see com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker
     * @see com.affymetrix.genoviz.widget.tieredmap.ProtAnnotMapTierGlyph
     */
    synchronized void setBioSeq(BioSeq gseq, boolean is_new) {
        this.gseq = gseq;
        seqmap.clearWidget();
        seqmap.setMapRange(gseq.getMin(), gseq.getMax());
        axismap.clearWidget();
        axismap.setMapRange(gseq.getMin(), gseq.getMax());
        seqmap.setBackground(new Color(protAnnotPreferencesService.getPanelRGB(Panel.BACKGROUND)));
        seqmap.setMaxZoom(NeoMap.Y, seqmap.getHeight() / zoomRatio);

        exonGlyphs = new ArrayList<>();
        exonList = new ArrayList<>();

        hairline.reset(this.seqmap, NeoConstants.HORIZONTAL, Color.lightGray);
        axishairline.reset(this.axismap, NeoConstants.HORIZONTAL, Color.lightGray);
        hairline.setLabeled(showhairlineLabel);

        int acount = gseq.getAnnotationCount();

        SeqSymmetry[] path2view = new SeqSymmetry[1];
        MutableSeqSymmetry viewSym = new SimpleMutableSeqSymmetry();
        viewSym.addSpan(new SimpleSeqSpan(gseq.getMin(), gseq.getMax(), gseq));
        vseq = new BioSeq("view seq", gseq.getLength());
        vseq.setBounds(gseq.getMin(), gseq.getMax());

        viewSym.addSpan(new SimpleSeqSpan(vseq.getMin(), vseq.getMax(), vseq));

        path2view[0] = viewSym;

        for (int i = 0; i < acount; i++) {
            SeqSymmetry asym = gseq.getAnnotation(i);
            if (DEBUG_GENOMIC_ANNOTS) {
                SeqUtils.printSymmetry(asym);
            }
            glyphifyMRNA(asym, path2view);
        }

        ProtAnnotMapTierGlyph sumTier = new ProtAnnotMapTierGlyph();
        sumTier.setCoords(gseq.getMin(), seqmap_pixel_height - 20, gseq.getLength(), 20);
        sumTier.setState(ProtAnnotMapTierGlyph.EXPANDED);

        ExpandedTierPacker epack = (ExpandedTierPacker) sumTier.getExpandedPacker();
        epack.setMoveType(ExpandedTierPacker.DOWN);
        GlyphSummarizer summer = new GlyphSummarizer(new Color(protAnnotPreferencesService.getPanelRGB(Panel.EXONSUMMARY)));
        if (exonGlyphs.size() > 0) {
            GlyphI gl = summer.getSummaryGlyph(exonGlyphs);
            sumTier.addChild(gl);
        }
        seqmap.addTier(sumTier);
        seqmap.repack();

//        table_view.showProperties(new Properties[0]);
        setupAxisMap();

        axismap.stretchToFit(is_new, false);
        seqmap.stretchToFit(is_new, true);

        seqmap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD,
                ((gseq.getMin() + gseq.getMax()) / 2.0));
        axismap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD,
                ((gseq.getMin() + gseq.getMax()) / 2.0));

        updateWidget();
    }

    /**
     * Toggle hairline on/off.
     *
     * @return
     */
    public boolean toggleHairline() {
        showhairline = !showhairline;

        hairline.setShowHairline(showhairline);
        axishairline.setShowHairline(showhairline);

        updateWidget();

        return showhairline;
    }

    /**
     * Toggle hairline label on/off.
     *
     * @return
     */
    public boolean toggleHairlineLabel() {
        showhairlineLabel = !showhairlineLabel;

        hairline.setLabeled(showhairlineLabel);

        updateWidget();

        return showhairlineLabel;
    }

    public void stretchToFit(boolean x, boolean y) {
        seqmap.stretchToFit(x, y);
        axismap.stretchToFit(x, y);
    }

    public void updateWidget() {
        seqmap.updateWidget();
        axismap.updateWidget();
    }

    /**
     * Sets the title of the frame provided by the parameter title.
     *
     * @param title	Title of the frame. Usually the name of the file on display.
     */
    void setTitle(String title) {
        table_view.setTitle(title);
    }

    /**
     * Make a Glyph to represent the given mRNA object.
     *
     * @param mrna2genome	a data model representing an mRNA, a set of exons
     * @param path2view	"view seq" symmetry enclosed in an array
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genometryImpl.BioSeq
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.SeqSpan
     * @see com.affymetrix.genometryImpl.util.SeqUtils
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genoviz.widget.tieredmap.ProtAnnotMapTierGlyph
     */
    private void glyphifyMRNA(SeqSymmetry mrna2genome, SeqSymmetry[] path2view) {
        int childcount = mrna2genome.getChildCount();
        ProtAnnotMapTierGlyph tier = new ProtAnnotMapTierGlyph();
        tier.setCoords(gseq.getMin(), 80, gseq.getLength(), 120);
        tier.setState(ProtAnnotMapTierGlyph.EXPANDED);
        tier.setLabel((String) ((TypeContainerAnnot) mrna2genome).getProperty("protein_product_id"));
        ExpandedTierPacker epack = (ExpandedTierPacker) tier.getExpandedPacker();
        epack.setMoveType(ExpandedTierPacker.DOWN);
        seqmap.addTier(tier);
        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(mrna2genome, annot2genome);

        boolean result = SeqUtils.transformSymmetry(annot2genome, path2view);
        logger.debug(Boolean.toString(result));
        GlyphI tGlyph = glyphifyExons(mrna2genome, annot2genome, childcount);
        tier.addChild(tGlyph);
        BioSeq mrna = SeqUtils.getOtherSpan(mrna2genome, mrna2genome.getSpan(gseq)).getBioSeq();
        displayAssociatedmRNAforTranscript(mrna, path2view, mrna2genome, tier, tGlyph);
    }

    /**
     * Create glyphs for exons.
     *
     * @param mrna2genome
     * @param annot2genome
     * @param childcount - exon count
     * @return
     */
    private GlyphI glyphifyExons(
            SeqSymmetry mrna2genome, MutableSeqSymmetry annot2genome, int childcount) {
        GlyphI tGlyph = new LineContainerDashGlyph();
        seqmap.setDataModel(tGlyph, mrna2genome);
        SeqSpan tSpan = annot2genome.getSpan(vseq);
        tGlyph.setCoords(tSpan.getMin(), 0, tSpan.getLength(), 20);
        tGlyph.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.TRANSCRIPT)));
        for (int i = 0; i < childcount; i++) {
            SeqSymmetry exon2genome = annot2genome.getChild(i);
            SeqSpan gSpan = exon2genome.getSpan(vseq);
            GlyphI cglyph = new FillRectGlyph();
            if (i == childcount - 1) {
                cglyph = new PointedGlyph();
            }
            seqmap.setDataModel(cglyph, exon2genome);
            // can't give this a type and therefore signal
            // to the selection logic that this is first class selectable
            // object
            // so let's put it in a list
            exonList.add(exon2genome);

            cglyph.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.TRANSCRIPT)));
            cglyph.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 10);

            exonGlyphs.add(cglyph);
            tGlyph.addChild(cglyph);
            //  testing display of "exon segments" for transcripts that have
            //     base inserts relative to the genomic sequence
            //  haven't dealt with display of base deletions in transcript relative to genomic yet
            //  if exon is segmented by inserts, then it will have children
            //     that specify this segmentation
            for (int seg_index = 0; seg_index < exon2genome.getChildCount(); seg_index++) {
                SeqSymmetry eseg2genome = exon2genome.getChild(seg_index);
                SeqSpan seg_gspan = eseg2genome.getSpan(vseq);
                if (seg_gspan.getLength() == 0) {
                    // only mark the inserts (those whose genomic extent is zero
                    GlyphI segGlyph = new OutlineRectGlyph();
                    segGlyph.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.BACKGROUND)));
                    segGlyph.setCoords(seg_gspan.getMin(), 0, seg_gspan.getLength(), 25);
                    tGlyph.addChild(segGlyph);
                }
            }
        }
        return tGlyph;
    }

    // now follow symmetry link to annotated mrna seqs, map those annotations to genomic
    //    coords, and display
    private void displayAssociatedmRNAforTranscript(
            BioSeq mrna, SeqSymmetry[] path2view, SeqSymmetry mrna2genome, ProtAnnotMapTierGlyph tier, GlyphI tGlyph) {
        if (mrna != null) {
            if (DEBUG_TRANSCRIPT_ANNOTS) {
                LOG.info(mrna.getId() + ",  " + mrna);
            }
            SeqSymmetry[] new_path2view = new SeqSymmetry[path2view.length + 1];
            System.arraycopy(path2view, 0, new_path2view, 1, path2view.length);
            new_path2view[0] = mrna2genome;
            int acount = mrna.getAnnotationCount();
            for (int i = 0; i < acount; i++) {
                SeqSymmetry annot2mrna = mrna.getAnnotation(i);
                if (annot2mrna != mrna2genome) {
                    glyphifyTranscriptAnnots(mrna, annot2mrna, mrna2genome, new_path2view, tier, tGlyph);
                }
            }
        }
    }

    // now follow symmetry link to annotated mrna seqs, map those annotations to genomic
    //    coords, and display
    // consider merging with displayAssociatedmRNAforTranscript
    private void displayAssociatedmRNAforProtein(
            BioSeq protein, SeqSymmetry[] path2view, SeqSymmetry annot2mrna, ProtAnnotMapTierGlyph tier) {
        if (protein != null) {
            if (DEBUG_PROTEIN_ANNOTS) {
                LOG.info(protein.getId() + ",  " + protein);
            }
            //   new path info is added to _beginning_ of path
            SeqSymmetry[] new_path2view = new SeqSymmetry[path2view.length + 1];
            System.arraycopy(path2view, 0, new_path2view, 1, path2view.length);
            new_path2view[0] = annot2mrna;
            int acount = protein.getAnnotationCount();
            for (int i = 0; i < acount; i++) {
                SeqSymmetry annot2protein = protein.getAnnotation(i);
                if (annot2protein != annot2mrna) {
                    glyphifyProteinAnnots(annot2protein, new_path2view, tier);
                }
            }
        }
    }

    /**
     * Create glyphs that represent each transcript.
     *
     * @param mrna
     * @param annot2mrna
     * @param path2view - "view seq" symmetry enclosed in an array appended with mrna2genome
     * @param tier - tier where glyphs will be added
     * @param trans_parent - parent glyph
     * @see com.affymetrix.genometryImpl.BioSeq
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genoviz.widget.tieredmap.ProtAnnotMapTierGlyph
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.SeqSpan
     * @see com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void glyphifyTranscriptAnnots(BioSeq mrna,
            SeqSymmetry annot2mrna, SeqSymmetry mrna2genome, SeqSymmetry[] path2view,
            ProtAnnotMapTierGlyph tier, GlyphI trans_parent) {
        if (DEBUG_TRANSCRIPT_ANNOTS) {
            SeqUtils.printSymmetry(annot2mrna);
        }
        SeqSpan mrna_span = annot2mrna.getSpan(mrna);
        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(annot2mrna, annot2genome);

        SeqUtils.transformSymmetry(annot2genome, path2view);
        if (DEBUG_TRANSCRIPT_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }

        SeqSpan pSpan = SeqUtils.getOtherSpan(annot2mrna, mrna_span);
        BioSeq protein = pSpan.getBioSeq();
        String amino_acid = null;

        try {
            amino_acid = protein.getResidues(0, protein.getLength());
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        GlyphI aGlyph = new LineContainerProtAnnotGlyph();
        SeqSpan aSpan = annot2genome.getSpan(vseq);
        aGlyph.setCoords(aSpan.getMin(), 0, aSpan.getLength(), 20);
        aGlyph.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.TRANSCRIPT)));
        seqmap.setDataModel(aGlyph, annot2mrna);
        glyphifyCDSs(annot2genome, mrna2genome, protein, aGlyph, amino_acid, vseq, trans_parent);
        trans_parent.addChild(aGlyph);
        displayAssociatedmRNAforProtein(protein, path2view, annot2mrna, tier);
    }

    /**
     * Add glyphs for CDS regions.
     *
     * @param annot2genome
     * @param protein
     * @param aGlyph parent glyph
     * @param amino_acid String representing amino acids; visible when zoomed in
     * @param vseq
     */
    private void glyphifyCDSs(
            MutableSeqSymmetry annot2genome, SeqSymmetry mrna2genome, BioSeq protein, GlyphI aGlyph, String amino_acid, BioSeq vseq, GlyphI trans_parent) {

        int cdsCount = annot2genome.getChildCount();
        int prev_amino_end = 0;
        int prev_add = 0;
        for (int j = 0; j < cdsCount; j++) {
            SeqSymmetry cds2genome = annot2genome.getChild(j);
            SeqSpan gSpan = cds2genome.getSpan(vseq);
            GlyphI cglyph;
            if (isLastGlyph(j, cdsCount, gSpan, aGlyph)) {
                cglyph = new PointedGlyph();
            } else {
                cglyph = new FillRectGlyph();
            }
            //SeqSpan protSpan = cds2genome.getSpan(protein);
            // coloring based on frame
            colorByFrame(cglyph, gSpan.getMin() + prev_add);
            prev_add += 3 - (gSpan.getLength() % 3);	//Keep a track of no of complete amino acid added.
            cglyph.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 20);
            cglyph.setSelectable(true);
            aGlyph.addChild(cglyph);
            if (amino_acid != null) {

                try {
                    SequenceGlyph sg = new ColoredResiduesGlyph(protAnnotPreferencesService, false);
                    if (isLastGlyph(j, cdsCount, gSpan, trans_parent)) {
                        Field fullRect = SequenceGlyph.class.getDeclaredField("full_rect");
                        fullRect.setAccessible(true);
                        fullRect.set(sg, new PointedFillRectGlyph());
                    }
                    int start = prev_amino_end;
                    int end = start + gSpan.getLength();
                    String sub_amino_acid = amino_acid.substring(start, end);
                    prev_amino_end += gSpan.getLength();
                    sg.setResidues(sub_amino_acid);
                    sg.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 20);
                    sg.setForegroundColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.AMINOACID)));
                    sg.setBackgroundColor(cglyph.getBackgroundColor());
                    sg.setSelectable(true);
                    sg.setInfo(mrna2genome);
                    aGlyph.addChild(sg);
                } catch (Exception ex) {
                    logger.debug("Reflection hack failed", ex);
                }
            }
        }
    }

    private static boolean isLastGlyph(int j, int cdsCount, SeqSpan gSpan, GlyphI aGlyph) {
        return j == cdsCount - 1 && gSpan.getEnd() == aGlyph.getCoordBox().x + aGlyph.getCoordBox().width;
    }

    /**
     * Colors by exon frame relative to genomic coordinates
     *
     * @param gl
     * @param genome_codon_start	First position of complete amino acid.
     */
    private void colorByFrame(GlyphI gl, int genome_codon_start) {

        genome_codon_start %= 3;
        if (genome_codon_start == 0) {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME0)));
        } else if (genome_codon_start == 1) {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME1)));
        } else {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME2)));
        }  // genome_codon_start = 2
    }

    /**
     * Colors by exon frame relative to genomic coordinates
     *
     * @param gl
     * @param protSpan	represents a protein annotation, an annotation on the transcript's translated sequence
     * @param genSpan
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genometryImpl.SeqSpan
     */
    private void colorByFrame(GlyphI gl, SeqSpan protSpan, SeqSpan genSpan) {
        double pstart = protSpan.getStartDouble();
        double fraction = Math.abs(pstart - (int) pstart);
        int genome_codon_start = genSpan.getStart();
        int exon_codon_start;
        if (fraction < 0.3) {
            exon_codon_start = 0;
        } else if (fraction < 0.6) {
            exon_codon_start = 2;
        } else {
            exon_codon_start = 1;
        }
        genome_codon_start += exon_codon_start;
        genome_codon_start %= 3;
        if (genome_codon_start == 0) {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME0)));
        } else if (genome_codon_start == 1) {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME1)));
        } else {
            gl.setColor(new Color(protAnnotPreferencesService.getPanelRGB(Panel.FRAME2)));
        }  // genome_codon_start = 2
    }

    /**
     *
     * @param annot2protein
     * @param path2view - "view seq" symmetry enclosed in an array appended with annot2mrna
     * @param tier
     * @see com.affymetrix.genometryImpl.BioSeq
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genoviz.widget.tieredmap.ProtAnnotMapTierGlyph
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.SeqSpan
     * @see com.affymetrix.genometryImpl.symmetry.SymWithProps
     * @see com.affymetrix.genometryImpl.util.SeqUtils
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     */
    private void glyphifyProteinAnnots(
            SeqSymmetry annot2protein,
            SeqSymmetry[] path2view,
            ProtAnnotMapTierGlyph tier) {


        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(annot2protein, annot2genome);

        if (DEBUG_PROTEIN_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }
        SeqUtils.transformSymmetry(annot2genome, path2view);
        if (DEBUG_PROTEIN_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }

        GlyphI aGlyph = new LineContainerProtAnnotGlyph();
        seqmap.setDataModel(aGlyph, annot2protein);

        SeqSpan aSpan = annot2genome.getSpan(vseq);
        if (aSpan == null) {
            SeqSpan span = annot2genome.getSpan(0);
            aSpan = span;
        }
        aGlyph.setCoords(aSpan.getMin(), 0, aSpan.getLength(), 20);
        // will return a color from the prefs for the protein annotation
        // span -- or else the default - col_domain
        Color color = pick_color_for_domain(aSpan, prefs_hash);
        aGlyph.setColor(color);

        // for now, need to descend two levels because that is depth of path --
        //    eventually will use some sort of flattening method (probably
        //    first set up as part of SeqUtils)
        int count1 = annot2genome.getChildCount();
        for (int i = 0; i < count1; i++) {

            SeqSymmetry child = annot2genome.getChild(i);
            int count2 = child.getChildCount();

            // reach "back" and get actual symmetry (rather than transformed symmetry)
            //   really need some sort of tracking in transform mechanism to associate calculated
            //   symmetries with original symmetries that they map back to...
            SymWithProps original_child = (SymWithProps) annot2protein.getChild(i);

            for (int j = 0; j < count2; j++) {
                SeqSymmetry grandchild = child.getChild(j);
                SeqSpan gSpan = grandchild.getSpan(vseq);
                LabelledRectGlyph cglyph = new LabelledRectGlyph();
                if (i % 2 == 0) {
                    cglyph.setColor(color);
                } else {
                    cglyph.setColor(color.darker());
                }

                String spanno = "Span " + String.valueOf(i + 1) + " of ";
                String interpro = (String) ((SymWithProps) annot2protein).getProperty("InterPro name");
                if (interpro != null) {
                    spanno += interpro;
                }
                cglyph.setText(spanno);
                cglyph.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 20);
                aGlyph.addChild(cglyph);
                seqmap.setDataModel(cglyph, original_child);
                original_child.setProperty("type", "protspan");
            }
        }
        tier.addChild(aGlyph);
    }

    /**
     * Returns color for given propertied object
     *
     * @param propertied
     * @return Color
     * @see com.affymetrix.genometryImpl.symmetry.SymWithProps
     */
    private Color pick_color_for_domain(Object propertied, Map<String, Color> prefs_hash) {
        Color to_return = new Color(protAnnotPreferencesService.getPanelRGB(Panel.DOMAIN));
        if (propertied instanceof SymWithProps) {
            Object property = ((SymWithProps) propertied).getProperty("method");
            if (property != null) {
                to_return = prefs_hash.get((String) property);
            }
        }
        return to_return;
    }

    /**
     * Sets the axis map. Sets range,background and foreground color.
     *
     * @see com.affymetrix.genoviz.glyph.SequenceGlyph
     */
    private void setupAxisMap() {
        /* Implementing it in this way because in above method synchronization is lost when
         zoomtoselected feature is used. So to correct it below used method is used */

        axismap.addAxis(upper_white_space + axis_pixel_height);
        ColoredResiduesGlyph sg = new ColoredResiduesGlyph(protAnnotPreferencesService, true);
        sg.setResiduesProvider(gseq, gseq.getLength());
        sg.setCoords(gseq.getMin(), upper_white_space + axis_pixel_height
                + middle_white_space, gseq.getLength(), seq_pixel_height);
        sg.setForegroundColor(col_sequence);
        sg.setBackgroundColor(col_axis_bg);
        axismap.getScene().addGlyph(sg);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        split_pane.repaint();
        stretchToFit(false, true);
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
//        if (e.isPopupTrigger()) {
//            popup.show(this, e.getX(), e.getY());
//            return;
//        }

        if (e.getClickCount() == 2) {
            zoomToSelection();
        }
    }

    /**
     * Sets zoom focus and selects any Glyph underlying the location of the click.
     *
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genoviz.event.NeoMouseEvent
     * @see com.affymetrix.genoviz.widget.NeoMap
     */
    @Override
    public void mousePressed(MouseEvent e) {

        tabbedPane.setSelectedIndex(0);

        if (!(e instanceof NeoMouseEvent)) {
            return;
        }
        NeoMouseEvent nme = (NeoMouseEvent) e;
        Object coord_source = nme.getSource();
        seqmap.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD,
                nme.getCoordY());
        seqmap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD,
                nme.getCoordX());
        axismap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD,
                nme.getCoordX());
        // if alt is down or shift is down, add to previous selection,
        //    otherwise replace previous selection
        boolean multiselect = false;
        if (nme.isAltDown() || nme.isShiftDown()) {
            multiselect = true;
        }

        if (coord_source == axismap) {
            if (!multiselect) {
                seqmap.clearSelected();
            }
        } else {

            List<GlyphI> hitGlyphs = seqmap.getItems(nme.getCoordX(),
                    nme.getCoordY());
            if (!multiselect) {
                seqmap.clearSelected();
            }
            selected = seqmap.getSelected();
            List<GlyphI> to_select = getGlyphsToSelect(hitGlyphs, selected,
                    multiselect);
            if (to_select == null) {
                selected = new ArrayList<>();
            } else {
                if (to_select.size() > 0) {
                    seqmap.select(to_select);
                    selected = to_select;
                }
            }
        }
        zoomPoint.setSpot(seqmap.getZoomCoord(NeoMap.X));

        axismap.updateWidget();
        seqmap.updateWidget();

        showProperties();

        if (hairline != null) {
            hairline.setRange((int) nme.getCoordX(), (int) nme.getCoordX() + 1);
        }

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            popup.show(this, e.getX(), e.getY() + Y_OFFSET_POPUP);
        }
    }

    public JSplitPane getSplitPane() {
        return split_pane;
    }

    public JPanel getTablePanel() {
        return table_view;
    }

    /**
     * Shows properties (meta-data about selected items) in the property table at the bottom of the display.
     *
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genometryImpl.symmetry.SymWithProps
     * @see com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     */
    private void showProperties() {
        Set<Properties> propvec = new HashSet<>();
        Properties props = null;
        for (GlyphI gl : selected) {
            SymWithProps info = null;
            Object candidate = gl.getInfo();
            if (candidate instanceof SymWithProps) {
                info = (SymWithProps) candidate;
                candidate = gl.getParent().getInfo();
                addParentInfo(candidate, info);
                info.setProperty("start", String.valueOf((int) gl.getCoordBox().x));
                info.setProperty("end", String.valueOf((int) (gl.getCoordBox().x + gl.getCoordBox().width)));
                info.setProperty("length", String.valueOf((int) gl.getCoordBox().width));
            } else {
                if (candidate instanceof SimpleMutableSeqSymmetry && exonList.contains((SeqSymmetry) candidate)) {
                    props = new Properties();
                    props.setProperty("start", String.valueOf((int) gl.getCoordBox().x));
                    props.setProperty("end", String.valueOf((int) (gl.getCoordBox().x + gl.getCoordBox().width)));
                    props.setProperty("length", String.valueOf((int) gl.getCoordBox().width));
                    props.setProperty("type", "exon");
                    candidate = gl.getParent().getInfo();
                    if (candidate instanceof SymWithProps) {
                        info = (SymWithProps) candidate;
                        for (Entry<Object, Object> E : props.entrySet()) {
                            info.setProperty((String) E.getKey(), E.getValue());
                        }
                    }
                }
            }
            if (info != null) {
                if (info.getProperties() != null) {
                    propvec.add(convertPropsToProperties(info.getProperties()));
                }
            }
        }
        Properties[] prop_array = propvec.toArray(new Properties[propvec.size()]);
        table_view.showProperties(prop_array);
    }

    public void clearPropertiesTable() {
        table_view.showProperties(new Properties[0]);
    }

    private void addParentInfo(Object candidate, SymWithProps info) {
        if (candidate instanceof SymWithProps) {
            SymWithProps parent = (SymWithProps) candidate;
            for (Entry<String, Object> E : parent.getProperties().entrySet()) {
                if (!ProtannotParser.TYPESTR.equals(E.getKey())
                        && !ProtannotParser.AA_START.equals(E.getKey())
                        && !ProtannotParser.AA_END.equals(E.getKey())
                        && !ProtannotParser.AA_LENGTH.equals(E.getKey())) {
                    info.setProperty(E.getKey(), E.getValue());
                }
            }
        }
    }

    /**
     * Converts Map to Properties to for display in property table.
     *
     * @param prop	a Map containing meta-data name/value pairs for an item
     * @return Properties the meta-data values transformed to Property objects
     */
    private static Properties convertPropsToProperties(Map<String, Object> prop) {
        Properties retval = new Properties();
        for (Entry<String, Object> ent : prop.entrySet()) {
            retval.put(ent.getKey(), ent.getValue());
        }
        return retval;
    }

    /**
     * Decides which glyphs to choose when mouse is clicked.
     *
     * @param clicked_glyphs
     * @param prev_glyphs
     * @param multiselect
     * @return List<GlyphI>
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     */
    private List<GlyphI> getGlyphsToSelect(List<GlyphI> clicked_glyphs,
            List<GlyphI> prev_glyphs,
            boolean multiselect) {
        List<GlyphI> candidates = new ArrayList<>();

        if (multiselect) {
            filterGlyphs(candidates, prev_glyphs);
        }
        filterGlyphs(candidates, clicked_glyphs);

        List<GlyphI> to_return = new ArrayList<>();
        GlyphI champion = null;
        Rectangle2D candidate_box;
        double champion_end = 0;
        double champion_start = 0;
        double championHeight = 0;
        double candidate_end = 0;
        double candidate_start = 0;
        double candidateHeight = 0;
        for (GlyphI candidate : candidates) {
            // we want everything
            if (multiselect) {
                to_return.add(candidate);
            } // we just want the topmost GlyphI
            // to figure out what Glyph is on top we have to think about geometry
            else {
                candidate_box = candidate.getCoordBox();
                candidate_start = candidate_box.getX();
                candidate_end = candidate_box.getX() + candidate_box.getWidth();
                candidateHeight = candidate_box.getHeight();
                // note: if champion is null, we're on the first Glyph - so let the
                // candidate be the champion for now
                if (champion == null
                        || (candidate_end < champion_end && candidate_start >= champion_start)
                        || (candidate_end <= champion_end && candidate_start > champion_start)
                        || candidateHeight > championHeight
                        || (champion.getChildren() != null && champion.getChildren().contains(candidate))) {
                    champion = candidate;
                    champion_start = candidate_start;
                    champion_end = candidate_end;
                    championHeight = candidateHeight;
                }
            }
        }
        if (champion != null) {
            to_return.add(champion);
        }
        return to_return;
    }

    /**
     * Filters out glyphs if no information is present or if it is not a instance of SymWithProps
     *
     * @param gList
     * @param glyphs
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     * @see com.affymetrix.genometryImpl.symmetry.SymWithProps
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     */
    private void filterGlyphs(List<GlyphI> gList, List<GlyphI> glyphs) {
        for (GlyphI g : glyphs) {
            Object info = g.getInfo();
            if (info != null) {
                if ((info instanceof SymWithProps
                        && ((SymWithProps) info).getProperty("type") != null)
                        || exonList.contains((SeqSymmetry) info)) {
                    gList.add(g);
                }
            }
        }
    }

    /**
     * Returns selected Glyphs
     *
     * @return Returns list of selected Glyphs
     * @see com.affymetrix.genoviz.bioviews.GlyphI
     */
    public List<GlyphI> getSelected() {
        return selected;
    }

    /**
     * Return the Properties for current selection.
     */
    public Properties[] getProperties() {
        return table_view.getProperties();
    }

    /**
     * Makes everything visible by zooming out completely.
     */
    void unzoom() {
        seqmap.stretchToFit(true, true);
        seqmap.updateWidget();
        axismap.stretchToFit(true, true);
        axismap.updateWidget();
    }

    /**
     * Zoom to the selected glyphs.
     */
    public void zoomToSelection() {
        List<GlyphI> selections = getSelected();
        if (selections.isEmpty()) {
            return;
        }

        double min_x = -1f;
        double max_x = -1f;

        for (GlyphI glyph : selections) {
            Rectangle2D glyphbox = glyph.getCoordBox();
            if (min_x == -1 || min_x > glyphbox.getX()) {
                min_x = glyphbox.getX();
            }
            if (max_x == -1 || max_x < glyphbox.getX() + glyphbox.getWidth()) {
                max_x = glyphbox.getX() + glyphbox.getWidth();
            }
        }

        double zoom_focus = (min_x + max_x) / 2f;
        double width = max_x - min_x;
        double pixels_per_coord
                = Math.min(seqmap.getView().getPixelBox().width / (width * 1.1f),
                        seqmap.getMaxZoom(NeoAbstractWidget.X));

        if (pixels_per_coord < seqmap.getMinZoom(NeoAbstractWidget.X)) {
            unzoom();
            seqmap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, zoom_focus);
            axismap.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, zoom_focus);
            return;
        }
        for (NeoMap map : maps) {
            map.zoom(NeoAbstractWidget.X, pixels_per_coord);
            double screen_width = map.getVisibleRange()[1] - map.getVisibleRange()[0];
            double scroll_to = min_x + (width * 0.5) - (screen_width * 0.5);
            map.scroll(NeoAbstractWidget.X, scroll_to);
            map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, zoom_focus);
            map.updateWidget(true);
        }

        seqmap.adjustScroller(NeoAbstractWidget.X);
        seqmap.adjustZoomer(NeoAbstractWidget.X);
    }

    private static final Logger logger = LoggerFactory.getLogger(GenomeView.class
    );

    /**
     * Copies a SeqSymmetry. Note that this clears all previous data from the MutableSeqSymmetry.
     *
     * @param sym Source parameter to copy from.
     * @param mut Target parameter to copy to.
     * @see com.affymetrix.genometryImpl.symmetry.SeqSymmetry
     * @see com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry
     * @see com.affymetrix.genometryImpl.SeqSpan
     * @see com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan
     */
    private static void copyToMutable(SeqSymmetry sym, MutableSeqSymmetry mut) {
        mut.clear();
        int spanCount = sym.getSpanCount();
        for (int i = 0; i < spanCount; i++) {
            SeqSpan span = sym.getSpan(i);
            SeqSpan newspan = new SimpleMutableSeqSpan(span);
            mut.addSpan(newspan);
            logger.debug(span.toString());
        }
        int childCount = sym.getChildCount();
        for (int i = 0; i < childCount; i++) {
            SeqSymmetry child = sym.getChild(i);
            MutableSeqSymmetry newchild = new SimpleMutableSeqSymmetry();
            copyToMutable(child, newchild);
            mut.addChild(newchild);
        }
    }

    /**
     * Store old values.
     */
    private void storeCurrentSelection() {
        storeSelected = getSelected();
    }

    /**
     * Restores old values.
     */
    private void restorePreviousSelection() {

        seqmap.select(storeSelected);
        selected = storeSelected;

        zoomPoint.setSpot(seqmap.getZoomCoord(NeoMap.X));
        axismap.updateWidget();
        seqmap.updateWidget();
        showProperties();

    }

    /**
     * Action to be performed when user saves color changes.
     *
     * @param colorhash Map<String,Color> new color preferences
     */
    void changePreference(Map<String, Color> colorhash) {
        protAnnotPreferencesService.updatePrefs(colorhash);
        protAnnotPreferencesService.commit();
        updatePreferences(colorhash);

    }

    private void updatePreferences(Map<String, Color> colorhash) {
        initPrefs(colorhash);
        if (gseq != null) {
            storeCurrentSelection();
            setBioSeq(gseq, false);
            restorePreviousSelection();
        }
        initPrefs(colorhash);
    }

    /**
     * Action to be performed when user attempts to apply color changes.
     *
     * @param colorhash Map<String,Color> new color preferences
     */
    void tempChangePreference(Map<String, Color> colorhash) {
        protAnnotPreferencesService.updatePrefs(colorhash);
        updatePreferences(colorhash);
    }

    /**
     * Action to be performed when user cancel color changes. So revert back to old color preferences.
     *
     */
    void cancelChangePrefernce() {
        protAnnotPreferencesService.abort();
        prefs_hash = protAnnotPreferencesService.getAllColorPreferences();
        if (gseq != null) {
            storeCurrentSelection();
            setBioSeq(gseq, false);
            restorePreviousSelection();
        }
    }

    /**
     * Returns color preferences.
     *
     * @return Map<String,Color> Returns color preferences.
     */
    Map<String, Color> getColorPrefs() {
        return prefs_hash;
    }

    public JRPTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public InterProScanTableModel getIpsTableModel() {
        return ipsTableModel;
    }

}

package org.bioviz.protannot;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.LineContainerGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.TieredNeoMap;
import com.affymetrix.genoviz.widget.VisibleRange;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 */

final class GenomeView extends JPanel implements MouseListener{

    static public enum COLORS
    {
        BACKGROUND("background", Color.white),
        FRAME0("frame0", new Color(0,0,145)),
        FRAME1("frame1", new Color(0,100,255)),
        FRAME2("frame2", new Color(192,192,114)),
        TRANSCRIPT("transcript", Color.black),
        DOMAIN("domain", new Color(84,168,132)),
        EXONSUMMARY("exonsummary", Color.blue);

        private final String name;
        private final Color color;

        COLORS(String nm, Color col)
        {
            this.name = nm;
            this.color = col;
        }

        @Override
        public String toString()
        {
            return name;
        }

        public Color defaultColor()
        {
            return color;
        }

        public int getRGB()
        {
            return color.getRGB();
        }

        static public Hashtable<String,Color> defaultColorList()
        {
            Hashtable<String,Color> defaults = new Hashtable<String,Color>();

            for(COLORS C : values())
                defaults.put(C.toString(), C.defaultColor());
            
            return defaults;
        }
    };

    public  JPopupMenu popup;
    private boolean rev_comp = false;
    private boolean DEBUG_GENOMIC_ANNOTS = false;
    private boolean DEBUG_TRANSCRIPT_ANNOTS = false;
    private boolean DEBUG_PROTEIN_ANNOTS = false;
    private TieredNeoMap seqmap;
    private NeoMap axismap;
    private NeoMap[] maps;
    private ModPropertySheet table_view;    
    private AdjustableJSlider xzoomer;
    private AdjustableJSlider yzoomer;
    private BioSeq gseq;
    private BioSeq vseq;
    private List<GlyphI> exonGlyphs = null;
    private List<SeqSymmetry> exonList = new ArrayList<SeqSymmetry>();
    private Hashtable<String,Color> prefs_hash;

    private Color col_bg = COLORS.BACKGROUND.defaultColor();
    private Color col_frame0 = COLORS.FRAME0.defaultColor();
    private Color col_frame1 = COLORS.FRAME1.defaultColor();
    private Color col_frame2 = COLORS.FRAME2.defaultColor();
    private Color col_ts = COLORS.TRANSCRIPT.defaultColor();
    private Color col_domain = COLORS.DOMAIN.defaultColor();
    private Color col_exon_summary = COLORS.EXONSUMMARY.defaultColor();
    private Color col_sequence = Color.black;
    private Color col_axis_bg = Color.lightGray;
    
    private List<GlyphI> selected = new ArrayList<GlyphI>();
    private List<GlyphI> storeSelected;
    private VisibleRange zoomPoint;
    
    // size constants
    private int axis_pixel_height = 20;
    private int seq_pixel_height = 10;
    private int upper_white_space = 5;
    private int middle_white_space = 2;
    private int lower_white_space = 2;
    private int divider_size = 8;
    private int table_height = 150;
    private int seqmap_pixel_height = 500;

    
    /**
     * Removes currently loaded data by clearing maps.
     */
    void no_data() {
        seqmap.clearWidget();
        axismap.clearWidget();
        seqmap.updateWidget();
        axismap.updateWidget();
        table_view.showProperties(new Properties[0]);
    }

    /**
     * Sets up the layout
     * @param   phash   Color perefrences stored in hashtable to setup the layout.
     * @see     om.affymetrix.genoviz.widget.NeoAbstractWidget
     */
    GenomeView(Hashtable<String,Color> phash) {

        initPrefs(phash);
        popup = new JPopupMenu();
        seqmap = new TieredNeoMap(true, false);
        seqmap.setReshapeBehavior(NeoAbstractWidget.X, NeoAbstractWidget.FITWIDGET);
        seqmap.setReshapeBehavior(NeoAbstractWidget.Y, NeoAbstractWidget.FITWIDGET);
        seqmap.setMapOffset(0, seqmap_pixel_height);
        axismap = new NeoMap(false, false);
        axismap.setMapColor(col_axis_bg);
        //axismap.setReshapeBehavior(NeoAbstractWidget.X, NeoAbstractWidget.FITWIDGET);
        axismap.setMapOffset(0, axis_pixel_height + seq_pixel_height
                + upper_white_space + middle_white_space
                + lower_white_space);
        JScrollBar y_scroller = new JScrollBar(JScrollBar.VERTICAL);                
        seqmap.setOffsetScroller(y_scroller);

        xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
        xzoomer.setBackground(Color.white);
        yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
        yzoomer.setBackground(Color.white);
        
        seqmap.setZoomer(NeoMap.X, xzoomer);
        seqmap.setZoomer(NeoMap.Y, yzoomer);
        
        axismap.setZoomer(NeoMap.X, seqmap.getZoomer(TieredNeoMap.X));
//        axismap.setRangeScroller(seqmap.getScroller(TieredNeoMap.X));
        

        seqmap.getScroller(NeoMap.X).addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                axismap.getScroller(NeoMap.X).setValue(seqmap.getScroller(NeoMap.X).getValue());
            }
        });

        seqmap.getZoomer(NeoMap.X).addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                axismap.getScroller(NeoMap.X).setValue(seqmap.getScroller(NeoMap.X).getValue());
            }
        });

        this.setLayout(new BorderLayout());

        JPanel map_panel = new JPanel();

        map_panel.setLayout(new BorderLayout());
        map_panel.add("North", axismap);
        seqmap.setPreferredSize(new Dimension(100, seqmap_pixel_height));
        seqmap.setBackground(col_bg);
        map_panel.add("Center", seqmap);
        JPanel right = new JPanel();
        right.setLayout(new GridLayout(1, 2));
        right.add(y_scroller);
        right.add(yzoomer);
        int maps_height = axis_pixel_height + seq_pixel_height
                + upper_white_space + middle_white_space + lower_white_space
                + divider_size + seqmap_pixel_height;

        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(seqmap.getWidth(), maps_height));
        p.setLayout(new BorderLayout());
        p.add("Center", map_panel);
        p.add("East", right);
        map_panel.add("South", xzoomer);
        table_view = new ModPropertySheet();
        table_view.setPreferredSize(new Dimension(seqmap.getWidth(), table_height));

        this.add("Center", p);
        this.add("South", table_view);
        seqmap.addMouseListener(this);
        seqmap.setSelectionEvent(TieredNeoMap.NO_SELECTION);

        seqmap.setSelectionAppearance(Scene.SELECT_OUTLINE);
        axismap.addMouseListener(this);
        axismap.setSelectionEvent(NeoMap.NO_SELECTION);
        axismap.setSize(seqmap.getSize().width, upper_white_space
                + middle_white_space + lower_white_space
                + axis_pixel_height + seq_pixel_height);
        maps = new NeoMap[2];
        maps[0] = seqmap;
        maps[1] = axismap;

    }

    
    /**
     * Initialized GenomeView colors with prefrences provided in the parameter phash
     * @param   phash   Hashtable providing color prefrences for GenomeView
     */
    private void initPrefs(Hashtable<String,Color> phash) {
        tempColorPrefs(phash);
        prefs_hash = phash;
    }

    /**
     * Changes color preferences
     * @param phash     Hashtable<String,Color> 
     */
    private void tempColorPrefs(Hashtable<String,Color> phash)
    {
        if (phash == null) {
            return;
        }

        if (phash.containsKey(COLORS.BACKGROUND.toString())) {
            col_bg = phash.get(COLORS.BACKGROUND.toString());
        }
        if (phash.containsKey(COLORS.FRAME0.toString())) {
            col_frame0 = phash.get(COLORS.FRAME0.toString());
        }
        if (phash.containsKey(COLORS.FRAME1.toString())) {
            col_frame1 = phash.get(COLORS.FRAME1.toString());
        }
        if (phash.containsKey(COLORS.FRAME2.toString())) {
            col_frame2 = phash.get(COLORS.FRAME2.toString());
        }
        if (phash.containsKey(COLORS.TRANSCRIPT.toString())) {
            col_ts = phash.get(COLORS.TRANSCRIPT.toString());
        }
        if (phash.containsKey(COLORS.DOMAIN.toString())) {
            col_domain = phash.get(COLORS.DOMAIN.toString());
        }
        if (phash.containsKey(COLORS.EXONSUMMARY.toString())) {
            col_exon_summary = phash.get(COLORS.EXONSUMMARY.toString());
        }
    }

    /**
     * Add mouse listener to maps.
     * @param   listener    Lintener that is to be added to maps.
     */
    void addMapListener(MouseListener listener) {
        seqmap.addMouseListener(listener);
        axismap.addMouseListener(listener);
    }

    /**
     * 
     * @param   gseq
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genoviz.widget.Shadow
     * @see     com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker
     * @see     com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph
     */
    void setBioSeq(BioSeq gseq, boolean is_new) {
        this.gseq = gseq;
        seqmap.clearWidget();
        seqmap.setMapRange(0, gseq.getLength());
        axismap.clearWidget();
        seqmap.setBackground(col_bg);
        
        exonGlyphs = new ArrayList<GlyphI>();
        exonList = new ArrayList<SeqSymmetry>();

        zoomPoint = new VisibleRange();
        Shadow hairline = new Shadow( this.seqmap, com.affymetrix.genoviz.util.NeoConstants.HORIZONTAL, Color.black );
	hairline.setSelectable( false );
	zoomPoint.addListener( hairline );

        Shadow axishairline = new Shadow( this.axismap, com.affymetrix.genoviz.util.NeoConstants.HORIZONTAL, Color.black );
	axishairline.setSelectable( false );
	zoomPoint.addListener( axishairline );
                
        int acount = gseq.getAnnotationCount();

        SeqSymmetry[] path2view = new SeqSymmetry[1];
        MutableSeqSymmetry viewSym = new SimpleMutableSeqSymmetry();
        viewSym.addSpan(new SimpleSeqSpan(0, gseq.getLength(), gseq));
        vseq = new BioSeq("view seq", null, gseq.getLength());

        if (rev_comp) {
            viewSym.addSpan(new SimpleSeqSpan(gseq.getLength(), 0, vseq));
        } else {
            viewSym.addSpan(new SimpleSeqSpan(0, gseq.getLength(), vseq));
        }

        path2view[0] = viewSym;

        for (int i = 0; i < acount; i++) {
            SeqSymmetry asym = gseq.getAnnotation(i);
            if (DEBUG_GENOMIC_ANNOTS) {
                SeqUtils.printSymmetry(asym);
            }
            glyphifyMRNA(asym, path2view);
        }

        MapTierGlyph sumTier = new MapTierGlyph();
        //    sumTier.setCoords(0, 0, gseq.getLength(), 20);
        sumTier.setCoords(0, seqmap_pixel_height - 20, gseq.getLength(), 20);
        sumTier.setState(MapTierGlyph.EXPANDED);

        ExpandedTierPacker epack = (ExpandedTierPacker) sumTier.getExpandedPacker();
        epack.setMoveType(ExpandedTierPacker.DOWN);
        GlyphSummarizer summer = new GlyphSummarizer(col_exon_summary);
        if (exonGlyphs.size() > 0) {
            GlyphI gl = summer.getSummaryGlyph(exonGlyphs);
            sumTier.addChild(gl);
        }
        seqmap.addTier(sumTier);
        seqmap.repack();

        table_view.showProperties(new Properties[0]);
        setupAxisMap();

        if(is_new)
        {
            axismap.stretchToFit(true, false);
            seqmap.stretchToFit(true, true);
        }
        else
        {
            axismap.stretchToFit(false, false);
            seqmap.stretchToFit(false, false);
        }
        
        seqmap.updateWidget();
        axismap.updateWidget();
        
    }

    /**
     * Sets the title of the frame provided by the parameter title
     * @param   title   - Title of the frame. Usually the name of the file
     */
    void setTitle(String title) {
        table_view.setTitle(title);
    }

    /**
     * Make RNA into a glyph.
     * @param   mrna2genome
     * @param   path2view
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph
     */
    private void glyphifyMRNA(SeqSymmetry mrna2genome, SeqSymmetry[] path2view) {
        int childcount = mrna2genome.getChildCount();
        MapTierGlyph tier = new MapTierGlyph();
        tier.setCoords(0, 0, gseq.getLength(), 80);
        tier.setState(MapTierGlyph.EXPANDED);
        ExpandedTierPacker epack = (ExpandedTierPacker) tier.getExpandedPacker();
        epack.setMoveType(ExpandedTierPacker.DOWN);
        seqmap.addTier(tier);

        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(mrna2genome, annot2genome);

        SeqUtils.transformSymmetry(annot2genome, path2view);
        GlyphI tGlyph = new LineContainerGlyph();
        seqmap.setDataModel(tGlyph, mrna2genome);
        SeqSpan tSpan = annot2genome.getSpan(vseq);
        tGlyph.setCoords(tSpan.getMin(), 0, tSpan.getLength(), 20);
        tGlyph.setColor(col_ts);
        for (int i = 0; i < childcount; i++) {
            SeqSymmetry exon2genome = annot2genome.getChild(i);
            SeqSpan gSpan = exon2genome.getSpan(vseq);
            GlyphI cglyph = new OutlineRectGlyph();
            seqmap.setDataModel(cglyph, exon2genome);
            // rats -- can't give this a type and therefore signal
            // to the selection logic that this is first class selectable
            // object
            // so let's put it in a list
            exonList.add(exon2genome);
            cglyph.setColor(col_ts);
            cglyph.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 20);
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
                    if (seg_gspan.getLength() == 0) {  // only mark the inserts (those whose genomic extent is zero
                        GlyphI segGlyph = new OutlineRectGlyph();
                        segGlyph.setColor(col_bg);
                        segGlyph.setCoords(seg_gspan.getMin(), 0, seg_gspan.getLength(), 25);
                        tGlyph.addChild(segGlyph);
                    }
                }
        }
        tier.addChild(tGlyph);

        // now follow symmetry link to annotated mrna seqs, map those annotations to genomic
        //    coords, and display
        //    BioSeq mrna = SeqUtils.getOtherSpan(mrna2genome, tSpan).getBioSeq();

        BioSeq mrna = SeqUtils.getOtherSpan(mrna2genome, mrna2genome.getSpan(gseq)).getBioSeq();
        if (DEBUG_TRANSCRIPT_ANNOTS) {
            System.out.println(mrna.getID() + ",  " + mrna);
        }
        if (mrna instanceof BioSeq) {
            SeqSymmetry[] new_path2view = new SeqSymmetry[path2view.length + 1];
            System.arraycopy(path2view, 0, new_path2view, 1, path2view.length);
            new_path2view[0] = mrna2genome;

            BioSeq amrna = mrna;

            int acount = amrna.getAnnotationCount();
            for (int i = 0; i < acount; i++) {
                SeqSymmetry annot2mrna = amrna.getAnnotation(i);
                if (annot2mrna != mrna2genome) {
                    // old way	  glyphifyTranscriptAnnots(amrna, annot2mrna, mrna2genome, tier, tGlyph);

                    glyphifyTranscriptAnnots(amrna, annot2mrna, new_path2view, tier, tGlyph);
                }
            }
        }
    }

    /**
     *
     * @param   amrna
     * @param   annot2mrna
     * @param   path2view
     * @param   tier
     * @param   trans_parent
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void glyphifyTranscriptAnnots(BioSeq amrna,
            SeqSymmetry annot2mrna, SeqSymmetry[] path2view,
            MapTierGlyph tier, GlyphI trans_parent) {
        if (DEBUG_TRANSCRIPT_ANNOTS) {
            SeqUtils.printSymmetry(annot2mrna);
        }
        SeqSpan mrna_span = annot2mrna.getSpan(amrna);
        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(annot2mrna, annot2genome);
        
        SeqUtils.transformSymmetry(annot2genome, path2view);
        if (DEBUG_TRANSCRIPT_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }

        SeqSpan pSpan = SeqUtils.getOtherSpan(annot2mrna, mrna_span);
        BioSeq protein = (BioSeq) pSpan.getBioSeq();


        GlyphI aGlyph = new LineContainerGlyph();
        SeqSpan aSpan = annot2genome.getSpan(vseq);
        aGlyph.setCoords(aSpan.getMin(), 0, aSpan.getLength(), 20);
        aGlyph.setColor(col_ts);
        seqmap.setDataModel(aGlyph, annot2mrna);
        int cdsCount = annot2genome.getChildCount();
        for (int j = 0; j < cdsCount; j++) {
            SeqSymmetry cds2genome = annot2genome.getChild(j);
            SeqSpan gSpan = cds2genome.getSpan(vseq);
            GlyphI cglyph = new FillRectGlyph();

            // coloring based on frame
            SeqSpan protSpan = cds2genome.getSpan(protein);

            colorByFrame(cglyph, protSpan, gSpan);
            cglyph.setCoords(gSpan.getMin(), 0, gSpan.getLength(), 20);
            aGlyph.addChild(cglyph);
        }
        trans_parent.addChild(aGlyph);

        // now follow symmetry link to annotated mrna seqs, map those annotations to genomic
        //    coords, and display
        if (DEBUG_PROTEIN_ANNOTS) {
            System.out.println(protein.getID() + ",  " + protein);
        }
        if (protein instanceof BioSeq) {
            SeqSymmetry prot2mrna = annot2mrna;

            // construct a new path which includes entire previous path plus prot2mrna
            //   new path info is added to _beginning_ of path
            // hmm, this is starting to look like a good argument for some sort of List/Vector/Stack
            //   for the path instead of arrays...
            SeqSymmetry[] new_path2view = new SeqSymmetry[path2view.length + 1];
            System.arraycopy(path2view, 0, new_path2view, 1, path2view.length);
            new_path2view[0] = prot2mrna;

            BioSeq aprotein = protein;
            int acount = aprotein.getAnnotationCount();
            for (int i = 0; i < acount; i++) {
                SeqSymmetry annot2protein = aprotein.getAnnotation(i);
                if (annot2protein != prot2mrna) {
                    glyphifyProteinAnnots(aprotein, annot2protein, new_path2view, tier);
                }
            }
        }
    }

    /**
     * Colors by exon frame relative to _genomic_ coordinates
     * @param   gl
     * @param   protSpan
     * @param   genSpan
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genometryImpl.SeqSpan
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
        genome_codon_start = genome_codon_start % 3;
        if (genome_codon_start == 0) {
            gl.setColor(col_frame0);
        } else if (genome_codon_start == 1) {
            gl.setColor(col_frame1);
        } else {
            gl.setColor(col_frame2);
        }  // genome_codon_start = 2
    }

    /**
     *
     * @param   protein
     * @param   annot2protein
     * @param   path2view
     * @param   tier
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     */
    private void glyphifyProteinAnnots(BioSeq protein,
            SeqSymmetry annot2protein,
            SeqSymmetry[] path2view,
            MapTierGlyph tier) {

        MutableSeqSymmetry annot2genome = new SimpleMutableSeqSymmetry();
        copyToMutable(annot2protein, annot2genome); 

        if (DEBUG_PROTEIN_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }
        SeqUtils.transformSymmetry(annot2genome, path2view);
        if (DEBUG_PROTEIN_ANNOTS) {
            SeqUtils.printSymmetry(annot2genome);
        }

        GlyphI aGlyph = new LineContainerGlyph();
        seqmap.setDataModel(aGlyph, annot2protein);

        SeqSpan aSpan = annot2genome.getSpan(vseq);
        aGlyph.setCoords(aSpan.getMin(), 0, aSpan.getLength(), 20);
        // will return a color from the prefs for the protein annotation
        // span -- or else the default - col_domain
        Color color = pick_color_for_domain(aSpan);
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
                GlyphI cglyph = new FillRectGlyph();
                cglyph.setColor(color);
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
     * @param   propertied
     * @return  Color
     * @see     com.affymetrix.genometryImpl.SymWithProps
     */
    private Color pick_color_for_domain(Object propertied) {
        Color to_return = col_domain;
        if (propertied instanceof SymWithProps) {
            Object property = ((SymWithProps) propertied).getProperty("method");
            if (property != null) {
                to_return = prefs_hash.get((String)property);
            }
        }
        return to_return;
    }

    /**
     * Sets the axismap. Sets range,background and foreground color
     * @see     com.affymetrix.genoviz.glyph.SequenceGlyph
     */
    private void setupAxisMap() { 
        axismap.setMapRange(0, gseq.getLength());

        /* Implementing it in this way because in above method synchronization is lost when
         zoomtoselected feature is used. So to correct it below used method is used */
        
        axismap.addAxis(upper_white_space+axis_pixel_height);
        String residues = gseq.getResidues();
        SequenceGlyph sg = new ColoredResiduesGlyph();
        sg.setResidues(residues);
        sg.setCoords(0, upper_white_space + axis_pixel_height
                + middle_white_space, gseq.getLength(), seq_pixel_height);
        sg.setForegroundColor(col_sequence);
        sg.setBackgroundColor(col_axis_bg);
        axismap.getScene().addGlyph(sg);     
    }

    /** MouseListener interface implementation */
    public void mouseClicked(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseEntered(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseExited(MouseEvent e) {
    }

    /** MouseListener interface implementation */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Sets zoom foucs. If clicked on any glpyh then it is selected.
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genoviz.event.NeoMouseEvent
     * @see     com.affymetrix.genoviz.widget.NeoMap
     */
    public void mousePressed(MouseEvent e) {

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
        } else
        {

            List<GlyphI> hitGlyphs = seqmap.getItems(nme.getCoordX(),
                    nme.getCoordY());
            if (!multiselect) {
                seqmap.clearSelected();
            }
            selected = seqmap.getSelected();
            List<GlyphI> to_select = getGlyphsToSelect(hitGlyphs, selected,
                    multiselect);
            if (to_select == null) {
                selected = new ArrayList<GlyphI>();
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

     
        if (e.isPopupTrigger()) {
            popup.show(this, e.getX(), e.getY());
        }
    }

    /**
     * Shows property in the property table
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     */
    private void showProperties() {
        List<Properties> propvec = new ArrayList<Properties>();
        Properties props = null;
        for (GlyphI gl : selected) {
            SymWithProps info = null;
            Object candidate = gl.getInfo();
            if (candidate instanceof SymWithProps) {
                info = (SymWithProps) candidate;
                candidate = gl.getParent().getInfo();
                    if (candidate instanceof SymWithProps) {
                        SymWithProps parent = (SymWithProps) candidate;
                        for(Entry E: parent.getProperties().entrySet())
                            info.setProperty((String) E.getKey(),E.getValue());
                    }
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
                        for(Entry E: props.entrySet())
                            info.setProperty( (String) E.getKey(),E.getValue());
                    }
                }
            }
            if (info != null) {
                if (info.getProperties() != null) {
                    propvec.add(convertPropsToProperties(info.getProperties()));
                }
            }
        }
		Properties[] prop_array = (Properties[])propvec.toArray(new Properties[0]);
        table_view.showProperties(prop_array);
    }

    /**
     * Coverts Map to Properties to be able use in property table
     * @param   prop
     * @return  Properties
     */
    private Properties convertPropsToProperties(Map<String, Object> prop) {
        Properties retval = new Properties();
        for (Entry<String, Object> ent : prop.entrySet()) {
            retval.put(ent.getKey(), ent.getValue());
        }
        return retval;
    }

    /**
     * Decides which glyphs to choose when mouse is clicked.
     * @param   clicked_glyphs
     * @param   prev_glyphs
     * @param   multiselect
     * @return  List<GlyphI>
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     */
    private List<GlyphI> getGlyphsToSelect(List<GlyphI> clicked_glyphs,
            List<GlyphI> prev_glyphs,
            boolean multiselect) {
        List<GlyphI> candidates = new ArrayList<GlyphI>();
        filterGlyphs(candidates, clicked_glyphs);
        if (multiselect) {
            filterGlyphs(candidates, prev_glyphs);
        }
        List<GlyphI> to_return = new ArrayList<GlyphI>();
        GlyphI champion = null;
        Rectangle2D candidate_box;
        double champion_end = 0;
        double champion_start = 0;
        double candidate_end = 0;
        double candidate_start = 0;
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
                // note: if champion is null, we're on the first Glyph - so let the
                // candidate be the champion for now
                if (champion == null
                        || (candidate_end < champion_end && candidate_start >= champion_start)
                        || (candidate_end <= champion_end && candidate_start > champion_start)
                        || // we leave the most computationally intensive test for last
                        (champion.getChildren() != null && champion.getChildren().contains(candidate))) {
                    champion = candidate;
                    champion_start = candidate_start;
                    champion_end = candidate_end;
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
     * @param   gList
     * @param   glyphs
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     */
    private void filterGlyphs(List<GlyphI> gList, List<GlyphI> glyphs) {
        for (GlyphI g : glyphs) {
            Object info = g.getInfo();
            if (info != null) {
                if ((info instanceof SymWithProps
                        && ((SymWithProps) info).getProperty("type") != null)
                        || exonList.contains((SeqSymmetry)info)) {
                    gList.add(g);
                }
            }
        }
    }

    /**
     * Returns selected Glyphs
     * @return  Returns list of selected Glyphs
     * @see     com.affymetrix.genoviz.bioviews.GlyphI
     */
    List<GlyphI> getSelected() {
        return selected;
    }

    /**
     * Return the Properties for whatever's currently selected.
     */
    Properties[] getProperties() {
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
    void zoomToSelection() {       
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
        double pixels_per_coord =
                Math.min(seqmap.getView().getPixelBox().width / (width * 1.1f),
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
    }

    /**
     * Copies a SeqSymmetry.
     * Note that this clears all previous data from the MutableSeqSymmetry.
     * @param   sym Source parameter to copy from.
     * @param   mut Target parameter to copy to.
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan
     */
    private static final void copyToMutable(SeqSymmetry sym, MutableSeqSymmetry mut) {
        mut.clear();
        int spanCount = sym.getSpanCount();
        for (int i = 0; i < spanCount; i++) {
            SeqSpan span = sym.getSpan(i);
            SeqSpan newspan = new SimpleMutableSeqSpan(span);
            mut.addSpan(newspan);
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
     * @param   colorhash   Hashtable<String,Color> new color preferences
     */
    public void changePreference(Hashtable<String,Color> colorhash)
    {
        tempChangePreference(colorhash);
        initPrefs(colorhash);
    }

    /**
     * Action to be performed when user apply color changes.
     * @param   colorhash   Hashtable<String,Color> new color preferences
     */
    public void tempChangePreference(Hashtable<String,Color> colorhash)
    {
        tempColorPrefs(colorhash);
        if(gseq != null)
        {
            storeCurrentSelection();
            setBioSeq(gseq,false);
            restorePreviousSelection();
        }
    }

    /**
     * Action to be performed when user cancel color changes. So revert back to old color preferences.
     * @param   colorhash   Hashtable<String,Color> new color preferences
     */
    public void cancelChangePrefernce()
    {
        tempColorPrefs(prefs_hash);
        if(gseq != null)
        {
            storeCurrentSelection();
            setBioSeq(gseq,false);
            restorePreviousSelection();
        }
    }

    /**
     * Returns color preferences.
     * @return  Hashtable<String,Color>     Returns color preferences.
     */
    public Hashtable<String,Color> getColorPrefs()
    {
        return prefs_hash;
    }
}



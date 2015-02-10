package com.gene.transcriptisoform;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.SeqMapRefreshed;
import com.affymetrix.genometry.event.SeqSelectionEvent;
import com.affymetrix.genometry.event.SeqSelectionListener;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.igb.service.api.IgbService;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

public class TranscriptIsoformEvidenceVisualizationManager implements SeqMapRefreshed, SeqSelectionListener, MouseListener, MouseMotionListener {

    private List<TierGlyph> refSeqTiers;
    private final Map<StyledGlyph.Direction, Map<SimpleSeqSpan, Set<GlyphI>>> intronSpan2Glyphs;
    private int maxCount;
    private boolean showUnfound = true;
    private IgbService igbService;

    private ExonConnectorGlyph.DensityDisplay showDensity = ExonConnectorGlyph.DensityDisplay.THICKNESS;

    public TranscriptIsoformEvidenceVisualizationManager(IgbService igbService) {
        this.refSeqTiers = new ArrayList<>();
        intronSpan2Glyphs = new EnumMap<>(StyledGlyph.Direction.class);
        intronSpan2Glyphs.put(StyledGlyph.Direction.FORWARD, new HashMap<>());
        intronSpan2Glyphs.put(StyledGlyph.Direction.REVERSE, new HashMap<>());
        igbService.getSeqMapView().addToRefreshList(this);
        igbService.getSeqMap().addMouseListener(this);
        igbService.getSeqMap().addMouseMotionListener(this);
        GenometryModel.getInstance().addSeqSelectionListener(this);
    }

    public boolean isShowUnfound() {
        return showUnfound;
    }

    public void setShowUnfound(boolean showUnfound) {
        this.showUnfound = showUnfound;
        updateDisplay();
    }

    public ExonConnectorGlyph.DensityDisplay getShowDensity() {
        return showDensity;
    }

    private void setDensity(ExonConnectorGlyph.DensityDisplay densityDisplay) {
        this.showDensity = densityDisplay;
        updateDisplay();
    }

    public void setShowDensityThickness() {
        setDensity(ExonConnectorGlyph.DensityDisplay.THICKNESS);
    }

    public void setShowDensityTransparency() {
        setDensity(ExonConnectorGlyph.DensityDisplay.TRANSPARENCY);
    }

    public void setShowDensityBrightness() {
        setDensity(ExonConnectorGlyph.DensityDisplay.BRIGHTNESS);
    }

    private String getExtension(String uri) {
        if (uri == null) {
            return null;
        }
        String uriString = uri.toLowerCase();
        String unzippedStreamName = GeneralUtils.stripEndings(uriString);
        return GeneralUtils.getExtension(unzippedStreamName);
    }

    public void setRefSeqTiers(List<TierGlyph> refSeqTiers) {
        if (refSeqTiers == null || refSeqTiers.isEmpty()) {
            ErrorHandler.errorPanel("no tiers selected");
            return;
        }
//		for (TierGlyph tierGlyph : refSeqTiers) {
//			if (!isBedTrack(getTrackURL(tierGlyph))) {
//				ErrorHandler.errorPanel(trackURL + " is not a BED track");
//				return;
//			}
//		}
        clearExonGlyphs();
        this.refSeqTiers = refSeqTiers;
        updateDisplay();
    }

    private boolean isCigarTier(TierGlyph glyph) {
        String extension = getExtension(glyph.getAnnotStyle().getMethodName());
        if (".bam".equals(extension) || ".sam".equals(extension)) {
            return true;
        }
        return false;
    }

    public void clearExonConnectorGlyphs() {
        // remove existing ExonConnectorGlyphs
        for (TierGlyph refSeqTier : refSeqTiers) {
            List<GlyphI> glyphs = refSeqTier.getChildren();
            if (glyphs != null) {
                for (GlyphI glyph : new ArrayList<>(glyphs)) {
                    if (glyph instanceof ExonConnectorGlyph) {
                        glyphs.remove(glyph);
                    }
                }
            }
        }
    }

    private static Map<StyledGlyph.Direction, List<StyledGlyph.Direction>> directionMap = new EnumMap<>(StyledGlyph.Direction.class);

    static {
        List<StyledGlyph.Direction> forward = new ArrayList<>();
        forward.add(StyledGlyph.Direction.FORWARD);
        directionMap.put(StyledGlyph.Direction.FORWARD, forward);
        List<StyledGlyph.Direction> reverse = new ArrayList<>();
        reverse.add(StyledGlyph.Direction.REVERSE);
        directionMap.put(StyledGlyph.Direction.REVERSE, reverse);
        List<StyledGlyph.Direction> both = new ArrayList<>();
        both.add(StyledGlyph.Direction.FORWARD);
        both.add(StyledGlyph.Direction.REVERSE);
        directionMap.put(StyledGlyph.Direction.BOTH, both);
    }

    private Set<SimpleSeqSpan> addFoundIntrons(BioSeq seq, TierGlyph refSeqTier) {
        // add a new ExonConnectorGlyph to introns in the refseq
        Set<SimpleSeqSpan> foundSpans = new HashSet<>();
        SeqSymmetry mainSym = (SeqSymmetry) refSeqTier.getInfo();
        for (int i = 0; i < mainSym.getChildCount(); i++) {
            SeqSymmetry geneSym = mainSym.getChild(i);
            if (geneSym.getChildCount() > 0) {
                Map<Integer, SeqSymmetry> startSpanMap = new HashMap<>();
                Map<Integer, SeqSymmetry> endSpanMap = new HashMap<>();
                for (int index = 0; index < geneSym.getChildCount(); index++) {
                    SeqSymmetry childSym = geneSym.getChild(index);
                    SeqSpan exonSpan = childSym.getSpan(seq);
                    if (exonSpan != null) {
                        startSpanMap.put(exonSpan.getMin(), childSym);
                        endSpanMap.put(exonSpan.getMax(), childSym);
                    }
                }
                for (StyledGlyph.Direction direction : directionMap.get(refSeqTier.getDirection())) {
                    for (SimpleSeqSpan intronSpan : intronSpan2Glyphs.get(direction).keySet()) {
                        SeqSymmetry startSym = endSpanMap.get(intronSpan.getStart());
                        SeqSymmetry endSym = startSpanMap.get(intronSpan.getEnd());
                        if (startSym != null && endSym != null) {
                            ExonConnectorGlyph exonConnectorGlyph = new ExonConnectorGlyph(intronSpan, intronSpan2Glyphs.get(direction).get(intronSpan), maxCount, igbService.getSeqMap().getItem(startSym), igbService.getSeqMap().getItem(endSym), startSym.getSpan(seq).isForward(), showDensity);
                            refSeqTier.addChild(exonConnectorGlyph);
                            exonConnectorGlyph.init();
                            foundSpans.add(intronSpan);
                        }
                    }
                }
            }
        }
        return foundSpans;
    }

    private void clearExonGlyphs() {
        if (refSeqTiers != null) {
            for (TierGlyph glyph : refSeqTiers) {
                for (int i = glyph.getChildCount() - 1; i >= 0; i--) {
                    if (glyph.getChild(i) instanceof ExonConnectorGlyph) {
                        glyph.removeChild(glyph.getChild(i));
                    }
                }
            }
        }
    }

    private void addUnfoundIntrons(Set<SimpleSeqSpan> unfoundSpans, TierGlyph refSeqTier) {
        // add a new ExonConnectorGlyph for each unfound intron span
        for (SimpleSeqSpan intronSpan : unfoundSpans) {
            Set<GlyphI> glyphs = new HashSet<>();
            if (refSeqTier.getDirection() == StyledGlyph.Direction.BOTH) {
                if (intronSpan2Glyphs.get(StyledGlyph.Direction.FORWARD).get(intronSpan) != null) {
                    glyphs.addAll(intronSpan2Glyphs.get(StyledGlyph.Direction.FORWARD).get(intronSpan));
                }
                if (intronSpan2Glyphs.get(StyledGlyph.Direction.REVERSE).get(intronSpan) != null) {
                    glyphs.addAll(intronSpan2Glyphs.get(StyledGlyph.Direction.REVERSE).get(intronSpan));
                }
            } else {
                if (intronSpan2Glyphs.get(refSeqTier.getDirection()).get(intronSpan) != null) {
                    glyphs = intronSpan2Glyphs.get(refSeqTier.getDirection()).get(intronSpan);
                }
            }
            ExonConnectorGlyph exonConnectorGlyph = new ExonConnectorGlyph(intronSpan, glyphs, maxCount, null, null, refSeqTier.getDirection() == StyledGlyph.Direction.FORWARD, showDensity);
            refSeqTier.addChild(exonConnectorGlyph);
            exonConnectorGlyph.init();
        }
    }

    private void displayIsoforms(List<TierGlyph> labelGlyphs) {
        clearExonConnectorGlyphs();
        BioSeq seq = GenometryModel.getInstance().getSelectedSeq();
        for (TierGlyph reference_tier : labelGlyphs) {
            if (reference_tier.getChildren() != null && isCigarTier(reference_tier)) {
                for (GlyphI glyph : reference_tier.getChildren()) {
                    processGlyph(glyph);
                }
            }
        }
        // check count
        if (maxCount == 0) {
            return;
        }
        // add found ExonConnectorGlyphs
        for (TierGlyph refSeqTier : refSeqTiers) {
            Set<SimpleSeqSpan> foundSpans = new HashSet<>();
            if (refSeqTier.getInfo() instanceof SeqSymmetry) {
                foundSpans.addAll(addFoundIntrons(seq, refSeqTier));
            }
            if (showUnfound) {
                StyledGlyph.Direction direction = refSeqTier.getDirection();
                Set<SimpleSeqSpan> unfoundSpans;
                if (direction == StyledGlyph.Direction.BOTH) {
                    unfoundSpans = new HashSet<>(intronSpan2Glyphs.get(StyledGlyph.Direction.FORWARD).keySet());
                    unfoundSpans.addAll(intronSpan2Glyphs.get(StyledGlyph.Direction.REVERSE).keySet());
                } else {
                    unfoundSpans = new HashSet<>(intronSpan2Glyphs.get(direction).keySet());
                }
                unfoundSpans.removeAll(foundSpans);
                addUnfoundIntrons(unfoundSpans, refSeqTier);
            }
        }
        // redraw
        igbService.getSeqMap().updateWidget();
    }

    private void addIntron(SimpleSeqSpan seqSpan, GlyphI glyph, StyledGlyph.Direction direction) {
        Set<GlyphI> glyphs = intronSpan2Glyphs.get(direction).get(seqSpan);
        if (glyphs == null) {
            glyphs = new HashSet<>();
            intronSpan2Glyphs.get(direction).put(seqSpan, glyphs);
        }
        glyphs.add(glyph);
        maxCount = Math.max(maxCount, glyphs.size());
    }

    private void processGlyph(GlyphI glyph) {
        if (!(glyph.getInfo() instanceof BAMSym)) {
            return;
        }
        BAMSym bs = (BAMSym) glyph.getInfo();
        Cigar cg = bs.getCigar();
        int offset = bs.isForward() ? bs.getStart() : bs.getEnd();
        for (CigarElement ce : cg.getCigarElements()) {
            if (ce.getOperator() == CigarOperator.SOFT_CLIP) {
                continue;
            }
            int endOffset = offset + ce.getLength();
            if (ce.getOperator() == CigarOperator.SKIPPED_REGION) {
                SimpleSeqSpan span = new SimpleSeqSpan(Math.min(offset, endOffset), Math.max(offset, endOffset), bs.getBioSeq());
                addIntron(span, glyph, bs.isForward() ? StyledGlyph.Direction.FORWARD : StyledGlyph.Direction.REVERSE);
            }
            offset = endOffset;
        }
    }

    private void updateDisplay() {
        displayIsoforms(igbService.getAllTierGlyphs());
    }

    @Override
    public void seqSelectionChanged(SeqSelectionEvent evt) {
        updateDisplay();
    }

    @Override
    public void mapRefresh() {
        updateDisplay();
    }

//// highlight source cigar glyphs
    private void processEvent(MouseEvent evt) {
        if (!(evt instanceof NeoMouseEvent)) {
            return;
        }
        NeoMouseEvent e = (NeoMouseEvent) evt;
        List<ExonConnectorGlyph> selectedEcgs = new ArrayList<>();
        for (TierGlyph refSeqTier : refSeqTiers) {
            if (refSeqTier != null && refSeqTier.getChildren() != null) {
                for (GlyphI glyph : refSeqTier.getChildren()) {
                    if (glyph instanceof ExonConnectorGlyph) {
                        ExonConnectorGlyph ecg = (ExonConnectorGlyph) glyph;
                        boolean wasSelected = ecg.isSelected();
                        boolean isSelected = ecg.checkClicked(e.getPoint2D());
                        if (wasSelected != isSelected) {
                            ecg.setSelected(isSelected);
                            ecg.applyColorChange();
                        }
                        if (isSelected) {
                            selectedEcgs.add(ecg);
                        }
                    }
                }
            }
        }
        if (selectedEcgs.size() > 0) {
            for (ExonConnectorGlyph ecg : selectedEcgs) {
                for (GlyphI intronGlyph : ecg.getIntronGlyphs()) {
                    intronGlyph.setSelected(true);
                }
            }
        }
        igbService.getSeqMap().updateWidget();
    }

    // MouseListener
    @Override
    public void mousePressed(MouseEvent evt) {
        processEvent(evt);
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
    }

    @Override
    public void mouseReleased(MouseEvent evt) {
    }

    @Override
    public void mouseEntered(MouseEvent evt) {
    }

    @Override
    public void mouseExited(MouseEvent evt) {
    }

    @Override
    public void mouseDragged(MouseEvent evt) {
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithPropsWithCdsSpan;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.view.factories.AnnotationGlyphFactory;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tarun
 */
public class SeqMapViewUtils {

    public static void splitGraph(GraphGlyph glyph) {
        GraphSym gsym = (GraphSym) glyph.getInfo();
        GraphState gstate = gsym.getGraphState();
        if (gstate.getComboStyle() != null) {
            gstate.getTierStyle().setY(gstate.getComboStyle().getY());
        }
        gstate.setComboStyle(null, 0);
        gstate.getTierStyle().setJoin(false);
        gstate.getTierStyle().setFloatTier(false);
    }

    public void updateEnd(int end, SeqSymmetry sym, SeqMapView seqMapView) {
        GlyphI glyph = seqMapView.getSeqMap().getItemFromTier(sym);
        Rectangle2D.Double originalCoordBox = glyph.getCoordBox();
        Rectangle2D.Double coordBox = glyph.getCoordBox();
        int start = (int) coordBox.x;
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        glyph.setCoords(min, coordBox.y, max - min, coordBox.height);
        updateSpan(glyph, sym, seqMapView);
        for (int i = 0; i < sym.getChildCount(); i++) {
            SeqSymmetry child = sym.getChild(i);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            if (child != null && coordBox.x + coordBox.width > end) {
                start = (int) coordBox.x;
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
            }
        }
        if (sym instanceof SimpleSymWithPropsWithCdsSpan) {
            SeqSpan span = ((SimpleSymWithPropsWithCdsSpan) sym).getCdsSpan();
            if (end < span.getMax()) {
                updateCdsEnd(end, sym, false, seqMapView);
            }
        }
        if (sym instanceof CdsSeqSymmetry) {
            SeqSymmetry parentSym = (SeqSymmetry) glyph.getParent().getInfo();
            SeqSymmetry child = parentSym.getChild(0);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            if (child != null && coordBox.intersects(originalCoordBox)) {
                start = (int) coordBox.x;
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
            }
            child = parentSym.getChild(parentSym.getChildCount() - 1);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            if (child != null && coordBox.intersects(originalCoordBox)) {
                int cdsEnd = end;
                end = (int) (coordBox.x + coordBox.width);
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
                updateCdsEnd(cdsEnd, parentSym, false, seqMapView);
            }
        }
        seqMapView.getSeqMap().updateWidget();
    }

    public void updateCdsStart(int start, SeqSymmetry sym, boolean select, SeqMapView seqMapView) {
        if (sym instanceof SimpleSymWithPropsWithCdsSpan) {
            SeqSpan cdsSpan = ((SimpleSymWithPropsWithCdsSpan) sym).getCdsSpan();
            cdsSpan = new SimpleSeqSpan(start, cdsSpan.getEnd(), cdsSpan.getBioSeq());
            ((SimpleSymWithPropsWithCdsSpan) sym).setCdsSpan(cdsSpan);
        }
        removeSym(sym, seqMapView);
        (new AnnotationGlyphFactory()).createGlyph(sym, seqMapView);
        seqMapView.getSeqMap().repackTheTiers(true, true);
        if (select) {
            List<SeqSymmetry> selections = new ArrayList<>();
            selections.add(sym);
            seqMapView.select(selections, true);
        }
    }

    public void updateCdsEnd(int end, SeqSymmetry sym, boolean select, SeqMapView seqMapView) {
        if (sym instanceof SimpleSymWithPropsWithCdsSpan) {
            SeqSpan cdsSpan = ((SimpleSymWithPropsWithCdsSpan) sym).getCdsSpan();
            cdsSpan = new SimpleSeqSpan(cdsSpan.getStart(), end, cdsSpan.getBioSeq());
            ((SimpleSymWithPropsWithCdsSpan) sym).setCdsSpan(cdsSpan);
        }
        removeSym(sym, seqMapView);
        (new AnnotationGlyphFactory()).createGlyph(sym, seqMapView);
        seqMapView.getSeqMap().repackTheTiers(true, true);
        if (select) {
            List<SeqSymmetry> selections = new ArrayList<>();
            selections.add(sym);
            seqMapView.select(selections, true);
        }
    }

    public void updateStart(int start, SeqSymmetry sym, SeqMapView seqMapView) {
        GlyphI glyph = seqMapView.getSeqMap().getItemFromTier(sym);
        Rectangle2D.Double originalCoordBox = glyph.getCoordBox();
        Rectangle2D.Double coordBox = glyph.getCoordBox();
        int end = (int) (coordBox.x + coordBox.width);
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        glyph.setCoords(min, coordBox.y, max - min, coordBox.height);
        updateSpan(glyph, sym, seqMapView);
        for (int i = 0; i < sym.getChildCount(); i++) {
            SeqSymmetry child = sym.getChild(i);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            if (child != null && start > coordBox.x) {
                end = (int) (coordBox.x + coordBox.width);
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
            }
        }
        if (sym instanceof SimpleSymWithPropsWithCdsSpan) {
            SeqSpan span = ((SimpleSymWithPropsWithCdsSpan) sym).getCdsSpan();
            if (start > span.getMin()) {
                updateCdsStart(start, sym, false, seqMapView);
            }
        }
        if (sym instanceof CdsSeqSymmetry) {
            SeqSymmetry parentSym = (SeqSymmetry) glyph.getParent().getInfo();
            SeqSymmetry child = parentSym.getChild(0);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            boolean checkCdsStart = false;
            int cdsStart = start;
            if (child != null && coordBox.intersects(originalCoordBox)) {
                checkCdsStart = true;
                start = (int) coordBox.x;
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
            }
            child = parentSym.getChild(parentSym.getChildCount() - 1);
            glyph = seqMapView.getSeqMap().getItemFromTier(child);
            coordBox = glyph.getCoordBox();
            if (child != null && coordBox.intersects(originalCoordBox)) {
                end = (int) (coordBox.x + coordBox.width);
                glyph.setCoords(start, coordBox.y, end - start, coordBox.height);
                updateSpan(glyph, child, seqMapView);
            }
            if (checkCdsStart) {
                updateCdsStart(cdsStart, parentSym, false, seqMapView);
            }
        }
        seqMapView.getSeqMap().updateWidget();
    }

    private void updateSpan(GlyphI glyph, SeqSymmetry sym, SeqMapView seqMapView) {
        if (sym instanceof MutableSeqSymmetry) {
            SeqSpan span = sym.getSpan(seqMapView.getAnnotatedSeq());
            ((MutableSeqSymmetry) sym).removeSpan(span);
            if (span.isForward()) {
                span = new SimpleSeqSpan((int) glyph.getCoordBox().x, (int) (glyph.getCoordBox().x + glyph.getCoordBox().width), seqMapView.getAnnotatedSeq());
            } else {
                span = new SimpleSeqSpan((int) (glyph.getCoordBox().x + glyph.getCoordBox().width), (int) glyph.getCoordBox().x, seqMapView.getAnnotatedSeq());
            }
            ((MutableSeqSymmetry) sym).addSpan(span);
        }
    }

    public GlyphI removeSym(SeqSymmetry sym, SeqMapView seqMapView) {
        GlyphI glyph = seqMapView.getSeqMap().getItemFromTier(sym);
        if (!(glyph.getParent() instanceof TierGlyph)) {
            SeqSymmetry parentSym = (SeqSymmetry) glyph.getParent().getInfo();
            if (parentSym instanceof MutableSeqSymmetry) {
                ((MutableSeqSymmetry) parentSym).removeChild(sym);
            }
        }
        seqMapView.getSeqMap().removeItem(glyph);
        seqMapView.getSeqMap().updateWidget();
        return glyph;
    }

    public void showProperties(int x, GraphGlyph glyph, SeqMapView seqMapView) {
        List<GraphGlyph> glyphs = new ArrayList<>();
        glyphs.add(glyph);
        List<SeqSymmetry> sym = SeqMapView.glyphsToSyms(glyphs);
        if (!sym.isEmpty()) {
            if (seqMapView.propertyHandler != null) {
                seqMapView.propertyHandler.showGraphProperties((GraphSym) sym.get(0), x, seqMapView);
            }
        }
    }
}

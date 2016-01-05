package com.gene.findannotations;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.SwingWorker;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.lorainelab.igb.services.IgbService;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

public class FindAnnotationsSelectListener implements MouseListener {

    private static final int MAX_CHILDREN = 1000;
    private final JTable table;
    private final IgbService igbService;

    public FindAnnotationsSelectListener(JTable table, IgbService igbService) {
        super();
        this.table = table;
        this.igbService = igbService;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getComponent().isEnabled()
                && e.getButton() == MouseEvent.BUTTON1
                && e.getClickCount() == 2) {
            int srow = table.getSelectedRow();
            List<SeqSymmetry> results = ((AnnotationsTableModel) table.getModel()).getResults();
            if (results == null || srow < 0 || srow >= results.size()) {
                return;
            }
            final SeqSymmetry sym = results.get(srow);
            // Set selected symmetry normally
            final SeqSpan span = sym.getSpan(0);
            igbService.zoomToCoord(span.getBioSeq().toString(), span.getMin(), span.getMax());
            GenericAction action = GenericActionHolder.getInstance().getGenericAction("RefreshDataAction");
            if (action != null) {
                action.actionPerformed(null);
            }
            new SwingWorker<List<SeqSymmetry>, Void>() {
                @Override
                protected List<SeqSymmetry> doInBackground() throws Exception {
                    List<SeqSymmetry> syms = new ArrayList<>();
                    for (TierGlyph tierGlyph : igbService.getAllTierGlyphs()) {
                        if (tierGlyph.isVisible()) {
                            for (GlyphI glyphAtSpan : getGlyphsAtSpan(tierGlyph, span)) {
                                SeqSymmetry symAtSpan = (SeqSymmetry) glyphAtSpan.getInfo();
                                if ((sym.getID() == null && symAtSpan.getID() == null) || (sym.getID() != null && sym.getID().toLowerCase().equals(symAtSpan.getID().toLowerCase()))) {
                                    syms.add(symAtSpan);
                                }
                            }
                        }
                    }
                    return syms;
                }

                public void done() {
                    try {
                        igbService.getSeqMapView().select(get(), true);
                    } catch (Exception x) {
                        Logger.getLogger(FindAnnotationsSelectListener.class.getName()).log(Level.SEVERE, "cannot process glyphs", x);
                    }
                }
            }.execute();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    /*
     private boolean equalSym(GlyphI glyph1, GlyphI glyph2) {
     if (glyph1.getInfo() == null || glyph2.getInfo() == null ||
     !(glyph1.getInfo() instanceof SeqSymmetry) || !(glyph2.getInfo() instanceof SeqSymmetry)) {
     return false;
     }
     SeqSymmetry sym1 = (SeqSymmetry)glyph1.getInfo();
     SeqSymmetry sym2 = (SeqSymmetry)glyph2.getInfo();
     if (sym1.getSpanCount() == 0 || sym2.getSpanCount() == 0) {
     return false;
     }
     SeqSpan span1 = sym1.getSpan(0);
     SeqSpan span2 = sym2.getSpan(0);
     if (!span1.getBioSeq().toString().equals(span2.getBioSeq().toString()) || span1.getStart() != span2.getStart() || span1.getEnd() != span2.getEnd()) {
     return false;
     }
     return true;
     }
     */
    private boolean glyphMatchesSpan(GlyphI glyph, SeqSpan span) {
        Object obj = glyph.getInfo();
        if (obj instanceof SeqSymmetry && ((SeqSymmetry) obj).getSpanCount() > 0) {
            SeqSpan span2 = ((SeqSymmetry) obj).getSpan(0);
            return span.getBioSeq().toString().equals(span2.getBioSeq().toString())
                    && span.getMin() == span2.getMin() && span.getMax() == span2.getMax();
        }
        return false;
    }

    public List<GlyphI> getGlyphsAtSpan(GlyphI glyph, SeqSpan span) {
        List<GlyphI> glyphsAtSpan = new ArrayList<>();
        if (glyphMatchesSpan(glyph, span)) {
            glyphsAtSpan.add(glyph);
        }
        if (glyph.getChildCount() > 0) {
            if (glyph.getChildCount() < MAX_CHILDREN) {
                for (GlyphI childGlyph : glyph.getChildren()) {
                    glyphsAtSpan.addAll(getGlyphsAtSpan(childGlyph, span));
                }
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Glyph has too many children {0}", glyph.getChildCount());
            }
        }
        return glyphsAtSpan;
    }

}

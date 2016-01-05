/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.color.ColorScheme;
import com.affymetrix.genoviz.color.ColorSchemeComboBox;
import com.affymetrix.igb.action.SeqMapViewActionA;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * For setting a particular color scheme. Usage: <blockquote><pre><code>
 * {@link ColorSchemeComboBox} b = new ColorSchemeComboBox(); Action a = new
 * <strong>ColorSchemeAction</strong>(); b.addActionListener(a);
 * affyLabelledTierMap.addSelectionListener(b); panel.add(b);
 * </code></pre></blockquote>
 *
 * @author blossome
 */
public class ColorSchemeAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static ColorSchemeAction ACTION;

    /**
     * Get an (the) instance of a chooser.
     *
     * @return the same static instance every time.
     */
    public static ColorSchemeAction getAction() {
        if (ACTION == null) {
            ACTION = new ColorSchemeAction();
        }
        return ACTION;
    }
    private AffyLabelledTierMap ltm = null;

    /**
     * Create a default action for setting a color scheme. The icons are by Mark
     * James of <a href="http://www.famfamfam.com/about/">Fam Fam Fam</a>.
     */
    public ColorSchemeAction() {
        super("Color Scheme",
                "16x16/actions/colorschemechooser.png",
                "22x22/actions/colorschemechooser.png");
        AffyTieredMap m = this.getSeqMapView().getSeqMap();
        if (m instanceof AffyLabelledTierMap) {
            this.ltm = (AffyLabelledTierMap) m;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o instanceof ColorScheme) {
            ColorScheme s = (ColorScheme) o;
            Color bg = Color.decode(s.getBackground());
            List<TierLabelGlyph> l = ltm.getOrderedTierLabels();
            int j = 0;
            int colors = s.numberOfForegrounds();
            for (TierLabelGlyph tlg : l) {
                TierGlyph g = tlg.getReferenceTier();
                if (tlg.isSelected()) {
                    Color c = Color.decode(s.getForeground(j));
                    g.getAnnotStyle().setBackground(bg);
                    g.getAnnotStyle().setForeground(c);
                    j = (j + 1) % colors;
                }
            }
            refreshMap(false, false);
        }
    }

    public void tempAction(ColorScheme s) {
        Color bg = Color.decode(s.getBackground());
        List<TierLabelGlyph> l = ltm.getOrderedTierLabels();
        int j = 0;
        int colors = s.numberOfForegrounds();
        for (TierLabelGlyph tlg : l) {
            TierGlyph g = tlg.getReferenceTier();
            if (tlg.isSelected()) {
                Color c = Color.decode(s.getForeground(j));
                g.getAnnotStyle().setBackground(bg);
                g.getAnnotStyle().setForeground(c);
                j = (j + 1) % colors;
            }
        }
        refreshMap(false, false);
    }
}

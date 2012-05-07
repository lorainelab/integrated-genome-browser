/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.igb.action;

import com.affymetrix.igb.color.ColorScheme;
import com.affymetrix.igb.color.ColorSchemeComboBox;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * For setting a particular color scheme.
 * Usage: <blockquote><code>
 * {@link ColorSchemeComboBox} b = new ColorSchemeComboBox();<br>
 * Action a = new <strong>ColorSchemeAction</strong>();<br>
 * b.addActionListener(a);<br>
 * affyLabelledTierMap.addSelectionListener(b);<br>
 * b.setActionListener(a);<br>
 * panel.add(b);
 * </code></blockquote>
 * @author blossome
 */
public class ColorSchemeAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private AffyLabelledTierMap ltm = null;

	/**
	 * Create a default action for setting a color scheme.
	 * The icons are by Mark James
	 * of <a href="http://www.famfamfam.com/about/">Fam Fam Fam</a>.
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
			for (int i = 0; i < l.size(); i++) {
				TierLabelGlyph tlg = l.get(i);
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

}

/**
 *   Copyright (c) 2001-2005 Affymetrix, Inc.
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
package com.affymetrix.igb.glyph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ColorUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;

public final class EdgeMatchAdjuster extends JPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;

	private SeqMapView gviewer;
	private final JSlider tslider;
	private final JRPTextField text;
	private static final int thresh_min = 0;
	private static final int thresh_max = 100;
	private int prev_thresh;
	private static final int min_xpix = 48;
	private static final int min_ypix = 15;
	private static final int max_xpix = 60;
	private static final int max_ypix = 35;
	private static EdgeMatchAdjuster singleton_adjuster = null;
	private static JFrame singleton_frame = null;
			JPanel adjustFuzziness;
		JPanel textP;
		JPanel sliderP;

	public static EdgeMatchAdjuster showFramedThresholder(
			GlyphEdgeMatcher matcher, SeqMapView view) {
		if (singleton_adjuster == null) {
			singleton_adjuster = new EdgeMatchAdjuster(matcher, view);
			singleton_frame = new JFrame("Adjust Edge Match Fuzziness");
			Image icon = Application.getSingleton().getIcon();
			if (icon != null) {
				singleton_frame.setIconImage(icon);
			}
			Container cpane = singleton_frame.getContentPane();
			cpane.setLayout(new BorderLayout());
			cpane.add("Center", singleton_adjuster);

			singleton_frame.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent evt) {
					Window w = evt.getWindow();
					w.setVisible(false);
					w.dispose();
				}
			});
		}

		singleton_adjuster.gviewer = view; // in case the SeqMapView isn't the same
		singleton_frame.pack();
		singleton_frame.setState(Frame.NORMAL);
		singleton_frame.toFront();
		singleton_frame.setVisible(true);
		return singleton_adjuster;
	}

	private EdgeMatchAdjuster(GlyphEdgeMatcher matcher, SeqMapView view) {
		gviewer = view;
		prev_thresh = (int) matcher.getFuzziness();
		adjustFuzziness = new JPanel();
		textP = new JPanel();
		sliderP = new JPanel();
		textP.setLayout((new BoxLayout(textP, BoxLayout.Y_AXIS)));
		sliderP.setLayout((new BoxLayout(sliderP, BoxLayout.Y_AXIS)));
//		adjustFuzziness.setLayout((new GridLayout(1, 2)));
		text = new JRPTextField("EdgeMatchAdjuster_text", 3);
		text.setAlignmentY(SwingConstants.CENTER);
		text.setMinimumSize(new Dimension(min_xpix, min_ypix));
		text.setMaximumSize(new Dimension(max_xpix, max_ypix));
		text.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent ke) {
				String typed = text.getText();
				tslider.setValue(0);
				if (!typed.matches("\\d+") || typed.length() > 3) {
					return;
				}
				int value = Integer.parseInt(typed);
				tslider.setValue(value);
			}
		});

		tslider = new JSlider(JSlider.HORIZONTAL,
				thresh_min, thresh_max, prev_thresh);
		tslider.setMinorTickSpacing(2);
		tslider.setMajorTickSpacing(10);
		tslider.setPaintTicks(true);
		tslider.setPaintLabels(true);
		tslider.addChangeListener(this);
		tslider.setPreferredSize(new Dimension(400, 70));
		sliderP.add(tslider);
		textP.add(text);
		adjustFuzziness.add(textP);
		adjustFuzziness.add(sliderP);
		this.setLayout(new BorderLayout());
		this.add("Center", adjustFuzziness);
		this.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e){
            }

            @Override
            public void focusGained(FocusEvent e) {
                text.setText(String.valueOf(tslider.getValue()));
            }
        });
		JPanel edge_match_box = new JPanel();
		edge_match_box.setLayout(new GridLayout(2, 0));
		edge_match_box.setBorder(new javax.swing.border.TitledBorder("Edge match colors"));

		edge_match_box.add(addColorChooser("Standard", SeqMapView.PREF_EDGE_MATCH_COLOR, SeqMapView.default_edge_match_color));
		edge_match_box.add(addColorChooser("Fuzzy matching", SeqMapView.PREF_EDGE_MATCH_FUZZY_COLOR, SeqMapView.default_edge_match_fuzzy_color));
		this.add("South", edge_match_box);
		this.revalidate();
	}

	private static JPanel addColorChooser(String label_str, String pref_name, Color default_color) {
		JComponent component = ColorUtils.createColorComboBox(pref_name, default_color, null);

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		JPanel inner_panel = new JPanel();

		inner_panel.add(component);
		panel.add(new JLabel(label_str + ": "));
		panel.add(inner_panel);

		return panel;
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src == tslider) {
			// EdgeMatching can be very slow, so don't redo it until user stops sliding the slider
			int current_thresh = tslider.getValue();
			if (current_thresh != prev_thresh) {
				gviewer.adjustEdgeMatching(current_thresh);
				prev_thresh = current_thresh;
				text.setText(String.valueOf(tslider.getValue()));
			}
		}
	}
}

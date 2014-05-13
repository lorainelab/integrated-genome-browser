/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import static com.affymetrix.igb.shared.Selections.*;

/**
 *
 * @author dcnorris
 */
public class ChangeTierLabelColorActionA extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	protected ChangeTierLabelColorActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected void changeColor(final boolean fg) {
		if (allStyles.isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();
		ITrackStyleExtended style_0 = allStyles.get(0);
		if (style_0 != null) {
			if (fg) {
				chooser.setColor(style_0.getLabelForeground());
			} else {
				chooser.setColor(style_0.getLabelBackground());
			}
		}
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (ITrackStyleExtended style : allStyles) {
					if (style != null) {
						if (fg) {
							style.setLabelForeground(chooser.getColor());
						} else {
							style.setLabelBackground(chooser.getColor());
						}
					}
				}
			}			
		};

		JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
				"Pick a Color",
				true, //modal
				chooser,
				al, //OK button handler
				null); //no CANCEL button handler

		dialog.setVisible(true);
		getSeqMapView().getSeqMap().updateWidget();
	}
}

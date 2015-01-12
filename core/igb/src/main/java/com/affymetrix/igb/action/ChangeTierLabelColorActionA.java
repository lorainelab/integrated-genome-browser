/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import static com.affymetrix.igb.shared.Selections.allStyles;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JColorChooser;
import javax.swing.JDialog;

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
        ActionListener al = e -> {
            allStyles.stream().filter(style -> style != null).forEach(style -> {
                if (fg) {
                    style.setLabelForeground(chooser.getColor());
                } else {
                    style.setLabelBackground(chooser.getColor());
                }
            });
        };

        JDialog dialog = JColorChooser.createDialog(null, // parent
                "Pick a Color",
                true, //modal
                chooser,
                al, //OK button handler
                null); //no CANCEL button handler

        dialog.setVisible(true);
        getSeqMapView().getSeqMap().updateWidget();
    }
}

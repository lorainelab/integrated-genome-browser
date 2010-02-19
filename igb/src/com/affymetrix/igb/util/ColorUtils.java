package com.affymetrix.igb.util;

import com.affymetrix.genoviz.swing.ColorIcon;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JColorChooser;

/**
 *
 * @version $Id$
 */
public class ColorUtils {

	/**
	 *  Creates a JButton associated with a Color preference.
	 *  Will initialize itself with the value of the given
	 *  preference and will update itself, via a PreferenceChangeListener,
	 *  if the preference value changes.
	 * 
	 * @param node
	 * @param pref_name 
	 * @param default_val 
	 * @return
	 */
	public static JButton createColorButton(final Preferences node,
			final String pref_name, final Color default_val) {

		Color initial_color = UnibrowPrefsUtil.getColor(node, pref_name, default_val);
		final ColorIcon icon = new ColorIcon(11, initial_color);
		final String panel_title = "Choose a color";

		final JButton button = new JButton(icon);
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				Color c = JColorChooser.showDialog(button, panel_title, UnibrowPrefsUtil.getColor(node, pref_name, default_val));
				if (c != null) {
					UnibrowPrefsUtil.putColor(node, pref_name, c);
				}
			}
		});
		node.addPreferenceChangeListener(new PreferenceChangeListener() {

			public void preferenceChange(PreferenceChangeEvent evt) {
				if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
					Color c = UnibrowPrefsUtil.getColor(node, pref_name, default_val);
					icon.setColor(c);
					button.repaint();
				}
			}
		});
		return button;
	}
}

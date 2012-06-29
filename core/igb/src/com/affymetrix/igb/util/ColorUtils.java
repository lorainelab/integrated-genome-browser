package com.affymetrix.igb.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;

import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.jidesoft.combobox.ColorComboBox;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

/**
 *
 * @version $Id: ColorUtils.java 8371 2011-06-29 19:55:31Z dcnorris $
 */
public class ColorUtils {
	
	/**
	 *  Creates a Color chooser combo box associated with a Color preference.
	 *  Will initialize itself with the value of the given
	 *  preference and will update itself, via a PreferenceChangeListener,
	 *  if the preference value changes.
	 */	

	public static ColorComboBox createColorComboBox(final Preferences node,
		final String pref_name, final Color default_val, final PreferenceChangeListener listener){
		Color initial_color = PreferenceUtils.getColor(node, pref_name, default_val);
		final ColorComboBox combobox = new ColorComboBox();
		combobox.setSelectedColor(initial_color);
		combobox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
		combobox.setColorValueVisible(false);
		combobox.setCrossBackGroundStyle(false);
		combobox.setButtonVisible(false);
		combobox.setStretchToFit(true);
		combobox.setMaximumSize(new Dimension(150,20));
		combobox.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				Color c = combobox.getSelectedColor();
				if (c != null) {
					PreferenceUtils.putColor(node, pref_name, c);
				}else{
					combobox.setSelectedColor(PreferenceUtils.getColor(node, pref_name, default_val));
				}
			}
		});

		node.addPreferenceChangeListener(new PreferenceChangeListener() {

			public void preferenceChange(PreferenceChangeEvent evt) {
				if (listener != null && evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
					listener.preferenceChange(evt);
				}
			}
		});
		

		return combobox;
	}
}

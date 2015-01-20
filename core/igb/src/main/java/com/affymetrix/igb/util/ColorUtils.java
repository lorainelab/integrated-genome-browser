package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.jidesoft.combobox.ColorComboBox;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
/**
 *
 * @version $Id: ColorUtils.java 8371 2011-06-29 19:55:31Z dcnorris $
 */
public class ColorUtils {
	private static Map<String, Color> colorMap = new HashMap<>();
	
	/**
	 *  Creates a Color chooser combo box associated with a Color preference.
	 *  Will initialize itself with the value of the given
	 *  preference and will update itself, via a PreferenceChangeListener,
	 *  if the preference value changes.
	 */	

	public static ColorComboBox createColorComboBox(final String pref_name, final Color default_val, final PreferenceChangeListener listener){
		final Preferences node = PreferenceUtils.getTopNode();
		final Color initial_color = PreferenceUtils.getColor(pref_name, default_val);
		final ColorComboBox combobox = new ColorComboBox();
		combobox.setSelectedColor(initial_color);
		combobox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 1, true));
		combobox.setColorValueVisible(false);
		combobox.setCrossBackGroundStyle(false);
		combobox.setButtonVisible(false);
		combobox.setStretchToFit(true);
		combobox.setMaximumSize(new Dimension(150,20));
		combobox.addItemListener(e -> {
            Color c = combobox.getSelectedColor();
            if (c != null) {
                PreferenceUtils.putColor(node, pref_name, c);
            }else{
                combobox.setSelectedColor(PreferenceUtils.getColor(pref_name, default_val));
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
	
	public static Color getColor(String str) {
		Color c = colorMap.get(str);
		if (c == null) {
			try {
				if (str.contains(",")) {
					String[] rgb = str.split(",");
					c = new Color(Integer.valueOf(rgb[0].trim()), Integer.valueOf(rgb[1].trim()), Integer.valueOf(rgb[2].trim()));
				} else {
					if(str.startsWith("0x")){
						c = Color.decode(str.trim());
					}else {
						c = Color.decode("0x" + str.trim());
					}
				}
				colorMap.put(str, c);
			} catch (Exception e) {
				c = null;
			}
		}
		return c;
	}
	
	public static Color getAlternateColor(Color color) {
		Color altColor;
		int intensity = color.getRed() + color.getGreen() + color.getBlue();
		if (intensity == 0) {
			altColor = Color.darkGray;
		} else if (intensity > (255 + 127)) {
			altColor = color.darker();
		} else if (color.getRed() == 255 || color.getGreen() == 0 || color.getBlue() == 0){
			altColor = color.darker();
		} else if (color.getRed() == 0 || color.getGreen() == 255 || color.getBlue() == 0){
			altColor = color.darker();
		} else if (color.getRed() == 0 || color.getGreen() == 0 || color.getBlue() == 255){
			altColor = color.darker();
		} else {
			altColor = color.brighter();
		}
		return altColor;
	}
}

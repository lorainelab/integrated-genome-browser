/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometryImpl.color.ColorProvider;
import com.affymetrix.genometryImpl.color.RGB;
import com.affymetrix.genometryImpl.color.Score;
import com.affymetrix.genometryImpl.color.Strand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class ColorProviderHolder {
	public static final Map<String, Class<? extends ColorProvider>> OPTIONS;
	static {
		OPTIONS = new LinkedHashMap<String, Class<? extends ColorProvider>>();
		OPTIONS.put("None", null);
		OPTIONS.put("RGB", RGB.class);
		OPTIONS.put("Score", Score.class);
		OPTIONS.put("Strand", Strand.class);
		OPTIONS.put("Property", ColorByProperty.class);
	}
	
	public static ColorProvider getCPInstance(Class<? extends ColorProvider> clazz) {
		try {
			if (clazz != null) {
				return clazz.getConstructor().newInstance();
			}
		} catch (Exception ex) {
		}
		return null;
	}

	public static String getCPName(Class<? extends ColorProvider> clazz) {
		for (Entry<String, Class<? extends ColorProvider>> entry : OPTIONS.entrySet()) {
			if (entry.getValue() == clazz) {
				return entry.getKey();
			}
		}
		return null;
	}
	
}

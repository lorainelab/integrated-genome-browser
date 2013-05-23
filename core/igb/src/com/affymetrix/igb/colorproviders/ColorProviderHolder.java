package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometryImpl.color.ColorProviderI;
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
	public static final Map<String, Class<? extends ColorProviderI>> OPTIONS;
	static {
		OPTIONS = new LinkedHashMap<String, Class<? extends ColorProviderI>>();
		OPTIONS.put("None", null);
		OPTIONS.put("RGB", RGB.class);
		OPTIONS.put("Score", Score.class);
		//OPTIONS.put("Strand", Strand.class);
		OPTIONS.put("Length", Length.class);
		OPTIONS.put("Property", Property.class);
	}
	
	public static ColorProviderI getCPInstance(Class<? extends ColorProviderI> clazz) {
		try {
			if (clazz != null) {
				return clazz.getConstructor().newInstance();
			}
		} catch (Exception ex) {
		}
		return null;
	}

	public static String getCPName(Class<? extends ColorProviderI> clazz) {
		for (Entry<String, Class<? extends ColorProviderI>> entry : OPTIONS.entrySet()) {
			if (entry.getValue() == clazz) {
				return entry.getKey();
			}
		}
		return null;
	}
	
}

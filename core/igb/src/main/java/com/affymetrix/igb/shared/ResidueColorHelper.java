package com.affymetrix.igb.shared;

import com.affymetrix.genometry.util.PreferenceUtils;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

/**
 *
 * @author hiralv
 */
public class ResidueColorHelper implements PreferenceChangeListener {

    private static Map<String, Color> DEFAULT_COLORS;

    public static final String PREF_A_COLOR = "Adenine color";
    public static final String PREF_T_COLOR = "Thymine color";
    public static final String PREF_G_COLOR = "Guanine color";
    public static final String PREF_C_COLOR = "Cytosine color";
    public static final String PREF_OTHER_COLOR = "Other color";
    public static final Color DEFAULT_A_COLOR = new Color(151, 255, 179);
    public static final Color DEFAULT_T_COLOR = new Color(102, 211, 255);
    public static final Color DEFAULT_G_COLOR = new Color(255, 210, 0);
    public static final Color DEFAULT_C_COLOR = new Color(255, 176, 102);
    public static final Color DEFAULT_OTHER_COLOR = Color.LIGHT_GRAY;
    private static final ResidueColorHelper singleton = new ResidueColorHelper();

    public static ResidueColorHelper getColorHelper() {
        return singleton;
    }

    private final Color[] colors;

    public ResidueColorHelper() {
        initializeDefaultColors();
        int i = 0;
        colors = new Color[5];
        PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
        for (Map.Entry<String, Color> entry : DEFAULT_COLORS.entrySet()) {
            colors[i] = PreferenceUtils.getColor(entry.getKey(), entry.getValue());
            i++;
        }
    }

    private static void initializeDefaultColors() {
        Map<String, Color> defaultColors = new LinkedHashMap<>();

        defaultColors.put(PREF_A_COLOR, DEFAULT_A_COLOR);
        defaultColors.put(PREF_T_COLOR, DEFAULT_T_COLOR);
        defaultColors.put(PREF_G_COLOR, DEFAULT_G_COLOR);
        defaultColors.put(PREF_C_COLOR, DEFAULT_C_COLOR);
        defaultColors.put(PREF_OTHER_COLOR, DEFAULT_OTHER_COLOR);

        DEFAULT_COLORS = Collections.unmodifiableMap(defaultColors);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        int i = 0;
        for (Map.Entry<String, Color> entry : DEFAULT_COLORS.entrySet()) {
            if (entry.getKey().equals(evt.getKey())) {
                colors[i] = PreferenceUtils.getColor(entry.getKey(), entry.getValue());
                break;
            }
            i++;
        }
    }

    public Color determineResidueColor(char charAt) {
        switch (charAt) {
            case 'A':
            case 'a':
                return colors[0];
            case 'T':
            case 't':
                return colors[1];
            case 'G':
            case 'g':
                return colors[2];
            case 'C':
            case 'c':
                return colors[3];
            default:
                return colors[4];
        }
    }

}

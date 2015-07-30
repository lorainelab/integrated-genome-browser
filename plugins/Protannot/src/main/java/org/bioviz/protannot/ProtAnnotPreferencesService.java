/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.AminoAcid;
import com.affymetrix.igb.shared.ResidueColorService;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
@Component(provide = ProtAnnotPreferencesService.class)
public class ProtAnnotPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(ProtAnnotPreferencesService.class);

    private ResidueColorService residueColorService;

    private Preferences prefs;

    private static final Color defaultColor = Color.gray;

    private Map<String, Color> uncommitted;

    @Activate
    public void activate() {
        prefs = PreferenceUtils.getProtAnnotNode();
    }

    @Reference
    public void setResidueColorService(ResidueColorService residueColorService) {
        this.residueColorService = residueColorService;
    }

    public int getResidueRGB(AminoAcid aminoAcid) {
        try {
            if (uncommitted != null && uncommitted.containsKey(getResidueLabelByAminoAcid(aminoAcid))) {
                return uncommitted.get(getResidueLabelByAminoAcid(aminoAcid)).getRGB();
            }

            return prefs.getInt(getResidueLabelByAminoAcid(aminoAcid), residueColorService.getDefaultColor(aminoAcid).getRGB());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return defaultColor.getRGB();
    }

    public Map<String, Color> getAllColorPreferences() {
        if (uncommitted != null) {
            return uncommitted;
        }
        Map<String, Color> phash = new HashMap<>();

        try {
            COLORS.defaultColorList().entrySet().stream().forEach((color_pref) -> {
                phash.put(color_pref.getKey(), new Color(prefs.getInt(color_pref.getKey(), color_pref.getValue().getRGB())));
            });

            phash.put(ResidueName.A.toString(), new Color(prefs.getInt(ResidueName.A.toString(),
                    residueColorService.getDefaultColor(AminoAcid.Alanine).getRGB())));
            phash.put(ResidueName.T.toString(), new Color(prefs.getInt(ResidueName.T.toString(),
                    residueColorService.getDefaultColor(AminoAcid.Threonine).getRGB())));
            phash.put(ResidueName.G.toString(), new Color(prefs.getInt(ResidueName.G.toString(),
                    residueColorService.getDefaultColor(AminoAcid.Glycine).getRGB())));
            phash.put(ResidueName.C.toString(), new Color(prefs.getInt(ResidueName.C.toString(),
                    residueColorService.getDefaultColor(AminoAcid.Cysteine).getRGB())));

            updatePrefs(phash);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return phash;
    }

    public void updatePrefs(Map<String, Color> hash) {
        uncommitted = hash;
    }

    public void abort() {
        uncommitted = null;
    }

    public void reset() {
        uncommitted = null;
        COLORS.defaultColorList().entrySet().stream().forEach((color_pref) -> {
            prefs.remove(color_pref.getKey());
        });

        prefs.remove(ResidueName.A.toString());
        prefs.remove(ResidueName.T.toString());
        prefs.remove(ResidueName.G.toString());
        prefs.remove(ResidueName.C.toString());
    }

    public void commit() {
        try {
            uncommitted.entrySet().stream().forEach((entry) -> {
                prefs.putInt(entry.getKey(), entry.getValue().getRGB());
            });
            prefs.flush();
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            uncommitted = null;
        }
    }

    private String getResidueLabelByAminoAcid(AminoAcid aminoAcid) throws Exception {
        switch (aminoAcid) {
            case Alanine:
                return ResidueName.A.toString();
            case Threonine:
                return ResidueName.T.toString();
            case Glycine:
                return ResidueName.G.toString();
            case Cysteine:
                return ResidueName.C.toString();
            default:
                throw new Exception("Unknown Label");
        }
    }

    private enum ResidueName {

        A("Residue A"), T("Residue T"), G("Residue G"), C("Residue C");

        public final String name;

        private ResidueName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    //TODO: Refactor into service
    public enum COLORS {

        BACKGROUND("background", Color.white),
        FRAME0("frame0", new Color(0, 100, 145)),
        FRAME1("frame1", new Color(0, 100, 255)),
        FRAME2("frame2", new Color(192, 192, 114)),
        TRANSCRIPT("transcript", Color.black),
        DOMAIN("domain", new Color(84, 168, 132)),
        EXONSUMMARY("exonsummary", Color.blue),
        AMINOACID("amino_acid", Color.black);

        private final String name;
        private final Color color;

        COLORS(String nm, Color col) {
            this.name = nm;
            this.color = col;
        }

        @Override
        public String toString() {
            return name;
        }

        public Color defaultColor() {
            return color;
        }

        int getRGB() {
            return color.getRGB();
        }

        public static Map<String, Color> defaultColorList() {
            Map<String, Color> defaults = new HashMap<>();

            for (COLORS C : values()) {
                defaults.put(C.toString(), C.defaultColor());
            }

            return defaults;
        }
    };
}

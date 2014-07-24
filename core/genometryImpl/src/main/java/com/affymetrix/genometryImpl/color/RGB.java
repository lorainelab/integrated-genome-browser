package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class RGB extends ColorProvider {

    @Override
    public String getName() {
        return "rgb";
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym instanceof SymWithProps) {
            return (Color) ((SymWithProps) sym).getProperty(TrackLineParser.ITEM_RGB);
        }
        return null;
    }
}

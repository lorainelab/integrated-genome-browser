package com.affymetrix.genometry.color;

import com.affymetrix.genometry.parsers.TrackLineParser;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
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

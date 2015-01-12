package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.awt.Color;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 */
public class Duplicate extends ColorProvider {

    private final static String DUPLICATE_COLOR = "duplicate";
    private final static Color DEFAULT_DUPLICATE_COLOR = new Color(204, 255, 255);

    private Parameter<Color> duplicateColor = new Parameter<>(DEFAULT_DUPLICATE_COLOR);

    public Duplicate() {
        super();
        parameters.addParameter(DUPLICATE_COLOR, Color.class, duplicateColor);
    }

    @Override
    public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
        return fileTypeCategory == FileTypeCategory.Alignment;
    }

    @Override
    public String getName() {
        return "duplicate";
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym instanceof BAMSym && ((BAMSym) sym).getDuplicateReadFlag()) {
            return duplicateColor.get();
        }
        return null;
    }
}

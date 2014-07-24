package com.affymetrix.genometryImpl.color;

import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class PariedByRunNo extends ColorProvider {

    private final static String FIRST_RUN_COLOR = "first_run";
    private final static String SECOND_RUN_COLOR = "second_run";
    private final static Color DEFAULT_FIRST_RUN_COLOR = new Color(204, 255, 255);
    private final static Color DEFAULT_SECOND_RUN_COLOR = new Color(51, 255, 255);

    private Parameter<Color> firstRunColor = new Parameter<Color>(DEFAULT_FIRST_RUN_COLOR);
    private Parameter<Color> secondRunColor = new Parameter<Color>(DEFAULT_SECOND_RUN_COLOR);

    public PariedByRunNo() {
        super();
        parameters.addParameter(FIRST_RUN_COLOR, Color.class, firstRunColor);
        parameters.addParameter(SECOND_RUN_COLOR, Color.class, secondRunColor);
    }

    @Override
    public String getName() {
        return "paired_by_run_no";
    }

    @Override
    public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
        return fileTypeCategory == FileTypeCategory.Alignment;
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym instanceof BAMSym && ((BAMSym) sym).getReadPairedFlag()) {
            if (((BAMSym) sym).getFirstOfPairFlag()) {
                return firstRunColor.get();
            } else if (((BAMSym) sym).getSecondOfPairFlag()) {
                return secondRunColor.get();
            }
        }
        return null;
    }
}

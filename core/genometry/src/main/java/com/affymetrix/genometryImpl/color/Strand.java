package com.affymetrix.genometry.color;

import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.awt.Color;

/**
 *
 * @author hiralv
 */
public class Strand extends ColorProvider {

    private final static String FORWARD_COLOR = "+";
    private final static String REVERSE_COLOR = "-";
    private final static Color DEFAULT_FORWARD_COLOR = new Color(204, 255, 255);
    private final static Color DEFAULT_REVERSE_COLOR = new Color(51, 255, 255);
    private static GenometryModel model = GenometryModel.getInstance();

    private Parameter<Color> forwardColor = new Parameter<>(DEFAULT_FORWARD_COLOR);
    private Parameter<Color> reverseColor = new Parameter<>(DEFAULT_REVERSE_COLOR);

    public Strand() {
        super();
        parameters.addParameter(FORWARD_COLOR, Color.class, forwardColor);
        parameters.addParameter(REVERSE_COLOR, Color.class, reverseColor);
    }

    @Override
    public String getName() {
        return "strand";
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym.getSpan(model.getSelectedSeq()).isForward()) {
            return forwardColor.get();
        }
        return reverseColor.get();
    }
}

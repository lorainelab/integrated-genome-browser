package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometry.color.ColorProvider;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SAMTags extends ColorProvider {

    private final static String SAMTAGS = "samtags";
    private final static List<String> SAMTAGS_VALUES = new LinkedList<>();
    private final static String SAMTAGS_VALUE = "value";
    public final static String DEFAULT_SAMTAGS_VALUE = "";
    private final static String MATCH_COLOR = "match";
    private final static Color DEFAULT_MATCH_COLOR = Color.GREEN;
    private final static String NOT_MATCH_COLOR = "not_match";
    private final static Color DEFAULT_NOT_MATCH_COLOR = Color.RED;

    static {
        SAMTAGS_VALUES.add("CR");
        SAMTAGS_VALUES.add("CB");
    }
    private Float float_property_value = null;

    protected Parameter<String> property = new BoundedParameter<>(SAMTAGS_VALUES);
    protected Parameter<String> property_value = new Parameter<String>(DEFAULT_SAMTAGS_VALUE) {
        @Override
        public boolean set(Object e) {
            if (e != null) {
                try {
                    float_property_value = Float.parseFloat(e.toString());
                } catch (Exception ex) {
                }
                return super.set(e.toString().toLowerCase());
            }

            return super.set(e);
        }
    };

    private Parameter<Color> matchColor = new Parameter<>(DEFAULT_MATCH_COLOR);
    private Parameter<Color> notMatchColor = new Parameter<>(DEFAULT_NOT_MATCH_COLOR);

    //	private ColorPalette cp = new ColorPalette(ColorScheme.ACCENT8);
    public SAMTags() {
        super();
        parameters.addParameter(SAMTAGS, String.class, property);
        parameters.addParameter(SAMTAGS_VALUE, String.class, property_value);
        parameters.addParameter(MATCH_COLOR, Color.class, matchColor);
        parameters.addParameter(NOT_MATCH_COLOR, Color.class, notMatchColor);
    }

    @Override
    public String getName() {
        return "samtags";
    }

    @Override
    public String getDisplay() {
        return IGBConstants.BUNDLE.getString("color_by_samtags");
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym instanceof SymWithProps) {
            Object value = ((SymWithProps) sym).getProperty(property.get());
            if (value instanceof Float || value instanceof Double){
                if(float_property_value != null && compare(float_property_value, value))
                    return matchColor.get();
                else
                    return notMatchColor.get();
            }
            else if (value != null && property_value.get() != null
                    && value.toString().toLowerCase().contains(property_value.get())) {
                return matchColor.get();
//				return cp.getColor(value.toString());
            }
        }
        return notMatchColor.get();
    }
    public static boolean compare(Float float_property_value, Object value){
        if(value instanceof Double || value instanceof Float){
            return Float.compare(float_property_value, ((Number) value).floatValue())==0;
        }else{
            return false;
        }
    }

}

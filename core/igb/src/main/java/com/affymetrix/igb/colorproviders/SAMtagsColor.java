package com.affymetrix.igb.colorproviders;

import com.affymetrix.genometry.color.ColorProvider;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class SAMtagsColor extends ColorProvider {
    private final static String TAG = "tag";
    private final static List<String> TAG_VALUE = new LinkedList<>();
    public final static String TABLE = " ";

    private HashMap<String,Object> TABLE_VALUES = null;

    static {
        TAG_VALUE.add("");
    }

    protected Parameter<String> samTags = new BoundedParameter<>(TAG_VALUE);
    protected Parameter<HashMap<String,Object>> color_values = new Parameter<>(TABLE_VALUES) {
        @Override
        public boolean set(Object e) {
            if (e != null) {
                try {
                    color_values = (Parameter<HashMap<String, Object>>) e;
                } catch (Exception ex) {
                }
                return super.set(e);
            }

            return super.set(e);
        }
    };

    public SAMtagsColor() {
        super();
        parameters.addParameter(TAG, String.class, samTags);
        parameters.addParameter(TABLE, HashMap.class, color_values);
    }

    @Override
    public String getName() {
        return "samtags";
    }

    @Override
    public String getDisplay() {
        return IGBConstants.BUNDLE.getString("color_by_" + getName());
    }

    @Override
    public Color getColor(SeqSymmetry sym) {
        if (sym instanceof SymWithProps) {
            Object read_tag_value = ((SymWithProps) sym).getProperty(samTags.get());
            if(read_tag_value!=null) {
                String rtv_upcase = read_tag_value.toString().toUpperCase();
                if (color_values instanceof Parameter<HashMap<String, Object>>) {
                    HashMap<String, Object> input_table_values = color_values.get();
                    HashMap<String, Color> itv_temp = new HashMap<>();
                    if(input_table_values != null) {
                        input_table_values.forEach((smtg, col) -> {
                            for (String smtg_val : smtg.toString().toUpperCase().split(";")) {
                                itv_temp.put(smtg_val, (Color) col);
                            }
                        });
                    }else{
                        return Color.lightGray;
                    }
                    if (itv_temp != null) {
                        if (itv_temp.containsKey(rtv_upcase)) {
                            return itv_temp.get(rtv_upcase);
                        } else {
                            return Color.lightGray;
                        }
                    }
                }
            }
        }
        return Color.lightGray;
    }

    public static boolean compare(String string_property_value, Object value) {
        if (value instanceof String) {
            return string_property_value.equalsIgnoreCase(value.toString());
        } else {
            return false;
        }
    }
}

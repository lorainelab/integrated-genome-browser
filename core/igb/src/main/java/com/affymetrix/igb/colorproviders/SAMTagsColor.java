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


public class SAMTagsColor extends ColorProvider {
    private final static String TAG = "tag";
    private final static List<String> TAG_VALUE = new LinkedList<>();
    public final static String TABLE = "values";

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

    public SAMTagsColor() {
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
            Object value = ((SymWithProps) sym).getProperty(samTags.get());
            if(value!=null) {
                String key = value.toString().toUpperCase();
                if (color_values instanceof Parameter<HashMap<String, Object>>) {
                    HashMap<String, Object> key_val = color_values.get();
                    if (key_val != null) {
                        if (key_val.containsKey(key)) {
                            return (Color) key_val.get(key);
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

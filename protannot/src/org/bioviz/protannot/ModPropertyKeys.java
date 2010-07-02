package org.bioviz.protannot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Expresses allowed property names for SeqFeatures.  Offers
 * methods for accessing name-value pairs using Properties
 * Objects retrieved from SeqFeatures.
 */
final class ModPropertyKeys {

    private final String[] keys = new String[0];

    /**
     * Fills up a List with arrays containing names and values
     * for each of the given Properties.
     * e.g., {name,value0,value1,value2,...,valueN} for
     * N different Properties Objects representing a list of
     * If one of the Properties has no value set for a particular
     * name key, then the value for this name is set to ND.
     * @param props - the list of Properties derived from
     *   SeqFeatures.
     */
    List<String[]> getNameValues(Properties[] props) {
        List<String[]> result = new ArrayList<String[]>();
        // collect all possible names from the given Properties
        int num_props = props.length;
        Map<String,String[]> rows_thus_far = new HashMap<String,String[]>();
        for (int i = 0; i < props.length; i++) {
            if (props[i] == null) {
                continue;
            }
            for (String name : props[i].stringPropertyNames()) {
                String[] name_value = rows_thus_far.get(name);
                if (name_value != null) {
                    continue;
                } else {
                    name_value = new String[num_props + 1];
                    name_value[0] = name;
                    for (int j = 0; j < props.length; j++) {
                        Object val = null;
                        if (props[j] != null) {
                            val = props[j].get(name);
                        }
                        val = (val == null ? "ND" : val);
                        name_value[j + 1] = val.toString();
                    }
                    rows_thus_far.put(name, name_value);
                }
            }
        }
        // now sort
        for (int i = 0; i < keys.length; i++) {
            String[] row = rows_thus_far.get(keys[i]);
            if (row != null) {
                result.add(row);
            }
            rows_thus_far.remove(keys[i]);
        }
        for (String[] row : rows_thus_far.values()) {
            result.add(row);
        }
        return result;
    }
}

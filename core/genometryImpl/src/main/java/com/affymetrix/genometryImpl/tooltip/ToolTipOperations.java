/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import static com.affymetrix.genometryImpl.tooltip.ToolTipConstants.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author tarun
 */
public class ToolTipOperations {

    public static Map<String, Object> propertyCleaning(Map<String, Object> props) {
        ToolTipCategory basicCategory = (ToolTipCategory) props.get(ToolTipConstants.BASIC_CATEGORY);
        ToolTipCategory bamCategory = (ToolTipCategory) props.get(ToolTipConstants.BAM_CATEGORY);
        ArrayList<String> keysToRemove = new ArrayList<String>();
        if (basicCategory == null) {
            basicCategory = new ToolTipCategory(null, new HashMap<String, ToolTipValue>());
        }
        if (bamCategory == null) {
            bamCategory = new ToolTipCategory(null, new HashMap<String, ToolTipValue>());
        }

        for (String key : props.keySet()) {
            if (!(key.equals(ToolTipConstants.BASIC_CATEGORY) || key.equals(ToolTipConstants.BAM_CATEGORY))) {
                if (basicCategory.getProperties().containsKey(key)
                        || bamCategory.getProperties().containsKey(key)) {
                    keysToRemove.add(key);
                }
            }
        }

        for (String key : keysToRemove) {
            props.remove(key);
        }

        return props;
    }

    public static ArrayList<ToolTipCategory> formatBamSymTooltip(Map<String, Object> props) {
        int count = 1;
        ArrayList<ToolTipCategory> properties = new ArrayList<ToolTipCategory>();

        HashMap<String, ToolTipValue> basicInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory basicInfoCategory = new ToolTipCategory(ToolTipConstants.BASIC_CATEGORY, 1, basicInfoProps);

        // Populating BASIC Category
        basicInfoProps.put(NAME, fetchToolTipValue(props, NAME, count));
        count++;
        basicInfoProps.put(ID, fetchToolTipValue(props, ID, count));
        count++;
        basicInfoProps.put(CHROMOSOME, fetchToolTipValue(props, CHROMOSOME, count));
        count++;
        basicInfoProps.put(START, fetchToolTipValue(props, START, count));
        count++;
        basicInfoProps.put(END, fetchToolTipValue(props, END, count));
        count++;
        basicInfoProps.put(LENGTH, fetchToolTipValue(props, LENGTH, count));
        count++;
        basicInfoProps.put(AVERAGE_QUALITY, fetchToolTipValue(props, AVERAGE_QUALITY, count));
        count++;

        count = 1;
        HashMap<String, ToolTipValue> bamInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory bamInfoCategory = new ToolTipCategory(ToolTipConstants.BAM_CATEGORY, 2, bamInfoProps);

        // Populating BAM Category
        bamInfoProps.put(ToolTipConstants.RESIDUES, fetchToolTipValue(props, RESIDUES, count));
        count++;
        bamInfoProps.put(ToolTipConstants.STRAND, fetchToolTipValue(props, STRAND, count));
        count++;
        bamInfoProps.put(ToolTipConstants.SHOW_MASK, fetchToolTipValue(props, SHOW_MASK, count));
        count++;
        bamInfoProps.put(ToolTipConstants.SCORES, fetchToolTipValue(props, SCORES, count));
        count++;
        bamInfoProps.put(ToolTipConstants.FORWARD, fetchToolTipValue(props, FORWARD, count));
        count++;
        bamInfoProps.put(ToolTipConstants.MAPQ, fetchToolTipValue(props, MAPQ, count));
        count++;
        bamInfoProps.put(ToolTipConstants.FLAGS, fetchToolTipValue(props, FLAGS, count));
        count++;

        count = 1;
        HashMap<String, ToolTipValue> cigarInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory cigarInfoCategory = new ToolTipCategory(ToolTipConstants.CIGAR_CATEGORY, 3, cigarInfoProps);

        // Populating CIGAR Category
        cigarInfoProps.put(ToolTipConstants.CIGAR, fetchToolTipValue(props, CIGAR, count));
        count++;
        cigarInfoProps.put(ToolTipConstants.CL, fetchToolTipValue(props, CL, count));
        count++;
        cigarInfoProps.put(ToolTipConstants.VN, fetchToolTipValue(props, VN, count));
        count++;
        cigarInfoProps.put(ToolTipConstants.NH, fetchToolTipValue(props, NH, count));
        count++;
        cigarInfoProps.put(ToolTipConstants.NM, fetchToolTipValue(props, NM, count));
        count++;
        cigarInfoProps.put(ToolTipConstants.XS, fetchToolTipValue(props, XS, count));
        count++;
        
        count = 1;
        HashMap<String, ToolTipValue> miscInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory miscInfoCategory = new ToolTipCategory(ToolTipConstants.MISC_CATEGORY, 3, miscInfoProps);
        
        for(String key : props.keySet()) {
            miscInfoProps.put(key, new ToolTipValue(props.get(key).toString(), count));
            count++;
        }

        if (basicInfoProps.size() > 1) {
            properties.add(basicInfoCategory);
        }
        if (bamInfoProps.size() > 1) {
            properties.add(bamInfoCategory);
        }
        if (cigarInfoProps.size() > 1) {
            properties.add(cigarInfoCategory);
        }
        if (miscInfoProps.size() > 1) {
            properties.add(miscInfoCategory);
        }
        
        return properties;
    }

    private static ToolTipValue fetchToolTipValue(Map<String, Object> props, String key, int weight) {
        ToolTipValue value = null;
        if (props.containsKey(key)) {
            value = new ToolTipValue(props.get(key).toString(), weight);
            props.remove(key);
            return value;
        } else {
            return new ToolTipValue(key, -1);
        }
    }
}

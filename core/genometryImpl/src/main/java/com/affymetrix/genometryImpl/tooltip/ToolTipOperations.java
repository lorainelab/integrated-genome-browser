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
        fetchAndPopulateToolTipValue(props, basicInfoProps, NAME, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, ID, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, CHROMOSOME, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, START, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, END, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, LENGTH, count);
        count++;
        fetchAndPopulateToolTipValue(props, basicInfoProps, AVERAGE_QUALITY, count);
        count++;

        count = 1;
        HashMap<String, ToolTipValue> bamInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory bamInfoCategory = new ToolTipCategory(ToolTipConstants.BAM_CATEGORY, 2, bamInfoProps);

        // Populating BAM Category
        fetchAndPopulateToolTipValue(props, bamInfoProps, RESIDUES, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, STRAND, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, SHOW_MASK, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, SCORES, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, FORWARD, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, MAPQ, count);
        count++;
        fetchAndPopulateToolTipValue(props, bamInfoProps, FLAGS, count);
        count++;

        count = 1;
        HashMap<String, ToolTipValue> cigarInfoProps = new HashMap<String, ToolTipValue>();
        ToolTipCategory cigarInfoCategory = new ToolTipCategory(ToolTipConstants.CIGAR_CATEGORY, 3, cigarInfoProps);

        // Populating CIGAR Category
        fetchAndPopulateToolTipValue(props, cigarInfoProps, CIGAR, count);
        count++;
        fetchAndPopulateToolTipValue(props, cigarInfoProps, CL, count);
        count++;
        fetchAndPopulateToolTipValue(props, cigarInfoProps, VN, count);
        count++;
        fetchAndPopulateToolTipValue(props, cigarInfoProps, NH, count);
        count++;
        fetchAndPopulateToolTipValue(props, cigarInfoProps, NM, count);
        count++;
        fetchAndPopulateToolTipValue(props, cigarInfoProps, XS, count);
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

    private static void fetchAndPopulateToolTipValue(Map<String, Object> props, Map<String, ToolTipValue> destProps, String key, int weight) {
        ToolTipValue value = null;
        if (props.containsKey(key)) {
            value = new ToolTipValue(props.get(key).toString(), weight);
            props.remove(key);
            destProps.put(key, value);
        }
    }
    
}

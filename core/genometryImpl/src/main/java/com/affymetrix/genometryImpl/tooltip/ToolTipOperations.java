/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import static com.affymetrix.genometryImpl.tooltip.ToolTipConstants.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tarun
 */
public class ToolTipOperations {

    public static List<ToolTipCategory> formatBamSymTooltip(Map<String, Object> props) {
        List<ToolTipCategory> properties = new ArrayList<ToolTipCategory>();

        Map<String, String> basicInfoProps = new HashMap<String, String>();
        ToolTipCategory basicInfoCategory = new ToolTipCategory(ToolTipConstants.BASIC_CATEGORY, 1, basicInfoProps);

        // Populating BASIC Category
        fetchAndPopulateToolTipValue(props, basicInfoProps, NAME);
        fetchAndPopulateToolTipValue(props, basicInfoProps, ID);
        fetchAndPopulateToolTipValue(props, basicInfoProps, CHROMOSOME);
        fetchAndPopulateToolTipValue(props, basicInfoProps, START);
        fetchAndPopulateToolTipValue(props, basicInfoProps, END);
        fetchAndPopulateToolTipValue(props, basicInfoProps, LENGTH);
        fetchAndPopulateToolTipValue(props, basicInfoProps, AVERAGE_QUALITY);


        Map<String, String> bamInfoProps = new HashMap<String, String>();
        ToolTipCategory bamInfoCategory = new ToolTipCategory(ToolTipConstants.BAM_CATEGORY, 2, bamInfoProps);

        // Populating BAM Category
        fetchAndPopulateToolTipValue(props, bamInfoProps, RESIDUES);
        fetchAndPopulateToolTipValue(props, bamInfoProps, STRAND);
        fetchAndPopulateToolTipValue(props, bamInfoProps, SHOW_MASK);
        fetchAndPopulateToolTipValue(props, bamInfoProps, SCORES);
        fetchAndPopulateToolTipValue(props, bamInfoProps, FORWARD);
        fetchAndPopulateToolTipValue(props, bamInfoProps, MAPQ);
        fetchAndPopulateToolTipValue(props, bamInfoProps, FLAGS);

        Map<String, String> cigarInfoProps = new HashMap<String, String>();
        ToolTipCategory cigarInfoCategory = new ToolTipCategory(ToolTipConstants.CIGAR_CATEGORY, 3, cigarInfoProps);

        // Populating CIGAR Category
        fetchAndPopulateToolTipValue(props, cigarInfoProps, CIGAR);
        fetchAndPopulateToolTipValue(props, cigarInfoProps, CL);
        fetchAndPopulateToolTipValue(props, cigarInfoProps, VN);
        fetchAndPopulateToolTipValue(props, cigarInfoProps, NH);
        fetchAndPopulateToolTipValue(props, cigarInfoProps, NM);
        fetchAndPopulateToolTipValue(props, cigarInfoProps, XS);
        
        Map<String, String> miscInfoProps = new HashMap<String, String>();
        ToolTipCategory miscInfoCategory = new ToolTipCategory(ToolTipConstants.MISC_CATEGORY, 3, miscInfoProps);
        
        for(String key : props.keySet()) {
            miscInfoProps.put(key, props.get(key).toString());
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

    private static void fetchAndPopulateToolTipValue(Map<String, Object> props, Map<String, String> destProps, String key) {
        String value = null;
        if (props.containsKey(key)) {
            value = props.get(key).toString();
            props.remove(key);
            destProps.put(key, value);
        }
    }
    
}

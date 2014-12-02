/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import static com.affymetrix.genometryImpl.tooltip.ToolTipConstants.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
        List<ToolTipCategory> categories = new ArrayList<ToolTipCategory>();

        // Populating BASIC Category
        populateCategory(props, BASIC_CATEGORY, categories);

        // Populating BAM Category
        populateCategory(props, BAM_CATEGORY, categories);

        // Populating CIGAR Category
        populateCategory(props, CIGAR_CATEGORY, categories);

        // Populating MISC category
        populateMisc(props, categories);

        return categories;
    }

    private static void populateCategory(Map<String, Object> props, ImmutableMap<String, ImmutableList<String>> categoryData, List<ToolTipCategory> categories) {
        String value;
        String categoryKey = categoryData.keySet().iterator().next();
        ImmutableList<String> keys = categoryData.get(categoryKey);
        Map<String, String> destProps = new HashMap<String, String>();
        ToolTipCategory category = null;
        for (String key : keys) {
            if (props.containsKey(key)) {
                value = props.get(key).toString();
                props.remove(key);
                destProps.put(key, value);
            }
        }
        if(destProps.size() > 0){
            category = new ToolTipCategory(categoryKey, 1, destProps);
            categories.add(category);
        }
    }
    
    private static void populateMisc(Map<String, Object> props, List<ToolTipCategory> categories) {
        Map<String, String> miscInfoProps = new HashMap<String, String>();
        ToolTipCategory miscInfoCategory = new ToolTipCategory(MISC_CATEGORY, 3, miscInfoProps);

        for (String key : props.keySet()) {
            miscInfoProps.put(key, props.get(key).toString());
        }

        if (miscInfoProps.size() > 1) {
            categories.add(miscInfoCategory);
        }
    }
    
}

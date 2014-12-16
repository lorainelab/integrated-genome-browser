/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import static com.affymetrix.genometryImpl.tooltip.ToolTipConstants.*;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.PropertyHandler;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.OrderComparator;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.view.SeqMapToolTips.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author tkanapar
 */
public class SelectionRuleAction extends GenericAction {
    
    private static final long serialVersionUID = 1L;

    private static final String no_selection_text = "Click the map below to select annotations";
    private static final String selection_info = "Selection Info";
    private static final Comparator<String> comparator = new OrderComparator(PropertyHandler.prop_order);
    private static final SelectionRuleAction ACTION = new SelectionRuleAction();
    private SymWithProps sym;

    private Map<String, Object> properties;
    private String selectionText;

    private SelectionRuleAction() {
        super("Get Info", BUNDLE.getString("selectionInforTooltip"), "16x16/actions/info.png", "16x16/actions/info.png", 0);
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setSelectionText(String selectionText) {
        this.selectionText = selectionText;
    }

    public static SelectionRuleAction getAction() {
        return ACTION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame messageFrame = new JFrame();
        JTextArea rules_text = new JTextArea();
        rules_text.setBorder(new EmptyBorder(10, 10, 10, 10));
        rules_text.setEditable(false);
        rules_text.setLineWrap(true);
        rules_text.setColumns(40);
        JScrollPane scroll_pane = new JScrollPane(rules_text);
        messageFrame.add(scroll_pane);
        if (no_selection_text.equals(selectionText)) {
            messageFrame.setTitle("How to Select and De-select Data in IGB");
            rules_text.append(getRules());
        } else {
            Map<String, Object> properties;
            properties = orderProperties();
            messageFrame.setTitle(selection_info);
            if (properties != null && !properties.isEmpty()) {
                int maxLength = 0;
                for (String key : properties.keySet()) {
                    StringBuilder value = new StringBuilder();
                    if (properties.get(key) instanceof String[]) {
                        for (String obj : (String[]) properties.get(key)) {
                            value.append(obj);
                        }
                    } else {
                        value.append(properties.get(key));
                    }
                    rules_text.append(key + ": " + value + "\n");
                    if (properties.get(key).toString().length() > maxLength) {
                        maxLength = properties.get(key).toString().length();
                    }
                }
                if (maxLength > 200) {
                    rules_text.setColumns(60);
                }
            } else {
                rules_text.append(selectionText);
            }
        }
        messageFrame.setMinimumSize(new Dimension(250, 100));
        messageFrame.pack();
        messageFrame.setLocationRelativeTo(IGB.getSingleton().getFrame());
        messageFrame.setVisible(true);
    }

    private String getRules() {
        return "1. Click on an annotation to select it.\n"
                + "2. Double-click something to zoom in on it.\n"
                + "3. Click-drag a region to select and count many items.\n"
                + "4. Click-SHIFT to add to the currently selected items.\n"
                + "5. Control-SHIFT click to remove an item from the currently selected items.\n"
                + "6. Click-drag the axis to zoom in on a region.\n";
    }

    private Map<String, Object> orderProperties() {
        List<String> propertyKeys = new ArrayList<String>();
        if (isBamSym(sym)) {
            propertyKeys.addAll(BAM_INFO_GRP);
            propertyKeys.addAll(BAM_LOC_GRP);
            propertyKeys.addAll(BAM_CIGAR_GRP);
        } else if (isBedSym(sym)) {
            propertyKeys.addAll(BED14_INFO_GRP);
            propertyKeys.addAll(BED14_LOC_GRP);
            propertyKeys.addAll(BED14_CIGAR_GRP);
        } else if (isLinkPSL(sym)) {
            propertyKeys.addAll(PSL_INFO_GRP);
            propertyKeys.addAll(PSL_LOC_GRP);
        } else if (isGFFSym(sym)) {
            propertyKeys.addAll(GFF_INFO_GRP);
            propertyKeys.addAll(GFF_LOC_GRP);
            propertyKeys.addAll(GFF_CIGAR_GRP);
        } else {
            propertyKeys.addAll(DEFAULT_INFO_GRP);
            propertyKeys.addAll(DEFAULT_LOC_GRP);
            propertyKeys.addAll(DEFAULT_CIGAR_GRP);
        }
        return orderProperties(propertyKeys);
    }

    private Map<String, Object> orderProperties(List<String> propertyKeys) {
        Map<String, Object> orderedProps = new LinkedHashMap<String, Object>();
        for (String property : propertyKeys) {
            if (properties.containsKey(property)) {
                orderedProps.put(property, properties.get(property).toString());
                //properties.remove(property);
            }
        }

        for (String property : properties.keySet()) {
            boolean test = propertyKeys.contains(property);
            if (!test) {
                orderedProps.put(property, properties.get(property).toString());
            }
        }
        return orderedProps;
    }

    public SymWithProps getSym() {
        return sym;
    }

    public void setSym(SymWithProps sym) {
        this.sym = sym;
    }
    
    
    
}

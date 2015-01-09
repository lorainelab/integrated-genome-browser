/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.tooltip.ToolTipCategory;
import static com.affymetrix.genometryImpl.tooltip.ToolTipConstants.STRAND;
import com.affymetrix.genometryImpl.tooltip.ToolTipOperations;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.apache.commons.lang3.text.WordUtils;
import static com.affymetrix.genometryImpl.util.SeqUtils.*;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tkanapar
 */
public class SeqMapToolTips extends JWindow {

    private static final long serialVersionUID = 1L;
    private static final SimpleAttributeSet NAME = new SimpleAttributeSet();
    private static final Logger logger = LoggerFactory.getLogger(SeqMapToolTips.class);
    private static final int TOOLTIP_BOTTOM_PADDING = 6;
    private static final int TOOLTIP_RIGHT_PADDING = 10;
    private int maxLength = 0;
    FontMetrics fontMetrics;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    static {
        StyleConstants.setBold(NAME, true);
    }
    private static final Color DEFAULT_BACKGROUNDCOLOR = new Color(253, 254, 196);
    private static final int MIN_HEIGHT = 200;
    private static final int MAX_WIDTH = 230;
    private static final int MAX_CHAR_PER_LINE = 30;
    private static final int PADDING = 5;
    private final JTextPane tooltip;
    private final Color backgroundColor;

    private Timer timer = new Timer(100, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(true);
        }

    });

    public SeqMapToolTips(Window owner) {
        super(owner);
        tooltip = new JTextPane();
        tooltip.setEditable(false);
        tooltip.setFont(new Font("Courier", Font.PLAIN, 12));
        fontMetrics = tooltip.getFontMetrics(tooltip.getFont());
        this.backgroundColor = DEFAULT_BACKGROUNDCOLOR;
        init();
    }

    private String wrappedString(String key, String value) {
        String input = key + "*" + value;
        if(maxLength < input.length()){
            maxLength = input.length();
        }
        String output = WordUtils.wrap(input, MAX_CHAR_PER_LINE, "\n", true);
        output = output.substring(key.length() + 1);
        return output;
    }

    public void setToolTip(Point point, Map<String, Object> properties, SeqSymmetry sym) {
        List<ToolTipCategory> propList;
        if (isVisible() && (properties == null || properties.isEmpty())) {
            setVisible(false);
        }
        timer.stop();
        if (!getOwner().isActive()) {
            return;
        }
        tooltip.setText(null);
        if (properties != null && properties.size() > 0 && sym != null) {
            if (isBamSym(sym)) {
                propList = ToolTipOperations.formatBamSymTooltip(properties);
            } else if (isBedSym(sym)) {
                propList = ToolTipOperations.formatBED14SymTooltip(properties);
            } else if (isLinkPSL(sym)) {
                propList = ToolTipOperations.formatLinkPSLSymTooltip(properties);
            } else if (isGFFSym(sym)) {
                propList = ToolTipOperations.formatGFFSymTooltip(properties);
            } else if (isMultiStrandWrapperType(sym)) {
                //for now manually remove strand information since it is added for everything
                properties.remove(STRAND);
                propList = ToolTipOperations.formatBamSymTooltip(properties);
            } else {
                logger.warn("Sym class not handled: " + sym.getClass().getSimpleName());
                propList = ToolTipOperations.formatDefaultSymTooltip(properties);
            }
            formatCategoryToolTip(propList);
            tooltip.setCaretPosition(0);
            setLocation(determineBestLocation(point));
            if (isVisible()) {
                timer.setInitialDelay(0);
            } else {
                timer.setInitialDelay(500);
            }
            setSize(MAX_WIDTH, obtainOptimumHeight());
            timer.setRepeats(false);
            timer.start();
        } else {
            setVisible(false);
            tooltip.setText(null);
        }
    }

    private void formatCategoryToolTip(List<ToolTipCategory> properties) {
        Map<String, String> toolTipProps;
        String propValue;
        int count = 0;
        int propCount = 0;
        try {
            for (ToolTipCategory category : properties) {
                // Added to avoid an extra "--------------" in tooltip
                if (count > 0) {
                    tooltip.getDocument().insertString(tooltip.getDocument().getLength(), LINE_SEPARATOR + "----------" + LINE_SEPARATOR, null);
                }
                count = 1;
                // Uncomment following line for category labels
                //tooltip.getDocument().insertString(tooltip.getDocument().getLength(), category.getCategory() + ":\n", NAME);
                toolTipProps = category.getProperties();
                propCount = 0;
                for (String propKey : toolTipProps.keySet()) {
                    // Added to avoid an extra line at the end of tooltip
                    if (propCount > 0) {
                        tooltip.getDocument().insertString(tooltip.getDocument().getLength(), LINE_SEPARATOR, null);
                    }
                    propCount = 1;
                    propValue = toolTipProps.get(propKey);
                    tooltip.getDocument().insertString(tooltip.getDocument().getLength(), propKey + " ", NAME);
                    tooltip.getDocument().insertString(tooltip.getDocument().getLength(), wrappedString(propKey, propValue), null);
                }
                
            }
        } catch (BadLocationException e) {
            logger.debug("Accessing invalid location in tooltip text: ", e);
        }
    }

    private Point determineBestLocation(Point currentPoint) {
        Point bestLocation = new Point(currentPoint.x + 10, currentPoint.y + 10);
        return bestLocation;
    }

    private void init() {
        setFocusableWindowState(false);
        setBackground(backgroundColor);
        setForeground(backgroundColor);
        tooltip.setBackground(backgroundColor);
        tooltip.setDisabledTextColor(tooltip.getForeground());

        tooltip.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        tooltip.setLayout(new BorderLayout(0, 0));
        add(tooltip);
        pack();
        setSize(MAX_WIDTH, MIN_HEIGHT);
    }

    private int obtainOptimumHeight() {
        int start = 0;
        int noOfLines = 1;
        String tooltipText = tooltip.getText();
        while((start=tooltipText.indexOf(LINE_SEPARATOR, start)) > -1) {
            noOfLines++;
            start++;
        }
        int lineHeight = fontMetrics.getHeight();
        int totalHeight = lineHeight * noOfLines;
        return totalHeight + TOOLTIP_BOTTOM_PADDING;
    }

    private int obtainOptimumWidth() {
        int widths[] = fontMetrics.getWidths();
        int charWidth = widths[65];
        if(maxLength > MAX_CHAR_PER_LINE) {
            maxLength = MAX_CHAR_PER_LINE;
        }
        int maxWidth = charWidth * maxLength;
        return maxWidth + TOOLTIP_RIGHT_PADDING;
    }

}
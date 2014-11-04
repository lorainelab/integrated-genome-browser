/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author tkanapar
 */
public class SeqMapToolTips extends JWindow {

    private static final long serialVersionUID = 1L;
    private static final SimpleAttributeSet NAME = new SimpleAttributeSet();

    static {
        StyleConstants.setBold(NAME, true);
    }
    private static final Color DEFAULT_BACKGROUNDCOLOR = new Color(253, 254, 196);
    private static final int MIN_HEIGHT = 200;
    private static final int MAX_WIDTH = 300;
    private final JTextPane tooltip;
    private final Color backgroundColor;
    private String[][] properties;

    public SeqMapToolTips(Window owner) {
        super(owner);
        tooltip = new JTextPane();
        tooltip.setEditable(false);
        this.backgroundColor = DEFAULT_BACKGROUNDCOLOR;
        init();
    }

    public void setToolTip(Point point, String[][] properties) {
        if (isVisible() && properties == null) {
            setVisible(false);
        }
        timer.stop();
        if (!getOwner().isActive()) {
            return;
        }

        if (properties != null && properties.length > 1) {
            timer.stop();

            this.properties = properties;
            formatTooltip();
            tooltip.setCaretPosition(0);
            setLocation(determineBestLocation(point));
            pack();
            setSize(MAX_WIDTH, getSize().height);
            timer.setInitialDelay(500);
            timer.start();

        } else if (isVisible()) {
        } else {
            this.properties = properties;
            tooltip.setText(null);
        }
    }
    private String wrappedString(String input){
        
        StringBuilder output = new StringBuilder(input);
        int index = input.length();
        int size = MAX_WIDTH/10;
        while(index > 0){
            output.insert(index, "\n");
            index-=size;
        }
        return output.toString();
    }
    
    private void formatTooltip() {
        tooltip.setText(null);
        for (String[] propertie : properties) {
            try {
                tooltip.getDocument().insertString(tooltip.getDocument().getLength(), propertie[0], NAME);
                tooltip.getDocument().insertString(
                        tooltip.getDocument().getLength(), " ", null);
                tooltip.getDocument().insertString(tooltip.getDocument().getLength(), wrappedString(propertie[1]), null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
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

        JScrollPane scrollPane = new JScrollPane(tooltip);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(backgroundColor);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        setLayout(new BorderLayout(0, 0));
        add(scrollPane);

        pack();
        setSize(MAX_WIDTH, MIN_HEIGHT);
    }

    Timer timer = new Timer(100, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(true);

        }
    });

}

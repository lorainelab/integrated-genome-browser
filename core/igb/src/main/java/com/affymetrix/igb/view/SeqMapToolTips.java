/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.lang.reflect.Method;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
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
    private static final HashMap<TextAttribute, Object> messageAttrMap = new HashMap<TextAttribute, Object>();
    private static final SimpleAttributeSet NAME = new SimpleAttributeSet();

    static {
        messageAttrMap.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        messageAttrMap.put(TextAttribute.FOREGROUND, Color.DARK_GRAY);
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
        this.backgroundColor = DEFAULT_BACKGROUNDCOLOR;
        init();
    }

    public void setToolTip(Point point, String[][] properties) {
        if (isVisible() && properties == null) {
            setVisible(false);
            Opacity.INSTANCE.set(SeqMapToolTips.this, 0.5f);
        }

        if (Opacity.INSTANCE.isSupported()) {
            timer.stop();
        }

        if (!getOwner().isActive()) {
            return;
        }

        if (properties != null && properties.length > 1) {
            this.properties = properties;
            formatTooltip();
            tooltip.setCaretPosition(0);

            setLocation(determineBestLocation(point));
            setVisible(true);
            if (Opacity.INSTANCE.isSupported()) {
                timer.setInitialDelay(1000);
                timer.start();
            }

        } else if (isVisible()) {
        } else {
            this.properties = properties;
            tooltip.setText(null);
        }
    }

    private void formatTooltip() {
        tooltip.setText(null);
        for (String[] propertie : properties) {
            try {
                tooltip.getDocument().insertString(tooltip.getDocument().getLength(), propertie[0], NAME);
                tooltip.getDocument().insertString(
                        tooltip.getDocument().getLength(), " ", null);
                tooltip.getDocument().insertString(tooltip.getDocument().getLength(), propertie[1], null);
                tooltip.getDocument().insertString(
                        tooltip.getDocument().getLength(), "\n", null);
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
        Opacity.INSTANCE.set(SeqMapToolTips.this, 0.5f);

        tooltip.setBackground(backgroundColor);
        tooltip.setEditable(false);
        tooltip.setDisabledTextColor(tooltip.getForeground());
        

        JScrollPane scrollPane = new JScrollPane(tooltip);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBackground(backgroundColor);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        setLayout(new BorderLayout(0, 0));
        add(scrollPane);

        if (Opacity.INSTANCE.isSupported()) {
            Toolkit.getDefaultToolkit().addAWTEventListener(opacityController, AWTEvent.MOUSE_EVENT_MASK);
        }

        pack();
        setSize(MAX_WIDTH, MIN_HEIGHT);
    }

    Timer timer = new Timer(100, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            float opacity = Opacity.INSTANCE.get(SeqMapToolTips.this);
            if (opacity + 0.05f >= 1.0f) {
                timer.stop();
                return;
            }
            Opacity.INSTANCE.set(SeqMapToolTips.this, opacity + 0.05f);
        }
    });

    AWTEventListener opacityController = new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
            if (event.getSource() instanceof Component
                    && SwingUtilities.isDescendingFrom((Component) event.getSource(), SeqMapToolTips.this)) {
                setTranslucency((MouseEvent) event);
            }
        }

        private void setTranslucency(MouseEvent e) {
            Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), SeqMapToolTips.this);

            if (SeqMapToolTips.this.contains(p)) {
                Opacity.INSTANCE.set(SeqMapToolTips.this, 1.0f);
            } else {
                Opacity.INSTANCE.set(SeqMapToolTips.this, 0.5f);
            }
        }

    };


    private enum Opacity {

        INSTANCE;

        private final boolean isSupported;
        private Method setOpacityMethod, getOpacityMethod;

        @SuppressWarnings("unchecked")
        private Opacity() {
            boolean noException = true;
            try {
                Class clazz = Class.forName("com.sun.awt.AWTUtilities");
                setOpacityMethod = clazz.getDeclaredMethod("setWindowOpacity", Window.class, float.class);
                getOpacityMethod = clazz.getDeclaredMethod("getWindowOpacity", Window.class);

                // Make dummy call to methods
                JWindow window = new JWindow();
                float opacity = _get(window);
                _set(window, opacity);
            } catch (Exception ex) {
                noException = false;
            }

            isSupported = noException;
        }

        public boolean isSupported() {
            return isSupported;
        }

        // Catch excetions here
        public void set(Window window, float opacity) {
            if (!isSupported()) {
                return;
            }

            try {
                _set(window, opacity);
            } catch (Exception ex) {
            }
        }

        // Catch excetions here
        public float get(Window window) {
            if (!isSupported()) {
                return 1.0f;
            }

            try {
                return _get(window);
            } catch (Exception ex) {
            }

            return 1.0f;
        }

        private void _set(Window window, float opacity) throws Exception {
            setOpacityMethod.invoke(null, window, opacity);
        }

        private float _get(Window window) throws Exception {
            return ((Float) getOpacityMethod.invoke(null, window));
        }
    }
    
    
}

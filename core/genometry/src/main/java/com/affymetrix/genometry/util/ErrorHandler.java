/**
 * Copyright (c) 2001-2005 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.util;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.OKAction;
import com.affymetrix.genometry.event.ReportBugAction;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple routines for bringing-up an error message panel and also logging the error to standard output.
 *
 * @author ed
 */
public abstract class ErrorHandler implements DisplaysError {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

    private static DisplaysError displayHandler;

    static {
        setDisplayHandler(null);
    }

    public static void setDisplayHandler(DisplaysError dH) {
        if (dH == null) {
            displayHandler = new ErrorHandler() {
            };
        } else {
            displayHandler = dH;
        }
    }

    /**
     * Error panel with default title.
     */
    public static void errorPanel(String message) {
        errorPanel("ERROR", message, (Throwable) null, Level.SEVERE);
    }

    /**
     * Error panel with default title and given Throwable.
     *
     * @param message
     * @param e
     */
    public static void errorPanel(String message, Throwable e, Level level) {
        errorPanel("ERROR", message, e, level);
    }

    public static void errorPanel(String title, String message, Level level) {
        errorPanel(title, message, (Throwable) null, level);
    }

    public static void errorPanel(final String title, String message, final List<Throwable> errs, Level level) {
        errorPanel(title, message, errs, null, level);
    }

    public static void errorPanelWithReportBug(String title, String message, Level level) {
        List<GenericAction> actions = new ArrayList<>();
        actions.add(OKAction.getAction());
        actions.add(ReportBugAction.getAction());
        errorPanel(title, message, new ArrayList<>(), actions, level);
    }

    @Override
    public void showError(final String title, final String message, final List<GenericAction> actions, Level level) {
        final Component scroll_pane = makeScrollPane(message);

        String[] options = null;
        if (actions != null) {
            options = new String[actions.size()];
            for (int i = 0; i < actions.size(); i++) {
                options[i] = actions.get(i).getText();
            }
        }
        final JOptionPane pane = new JOptionPane(scroll_pane, JOptionPane.ERROR_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options);
        final JDialog dialog = pane.createDialog(null, title);
        dialog.setResizable(true);

        SwingUtilities.invokeLater(() -> {
            processDialog(pane, dialog, actions);
        });

    }

    private static void errorPanel(String title, String message, Throwable e, Level level) {
        List<Throwable> errs = new ArrayList<>();
        if (e != null) {
            errs.add(e);
        }
        errorPanel(title, message, errs, null, level);
    }

    /**
     * Opens a JOptionPane.ERROR_MESSAGE panel with the given frame as its parent. This is designed to probably be safe
     * from the EventDispatchThread or from any other thread.
     */
    private static void errorPanel(final String title, String message, final List<Throwable> errs,
            final List<GenericAction> actions,
            Level level) {
        LOG.warn("Cannot open file: Could not load index file for file:/home/dcnorris/Downloads/cold_control.mm.bam. An index file is required for BAM files.");
        errs.stream().forEach(error -> LOG.warn(error.getMessage(), error));
        displayHandler.showError(title, message, actions, level);
    }

    private static JScrollPane makeScrollPane(String message) {
        JTextPane text = new JTextPane();
        text.setContentType("text/plain");
        text.setText(message);
        text.setEditable(false);
        text.setCaretPosition(0); // scroll to the top
        JScrollPane scroller = new JScrollPane(text);
        scroller.setPreferredSize(new java.awt.Dimension(400, 100));
        return scroller;
    }

    private static void processDialog(JOptionPane pane, JDialog dialog, List<GenericAction> actions) {
        dialog.setVisible(true);
        Object selectedValue = pane.getValue();
        if (selectedValue != null && actions != null) {
            actions.stream().filter(action -> action != null && selectedValue.equals(action.getText())).forEach(action -> {
                action.actionPerformed(null);
            });
        }
    }
}

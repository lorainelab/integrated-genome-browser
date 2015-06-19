/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.lorainelab.igb.keystrokes;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genoviz.swing.ExistentialTriad;
import com.affymetrix.genoviz.swing.SuperBooleanCellEditor;
import com.affymetrix.igb.swing.JRPJPanel;
import com.affymetrix.igb.swing.jide.JRPStyledTable;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.lorainelab.igb.keystrokes.model.KeyStrokeViewTableModel;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.HtmlHelpProvider;
import com.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import net.miginfocom.swing.MigLayout;
import org.slf4j.LoggerFactory;

/**
 * A panel that shows the preferences mapping between KeyStroke's and Actions.
 */
@Component(name = KeyStrokesView.COMPONENT_NAME, immediate = true, provide = {PreferencesPanelProvider.class})
public final class KeyStrokesView implements PreferencesPanelProvider, HtmlHelpProvider {

    public static final String COMPONENT_NAME = "KeyStrokesView";
    private JRPStyledTable table = new KeyStrokeViewTable("KeyStrokesView");
    public KeyStrokeViewTableModel model;
    public static final int IconColumn = 0;
    public static final int ToolbarColumn = 1;
    public static final int ActionColumn = 2;
    public static final int KeyStrokeColumn = 3;
    public static final int IdColumn = 4; // not displayed in table
    public static final int ColumnCount = 4;
    private final ListSelectionModel lsm;
    private KeyStrokeEditPanel editPanel;
    private int selected = -1;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KeyStrokesView.class);
    private final JRPJPanel keyStrokePanel;
    private IgbService igbService;
    private static final int TAB_POSITION = 3;

    @Reference
    public void addIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Reference
    public void addEditPanel(KeyStrokeEditPanel editPanel) {
        this.editPanel = editPanel;
    }

    private ListSelectionListener listSelectionListener = new ListSelectionListener() {
        /**
         * This is called when the user selects a row of the table.
         */
        @Override
        public void valueChanged(ListSelectionEvent evt) {
            if (evt.getSource() == lsm && !evt.getValueIsAdjusting()) {
                int srow = table.getSelectedRow();
                if (srow >= 0) {
                    String id = (String) table.getModel().getValueAt(srow, IdColumn);
                    editKeystroke(id);
                } else {
                    editPanel.setPreferenceKey(null, null);
                }
            }
        }

        private void editKeystroke(String id) {
            editPanel.setPreferenceKey(id, "");
        }
    };

    private PreferenceChangeListener pcl = evt -> {
        if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
            return;
        }
        // Each time a keystroke preference is changed, update the whole table.
        // Inelegant, but it works.
        refresh();
    };

    public KeyStrokesView() {
        table = new KeyStrokeViewTable("KeyStrokesView");
        keyStrokePanel = new JRPJPanel(KeyStrokesView.class.getName(), new MigLayout("fill")) {

            @Override
            public int getWeight() {
                return TAB_POSITION;
            }
            
        };
        JScrollPane scrollPane = new javax.swing.JScrollPane();
        scrollPane.setViewportView(table);
        keyStrokePanel.setName("Toolbar");
        keyStrokePanel.setToolTipText("Edit Locations");
        keyStrokePanel.add(scrollPane, "grow");
        lsm = table.getSelectionModel();
    }

    @Activate
    public void activate() {
        lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.setCellSelectionEnabled(false);
        table.setModel(model);
        table.setRowHeight(20);

        table.getColumnModel().getColumn(IconColumn).setPreferredWidth(25);
        table.getColumnModel().getColumn(IconColumn).setMinWidth(25);
        table.getColumnModel().getColumn(IconColumn).setMaxWidth(25);

        table.getColumnModel().getColumn(ToolbarColumn).setPreferredWidth(70);
        table.getColumnModel().getColumn(ToolbarColumn).setMinWidth(70);
        table.getColumnModel().getColumn(ToolbarColumn).setMaxWidth(70);

        table.getColumnModel().getColumn(ActionColumn).setPreferredWidth(215);

        table.getColumnModel().getColumn(KeyStrokeColumn).setPreferredWidth(35);

        editPanel.setEnabled(false);

        try {
            PreferenceUtils.getKeystrokesNode().flush();
        } catch (Exception e) {
        }

        refresh();
        lsm.addListSelectionListener(listSelectionListener);
        PreferenceUtils.getKeystrokesNode().addPreferenceChangeListener(pcl);
    }

    @Override
    public String getHelpHtml() {
        String htmlText = null;
        try {
            htmlText = Resources.toString(KeyStrokesView.class.getResource("/help/keyStrokesViewGUI.html"), Charsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Help file not found " , ex);
        }
        return htmlText;
    }

    @Reference(optional = false)
    public void setModel(KeyStrokeViewTableModel model) {
        this.model = model;
    }

    /**
     * Should fix the problems associated with updating entire table at every
     * preference change.
     */
    @Override
    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            model.refresh();
            if (selected > 0) {
                table.setRowSelectionInterval(selected, selected);
            }
        });

    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    class KeyStrokeViewTable extends JRPStyledTable {

        public KeyStrokeViewTable(String id) {
            super(id);
            setDefaultEditor(ExistentialTriad.class, new SuperBooleanCellEditor());
            setDefaultRenderer(ExistentialTriad.class, new SuperBooleanCellEditor());
        }

        private static final long serialVersionUID = 1L;

        @Override
        public TableCellEditor getCellEditor(int row, int col) {
            final DefaultCellEditor textEditor = new KeyEditor(editPanel.getKeyField());
            if (col == KeyStrokeColumn) {
                selected = row;
                textEditor.setClickCountToStart(1);
                return textEditor;
            }
            return super.getCellEditor(row, col);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int col) {
            TableCellRenderer renderer = super.getCellRenderer(row, col);
            if (col == ToolbarColumn) {
                ((SuperBooleanCellEditor) renderer).setHorizontalAlignment(SwingConstants.CENTER);
            } else if (col == IconColumn) {
            } else {
                ((DefaultTableCellRenderer) renderer).setHorizontalAlignment(SwingConstants.LEFT);
            }
            return renderer;
        }
    }

    @Override
    public int getWeight() {
        return TAB_POSITION;
    }

    @Override
    public JRPJPanel getPanel() {
        return keyStrokePanel;
    }

    private class KeyEditor extends DefaultCellEditor {

        private static final long serialVersionUID = 1L;

        KeyEditor(JTextField tf) {
            super(tf);
            tf.addKeyListener(listener);
        }

        @Override
        public boolean stopCellEditing() {
            editorComponent.removeKeyListener(listener);
            return super.stopCellEditing();
        }

        private KeyListener listener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent evt) {
                KeyStroke ks = KeyStroke.getKeyStroke(evt.getKeyCode(), evt.getModifiers());
                if (ks.getKeyCode() == KeyEvent.VK_ENTER) {
                    stopCellEditing();
                    refresh();
                }
            }

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }
        };
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import org.bioviz.protannot.PropertySheetHelper;
import org.bioviz.protannot.model.InterProScanTableModel;

/**
 *
 * @author Tarun
 */
public class InterProScanResultSheet extends JPanel {

    private final JLabel title;
    private final JScrollPane scrollPane;
    private final JViewport jvp;
    private static final String DEFAULT_TITLE = "InterProScan";
    private InterProScanTableModel tableData;
    private final JTable table;
    private final PropertySheetHelper helper;

    public InterProScanResultSheet() {
        super();
        this.title = new JLabel(DEFAULT_TITLE);
        this.table = new JTable();
        this.helper = new PropertySheetHelper(table);
        this.jvp = new JViewport();
        this.scrollPane = new JScrollPane(table);

        setUpPanel();
    }

    private void setUpPanel() {
        jvp.setView(title);
        scrollPane.setColumnHeader(jvp);

        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        table.addMouseListener(helper);
        table.addMouseMotionListener(helper);
        table.setRowSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setAutoCreateRowSorter(true);
        table.setEnabled(true);
    }
    
    public void setTitle(String title) {
        this.title.setText(title);
    }
    
    public String getTitle() {
        return title.getText();
    }
    
    public void showTableData(InterProScanTableModel tableData) {
        this.tableData = tableData;
        table.setModel(tableData);
        table.setDefaultRenderer(Object.class, helper);
    }
}

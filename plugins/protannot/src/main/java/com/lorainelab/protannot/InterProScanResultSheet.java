/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot;

import com.affymetrix.igb.swing.jide.JRPStyledTable;
import static com.lorainelab.protannot.ProtAnnotAction.BUNDLE;
import com.lorainelab.protannot.model.InterProScanTableModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Tarun
 */
public class InterProScanResultSheet extends JPanel {

    public static final String COMPONENT_NAME = "InterProScanResultSheet";
    private final JLabel title;
    private final JScrollPane scrollPane;
    private final JViewport jvp;
    private static final String DEFAULT_TITLE = "InterProScan";
    private InterProScanTableModel ipsTableModel;
    private final JRPStyledTable table;
    private final PropertySheetHelper helper;
    private final JButton cancelAllJobs;
    private final JButton runInterProScan;

    public InterProScanResultSheet() {
        super();
        this.title = new JLabel(DEFAULT_TITLE);
        this.table = new JRPStyledTable("IPS Table");
        this.helper = new PropertySheetHelper(table);
        this.jvp = new JViewport();
        this.scrollPane = new JScrollPane(table);
        cancelAllJobs = new JButton("Cancel All Jobs");
        runInterProScan = new JButton(BUNDLE.getString("menuRunInterProScan")+"...");
        setUpPanel();
    }

    private void setUpPanel() {
        jvp.setView(title);
        scrollPane.setColumnHeader(jvp);
        LC lc = new LC();
        setLayout(new MigLayout("fill"));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(runInterProScan, "left");
        buttonPanel.add(cancelAllJobs, "left");
        add(buttonPanel, "wrap");
        add(scrollPane, "grow, push, span");

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

    public void showTableData(InterProScanTableModel ipsTableModel) {
        this.ipsTableModel = ipsTableModel;
        table.setModel(ipsTableModel);
        table.setDefaultRenderer(Object.class, helper);
    }

    public JButton getCancelAllJobs() {
        return cancelAllJobs;
    }

    public JButton getRunInterProScan() {
        return runInterProScan;
    }

    
}

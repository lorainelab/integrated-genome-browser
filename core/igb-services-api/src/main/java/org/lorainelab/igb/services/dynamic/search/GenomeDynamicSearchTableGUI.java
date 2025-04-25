/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package org.lorainelab.igb.services.dynamic.search;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 * @author sravani
 */
public class GenomeDynamicSearchTableGUI extends javax.swing.JPanel {

    private final GenomeDynamicSearchTableModel genomeDynamicSearchTableModel;
    private final GenomeDynamicSearchTable genomeDynamicSearchTable;
    private final ExternalGenomeDataProvider externalGenomeDataProvider;
    private int currentPage = 1;
    private final int ROWS_PER_PAGE = 100;
    private int totalPages;
    private String sortedColumn;
    private boolean ascending = true;
    private javax.swing.JLabel resultsLabel;         // Left side label
    private javax.swing.JPanel rightPaginationPanel; // Right side panel for page numbers

    /**
     * Creates new form GenomeDynamicSearchTableGUI
     */
    public GenomeDynamicSearchTableGUI(ExternalGenomeDataProvider externalGenomeDataProvider) {
        this.externalGenomeDataProvider = externalGenomeDataProvider;
        genomeDynamicSearchTableModel = new GenomeDynamicSearchTableModel(externalGenomeDataProvider);
        genomeDynamicSearchTable = new GenomeDynamicSearchTable(genomeDynamicSearchTableModel, externalGenomeDataProvider);
        initComponents();
        refreshTable();
        updateTableHeader(0, ascending);
        genomeDynamicSearchTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int columnIndex = genomeDynamicSearchTable.columnAtPoint(e.getPoint());
                String columnName = genomeDynamicSearchTable.getColumnName(columnIndex);
                if(!columnName.isBlank()) {
                    if(columnName.equals(sortedColumn))
                        ascending = !ascending;
                    else {
                        sortedColumn = columnName;
                        ascending = true;
                    }
                    externalGenomeDataProvider.setSorting(columnName, ascending);
                    currentPage = 1;
                    refreshTable();
                    updateTableHeader(columnIndex, ascending);
                }
            }
        });
    }

    private void updateTableHeader(int columnIndex, boolean ascending) {
        JTableHeader header = genomeDynamicSearchTable.getTableHeader();
        TableColumnModel columnModel = genomeDynamicSearchTable.getColumnModel();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            String columnName = genomeDynamicSearchTable.getColumnName(i);
            if (i == columnIndex) {
                column.setHeaderValue(columnName + (ascending ? " ▲" : " ▼"));
            } else {
                column.setHeaderValue(columnName);
            }
        }

        header.repaint();
    }

    private void refreshTable() {
        genomeDynamicSearchTableModel.setData(externalGenomeDataProvider.getPageData(currentPage, ROWS_PER_PAGE));
        resizeColumnWidths();
    }

    private void resizeColumnWidths() {
        for (int col = 0; col < genomeDynamicSearchTable.getColumnCount(); col++) {
            int width = 50, maxWidth=200;
            TableCellRenderer headerRenderer = genomeDynamicSearchTable.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    genomeDynamicSearchTable, genomeDynamicSearchTable.getColumnName(col), false, false, 0, col);
            width = Math.max(headerComp.getPreferredSize().width + 10, width);

            for (int row = 0; row < genomeDynamicSearchTable.getRowCount(); row++) {
                TableCellRenderer renderer = genomeDynamicSearchTable.getCellRenderer(row, col);
                Component comp = genomeDynamicSearchTable.prepareRenderer(renderer, row, col);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            genomeDynamicSearchTable.getColumnModel().getColumn(col).setPreferredWidth(Math.min(width, maxWidth));
        }
    }

    private void updatePagination() {
        // Update total pages count
        int totalResults = externalGenomeDataProvider.getTotalGenomes();
        totalPages = (int) Math.ceil(totalResults / (double) ROWS_PER_PAGE);

        // Update result range label on the left
        String resultRange = String.format("%d-%d of %d results",
                (currentPage - 1) * ROWS_PER_PAGE + 1,
                Math.min(currentPage * ROWS_PER_PAGE, totalResults),
                totalResults);
        resultsLabel.setText(resultRange);

        // Clear the right side pagination panel before re-adding components
        rightPaginationPanel.removeAll();

        // Previous button
        JLabel prev = createPageLabel("<", currentPage > 1, () -> updatePage(currentPage - 1));
        rightPaginationPanel.add(prev);

        // Display a small range of page numbers (e.g., current - 1, current, current + 1)
        int startPage = Math.max(1, currentPage - 1);
        int endPage = Math.min(totalPages, currentPage + 1);
        for (int i = startPage; i <= endPage; i++) {
            final int pageNo = i;
            JLabel label = createPageLabel(String.valueOf(pageNo), true, () -> updatePage(pageNo));
            if (pageNo == currentPage) {
                label.setOpaque(true);
                label.setBackground(new Color(30, 80, 200));
                label.setForeground(Color.WHITE);
                label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            }
            rightPaginationPanel.add(label);
        }

        // Next button
        JLabel next = createPageLabel(">", currentPage < totalPages, () -> updatePage(currentPage + 1));
        rightPaginationPanel.add(next);

        // Repaint the pagination panel
        paginationWrapper.revalidate();
        paginationWrapper.repaint();
        refreshTable();
    }

    private void updatePage(int newPage) {
        if (newPage < 1 || newPage > totalPages) {
            return;
        }
        currentPage = newPage;
        refreshTable();
        updatePagination();
    }

    private JLabel createPageLabel(String text, boolean enabled, Runnable action) {
        JLabel label = new JLabel(text);
        label.setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        label.setForeground(enabled ? Color.BLACK : Color.GRAY);
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (enabled) {
                    action.run();
                }
            }
        });
        label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return label;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        searchText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        searchResultsTable = genomeDynamicSearchTable;
        clearSearchButton = new javax.swing.JButton();
        paginationWrapper = new javax.swing.JPanel();

        jLabel1.setText("Start typing a genome name:");

        searchText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchTextActionPerformed(evt);
            }
        });
        searchText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchTextKeyReleased(evt);
            }
        });

        searchResultsTable.setModel(genomeDynamicSearchTableModel);
        searchResultsTable.setSelectionBackground(new java.awt.Color(0, 51, 255));
        searchResultsTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(searchResultsTable);

        clearSearchButton.setText("Clear Search");
        clearSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSearchButtonActionPerformed(evt);
            }
        });

        paginationWrapper.setLayout(new java.awt.BorderLayout());
        resultsLabel = new javax.swing.JLabel();
        resultsLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
        paginationWrapper.add(resultsLabel, java.awt.BorderLayout.WEST);

        rightPaginationPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 5));
        paginationWrapper.add(rightPaginationPanel, java.awt.BorderLayout.EAST);

        updatePagination();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(paginationWrapper, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchText, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearSearchButton)
                        .addGap(0, 110, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(searchText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearSearchButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paginationWrapper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jScrollPane2.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void searchTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchTextActionPerformed

    private void clearSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSearchButtonActionPerformed
        searchText.setText("");
        externalGenomeDataProvider.search("");
        refreshTable();
        updatePagination();
    }//GEN-LAST:event_clearSearchButtonActionPerformed

    private void searchTextKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchTextKeyReleased
        String text = searchText.getText().trim();
        externalGenomeDataProvider.search(text);
        currentPage = 1;
        refreshTable();
        updatePagination();
    }//GEN-LAST:event_searchTextKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearSearchButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel paginationWrapper;
    private javax.swing.JTable searchResultsTable;
    private javax.swing.JTextField searchText;
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot;

import com.affymetrix.genometry.util.GeneralUtils;
import org.lorainelab.igb.protannot.model.InterProScanTableModel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Tarun
 */
public class PropertySheetHelper extends DefaultTableCellRenderer implements
        MouseListener, MouseMotionListener {

    private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private final Cursor defaultCursor = null;
    private final JTable table;

    public PropertySheetHelper(JTable table) {
        this.table = table;
    }

    @Override
    public int getHorizontalAlignment() {
        return SwingConstants.LEFT;
    }


    @Override
    public Component getTableCellRendererComponent(JTable table,
            Object obj, boolean isSelected, boolean hasFocus, int row, int column) {

        if (isURLField(row, column)) {

            String url = "<html> <a href='" + (String) obj + "'>"
                    + (String) obj + "</a> </html>)";

            return super.getTableCellRendererComponent(table, url,
                    isSelected, hasFocus, row, column);
        }

        return super.getTableCellRendererComponent(table, obj, isSelected,
                hasFocus, row, column);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        Point p = e.getPoint();
        int row = table.rowAtPoint(p);
        int column = table.columnAtPoint(p);

        if (e.getClickCount() >= 2) {
            Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection data = new StringSelection((String) table.getValueAt(row, column));
            system.setContents(data, null);
            return;
        }

        if (isURLField(row, column)) {
            GeneralUtils.browse((String) table.getValueAt(row, column));
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        int row = table.rowAtPoint(p);
        int column = table.columnAtPoint(p);

        if (isURLField(row, column)) {
            table.setCursor(handCursor);
        } else if (table.getCursor() != defaultCursor) {
            table.setCursor(defaultCursor);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    private boolean isURLField(int row, int column) {

        if (table.getModel() instanceof InterProScanTableModel) {
            try {
                URL url = new URL((String) table.getModel().getValueAt(row, column));
                return (column == InterProScanTableModel.URL_COLUMN);
            } catch (MalformedURLException ex) {
                return false;
            }
        } else {
            return (column != 0 && table.getValueAt(row, 0).equals("URL"));
        }
    }
}

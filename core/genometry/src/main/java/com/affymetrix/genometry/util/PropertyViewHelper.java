package com.affymetrix.genometry.util;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JTable;

/**
 *
 * @author hiralv
 */
public class PropertyViewHelper implements MouseListener, MouseMotionListener {

    private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private final Cursor defaultCursor = null;
    private final JTable table;

    public PropertyViewHelper(JTable table) {
        this.table = table;
        table.addMouseListener(this);
        table.addMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        Point p = e.getPoint();
        int row = table.rowAtPoint(p);
        int column = table.columnAtPoint(p);

        if (isURLField(row, column)) {
            GeneralUtils.browse((String) table.getValueAt(row, column));
        }

    }

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

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    private boolean isURLField(int row, int column) {

        if (row > table.getRowCount() || column > table.getColumnCount()
                || row < 0 || column < 0) {
            return false;
        }

        String value = (String) table.getValueAt(row, column);

        if (value.length() <= 0) {
            return false;
        }

        if (value.startsWith("<html>")) {
            return true;
        }

        return false;
    }
}

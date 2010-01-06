/* 
 * Derived from ColorRenderer in Sun's Tutorial "How to Use Tables".
 *
 * http://java.sun.com/docs/books/tutorial/uiswing/components/table.html#data
 */
package com.affymetrix.swing;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;

public final class ColorTableCellRenderer extends JLabel implements TableCellRenderer {
    private Border unselectedBorder = null;
    private Border selectedBorder = null;

    private boolean isBordered = true;

    private static final Border emptyBorder = new EmptyBorder(1, 1, 1, 1);

    public ColorTableCellRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    public Component getTableCellRendererComponent(
                            JTable table, Object color,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {
        Color newColor = (Color)color;
        setBackground(newColor);
                
        if (hasFocus) {

          Border focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
          if (isBordered) {
            // Make a compound border from the focus border and our own border
            
            Color c;
            if (table.isCellEditable(row, column)) {
              c = UIManager.getColor("Table.focusCellBackground");
            } else {
              c = table.getSelectionBackground();
            }
            Border insideBorder = BorderFactory.createMatteBorder(1,4,1,4, c);

            focusBorder = BorderFactory.createCompoundBorder(focusBorder, insideBorder);
          }

          setBorder(focusBorder);
        }
        else if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                              table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        } else {
          setBorder(emptyBorder);
        }
        
        return this;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genoviz.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author hiralv
 */
public class ButtonTableCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener{

	private final Object userObject;
    private final JButton button;
	private final List<ActionListener> listeners = new CopyOnWriteArrayList<ActionListener>();
	
	public ButtonTableCellEditor(Object userObject) {
        // Set up the editor (from the table's point of view),
        // which is a button.
		button = new JButton();
		button.addActionListener((ActionListener)this);
		this.userObject = userObject;
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
	public Object getCellEditorValue() {
		return userObject;
    }
	
    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
		
        return button;
    }

	public void addActionListener(ActionListener al){
		listeners.add(al);
	}
	
	public void actionPerformed(ActionEvent e) {
		ActionEvent event = new ActionEvent(userObject, e.getID(), e.getActionCommand());

		for(ActionListener al : listeners){
			al.actionPerformed(event);
		}
	}
}

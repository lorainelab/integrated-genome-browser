/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genoviz.swing;

import javax.swing.table.TableCellEditor;

/**
 *
 * @author dcnorris
 */
public abstract interface TableCellEditorRenderer extends TableCellEditor {

	abstract boolean isFullyEngaged();
}

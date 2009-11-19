package com.affymetrix.igb.view.load;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

class TrackPropertyTableModel extends AbstractTableModel {
	
	String featureName = "";
	Map<String, String> properties = null;
	Object[] keys = null;
	String[] values = null;
	
	public TrackPropertyTableModel(String featureName, Map<String, String> properties) {
		this.featureName = featureName;		
		this.properties = properties;
		if (properties != null) {
			this.keys = properties.keySet().toArray();
			values = new String[this.keys.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = properties.get(this.keys[i]);
			}
		}
	}
	
	public int getRowCount() {
		if (properties != null) {
			return properties.size();
		}
		return 0;
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		if (properties != null) {			
			if (col == 0) {					
				return  this.keys[row];					
			} else if (col == 1) {
				return this.values[row];					
			}							
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "Property";
		} else if (col == 1) {
			return "Value";
		} else {
			return null;
		}
	}

}

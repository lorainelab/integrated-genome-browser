package com.affymetrix.igb.bookmarks;

import java.util.*;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author nick
 */
public class BookmarkPropertyTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	
	// A list to determine what bookmark data should be listed in table.
	static protected final List<String> info_list = new ArrayList<String>(6);
	static {
		info_list.add("version");
		info_list.add("seqid");
		info_list.add("start");
		info_list.add("end");
		info_list.add("create");
		info_list.add("modified");
	}
	
	/**
	 * A silly little helper class that holds two strings. A String[2] array
	 * would work just as well.
	 */
	private static class Duple {

		public String a;
		public String b;

		public Duple(String a, String b) {
			this.a = a;
			this.b = b;
		}
	}
	
	private final List<Duple> duples = new ArrayList<Duple>(20);
	private final String[] names = {"Parameter", "Value"};
	
	/**
	 * The number of extra rows to display to give users room to enter extra
	 * data into the table.
	 */
	public final static int EXTRA_ROWS = 0;
	
	/**
	 * Fills the table model with data from the Map. Some extra empty rows may
	 * also be appended to the table to allow room for extra data.
	 */
	public void setValuesFromMap(Map<String, String[]> map) {
		if (map == null) {
			throw new IllegalArgumentException("Map was null");
		}
		duples.clear();
		List<String> properties = getProperties();
		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			String key = entry.getKey();
			String[] value = entry.getValue();
			if (shouldInclude(properties, key)) {
				if (value.length == 0) {
					duples.add(new Duple(key, ""));
				} else {
					for (int i = 0; i < value.length; i++) {
						Duple duple = new Duple(key, value[i]);
						duples.add(duple);
					}
				}
			}
		}
		for (int i = EXTRA_ROWS; i > 0; i--) {
			duples.add(new Duple("", ""));
		}
		fireTableDataChanged();
	}

	/**
	 * Returns the current contents of the table model as a Map. The returned
	 * Map will be a new map, not the same as the one passed in to
	 *  {@link #setValuesFromMap(Map)}. Any item with an empty key or value will
	 * not be included in the Map.
	 */
	Map<String, String[]> getValuesAsMap() {
		Map<String, String[]> m = new LinkedHashMap<String, String[]>();
		for (int i = 0; i < getRowCount(); i++) {
			String key = (String) getValueAt(i, 0);
			String value = (String) getValueAt(i, 1);
			Bookmark.addToMap(m, key, value);
		}
		return m;
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return duples.size();
	}

	public Object getValueAt(int row, int col) {
		if (row < duples.size()) {
			Duple duple = duples.get(row);
			if (col == 0) {
				return duple.a;
			} else if (col == 1) {
				return duple.b;
			}
		}
		return "";
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		String s = (aValue == null ? "" : aValue.toString());
		Duple duple = duples.get(row);
		if (col == 0) {
			duple.a = s;
		} else if (col == 1) {
			duple.b = s;
		}
		fireTableCellUpdated(row, col);
	}

	@Override
	public String getColumnName(int column) {
		return names[column];
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if (col == 1) {
			return true;
		}

		return false;
	}

	public void clear() {
		duples.clear();
		fireTableDataChanged();
	}
	
	protected List<String> getProperties(){
		return Collections.<String>emptyList();
	}
	
	protected boolean shouldInclude(List<String> properties, String key){
		return true;
	}
}

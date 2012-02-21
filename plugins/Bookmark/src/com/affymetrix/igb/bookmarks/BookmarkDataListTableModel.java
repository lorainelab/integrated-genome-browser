package com.affymetrix.igb.bookmarks;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author nick
 */
public class BookmarkDataListTableModel extends BookmarkInfoTableModel {

	@Override
	public void setValuesFromMap(Map<String, String[]> map) {
		if (map == null) {
			throw new IllegalArgumentException("Map was null");
		}
		duples = new ArrayList<Duple>();

		String key;
		String[] value;
		Duple duple;

		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			key = entry.getKey();
			value = entry.getValue();

			if (!info_list.contains(key)) {
				if (value.length == 0) {
					duples.add(new Duple(key, ""));
				} else {
					duple = new Duple(key, value[0]);
					duples.add(duple);
				}
			}
		}

		fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
}

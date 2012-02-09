package com.affymetrix.igb.bookmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nick
 */
public class BookmarkInfoTableModel extends BookmarkPropertyTableModel {

	private static final List<String> info_list = getInfoList();

	private static List<String> getInfoList() {
		List<String> infoList = new ArrayList<String>(20);
		infoList.add("version");
		infoList.add("seqid");
		infoList.add("start");
		infoList.add("end");
		infoList.add("loadresidues");
		infoList.add("create");
		infoList.add("modified");

		return infoList;
	}

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

			if (info_list.contains(key)) {
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

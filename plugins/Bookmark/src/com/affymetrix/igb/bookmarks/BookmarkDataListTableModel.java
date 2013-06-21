package com.affymetrix.igb.bookmarks;

import java.util.List;

/**
 *
 * @author nick
 */
public class BookmarkDataListTableModel extends BookmarkPropertyTableModel {
	private static final long serialVersionUID = 1L;

	@Override
	protected List<String> getProperties(){
		return info_list;
	}
	
	@Override
	protected boolean shouldInclude(List<String> properties, String key){
		return !properties.contains(key);
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
}

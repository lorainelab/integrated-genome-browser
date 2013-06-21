package com.affymetrix.igb.bookmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nick
 */
public class BookmarkInfoTableModel extends BookmarkPropertyTableModel {
	private static final long serialVersionUID = 1L;
	
	
	@Override
	protected List<String> getProperties(){
		return info_list;
	}
	
	@Override
	protected boolean shouldInclude(List<String> properties, String key){
		return properties.contains(key);
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
}

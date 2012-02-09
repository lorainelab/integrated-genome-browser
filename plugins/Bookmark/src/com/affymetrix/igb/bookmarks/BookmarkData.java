package com.affymetrix.igb.bookmarks;

import java.net.URL;
import java.util.*;
import javax.swing.*;

/**
 * 
 * @author nick
 */
public final class BookmarkData {

	private static BookmarkData singleton;
	private final BookmarkTableModel propertyModel;
	private final BookmarkTableModel infoModel;
	private final JTable propertyTable;
	private final JTable infoTable;

	public BookmarkData() {
		propertyModel = new BookmarkTableModel();
		propertyTable = new JTable(propertyModel);
		propertyTable.setCellSelectionEnabled(true);

		infoModel = new BookmarkTableModel();
		infoTable = new JTable(infoModel);
		infoTable.setCellSelectionEnabled(true);
	}

	public static synchronized BookmarkData getSingleton() {
		if (singleton == null) {
			singleton = new BookmarkData();
		}
		return singleton;
	}

	public JTable getPropertyTable() {
		return propertyTable;
	}

	public JTable getInfoTable() {
		return infoTable;
	}

	public BookmarkTableModel getInfoModel() {
		return infoModel;
	}

	public void setPropertyGUIFromBookmark(BookmarkList bl) {
		Bookmark bm = (Bookmark) bl.getUserObject();
		if (bm == null) {
			propertyModel.setPropertyValuesFromMap(Collections.<String, String[]>emptyMap());
		} else {
			URL url = bm.getURL();
			propertyModel.setPropertyValuesFromMap(Bookmark.parseParameters(url));
		}
	}

	public void setInfoGUIFromBookmark(BookmarkList bl) {
		Bookmark bm = (Bookmark) bl.getUserObject();
		if (bm == null) {
			infoModel.setInfoValuesFromMap(Collections.<String, String[]>emptyMap());
		} else {
			URL url = bm.getURL();
			infoModel.setInfoValuesFromMap(Bookmark.parseParameters(url));
		}
	}
}

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * 
 * @author nick
 */
public final class BookmarkData {

	private static BookmarkData singleton;
	private static BookmarkList bookmarkList;
	private final BookmarkPropertyTableModel propertyModel;
	private final BookmarkPropertyTableModel infoModel;
	private final JTable propertyTable;
	private final JTable infoTable;

	public BookmarkData() {
		propertyModel = new BookmarkPropertyTableModel();
		propertyTable = new JTable(propertyModel);
		propertyTable.setCellSelectionEnabled(true);
		propertyModel.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				setBookmarkFromPropertyGUI(bookmarkList);
			}
		});

		infoModel = new BookmarkInfoTableModel();
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

	public BookmarkPropertyTableModel getInfoModel() {
		return infoModel;
	}

	public void setPropertyGUIFromBookmark(BookmarkList bl) {
		bookmarkList = bl;
		setGUIFromBookmark(propertyModel, bl);
	}

	public void setInfoGUIFromBookmark(BookmarkList bl) {
		setGUIFromBookmark(infoModel, bl);
	}

	private void setGUIFromBookmark(BookmarkPropertyTableModel model, BookmarkList bl) {
		Bookmark bm = (Bookmark) bl.getUserObject();
		if (bm == null) {
			model.setValuesFromMap(Collections.<String, String[]>emptyMap());
		} else {
			URL url = bm.getURL();
			model.setValuesFromMap(Bookmark.parseParameters(url));
		}
	}

	public void setBookmarkFromPropertyGUI(BookmarkList bl) {
		Bookmark bm = (Bookmark) bl.getUserObject();
		URL url = bm.getURL();
		String url_base = bm.getURL().toExternalForm();
		int index = url_base.indexOf('?');
		if (index > 0) {
			url_base = url_base.substring(0, index);
		}

		String str = Bookmark.constructURL(url_base, propertyModel.getValuesAsMap());
		try {
			url = new URL(str);
			bm.setURL(url);
		} catch (MalformedURLException e) {
			JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, null);
			ErrorHandler.errorPanel(frame, "Error", "Cannot construct bookmark: " + e.getMessage(), null);
		}
	}
}

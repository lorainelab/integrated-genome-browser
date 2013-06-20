package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.shared.StyledJTable;
import java.net.URL;
import java.util.Collections;
import javax.swing.JTable;

/**
 * This class is designed to get and display one bookmark data to three
 * different tables(property table, information table and details table).
 *
 * @author nick
 */
public final class BookmarkData {

	private final BookmarkPropertyTableModel infoModel;
	private final BookmarkPropertyTableModel datalistModel;
	private final JTable infoTable;
	private final JTable datalistTable;

	/**
	 * Initialize bookmark tables and related models.
	 */
	public BookmarkData() {
		infoModel = new BookmarkInfoTableModel();
		infoTable = new StyledJTable(infoModel);

		datalistModel = new BookmarkDataListTableModel();
		datalistTable = new StyledJTable(datalistModel);
	}

	public JTable getInfoTable() {
		return infoTable;
	}

	public JTable getDataListTable() {
		return datalistTable;
	}

	public BookmarkPropertyTableModel getInfoModel() {
		return infoModel;
	}

	public BookmarkPropertyTableModel getDataListModel() {
		return datalistModel;
	}

	public void setInfoTableFromBookmark(BookmarkList bl) {
		setTableFromBookmark(infoModel, bl);
	}

	public void setDataListTableFromBookmark(BookmarkList bl) {
		setTableFromBookmark(datalistModel, bl);
	}

	/**
	 * Reset passed model data by passed bookmark list.
	 *
	 * @param model
	 * @param bl
	 */
	private void setTableFromBookmark(BookmarkPropertyTableModel model, BookmarkList bl) {
		Bookmark bm = (Bookmark) bl.getUserObject();
		if (bm == null) {
			model.setValuesFromMap(Collections.<String, String[]>emptyMap());
		} else {
			URL url = bm.getURL();
			model.setValuesFromMap(Bookmark.parseParameters(url));
		}
	}
}

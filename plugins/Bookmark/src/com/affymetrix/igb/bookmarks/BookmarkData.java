package com.affymetrix.igb.bookmarks;

import com.affymetrix.igb.shared.StyledJTable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * This class is designed to get and display one bookmark data to three
 * different tables(property table, information table and details table).
 *
 * @author nick
 */
public final class BookmarkData {

	private static BookmarkData singleton;
	private static BookmarkList bookmarkList;
	private final BookmarkPropertyTableModel propertyModel;
	private final BookmarkPropertyTableModel infoModel;
	private final BookmarkPropertyTableModel datalistModel;
	private final JTable propertyTable;
	private final JTable infoTable;
	private final JTable datalistTable;

	/**
	 * Initialize bookmark tables and related models.
	 */
	public BookmarkData() {
		propertyModel = new BookmarkPropertyTableModel();
		propertyTable = new StyledJTable(propertyModel);

		propertyModel.addTableModelListener(new TableModelListener() {

			/**
			 * Any values changed in property table will trigger to update info
			 * or data list table
			 */
			public void tableChanged(TableModelEvent e) {
				if (bookmarkList != null) {
					Bookmark bm = (Bookmark) bookmarkList.getUserObject();
					URL url = bm.getURL();
					String url_base = bm.getURL().toExternalForm();
					int index = url_base.indexOf('?');
					if (index > 0) {
						url_base = url_base.substring(0, index);
					}

					// record the modified time
					Map<String, String[]> props = propertyModel.getValuesAsMap();
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					Date date = new Date();
					props.put(Bookmark.MODIFIED, new String[]{dateFormat.format(date)});

					String str = Bookmark.constructURL(url_base, props);
					try {
						url = new URL(str);
					} catch (MalformedURLException ex) {
						Logger.getLogger(BookmarkData.class.getName()).log(Level.SEVERE, null, ex);
					}
					bm.setURL(url);

					BookmarkManagerView.getSingleton().thing.updateInfoOrDataTable();
				}
			}
		});

		infoModel = new BookmarkInfoTableModel();
		infoTable = new StyledJTable(infoModel);

		datalistModel = new BookmarkDataListTableModel();
		datalistTable = new StyledJTable(datalistModel);
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

	public JTable getDataListTable() {
		return datalistTable;
	}

	public BookmarkPropertyTableModel getInfoModel() {
		return infoModel;
	}

	public BookmarkPropertyTableModel getDataListModel() {
		return datalistModel;
	}

	public void setPropertyTableFromBookmark(BookmarkList bl) {
		bookmarkList = bl;
		setTableFromBookmark(propertyModel, bl);
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

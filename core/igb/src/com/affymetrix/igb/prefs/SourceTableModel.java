package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.ServerTypeI;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.view.load.GeneralLoadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author sgblanch
 * @version $Id: SourceTableModel.java 11460 2012-05-08 15:47:23Z imnick $
 */
public final class SourceTableModel extends AbstractTableModel implements PreferenceChangeListener {

	private static final long serialVersionUID = 1l;
	private final List<GenericServer> servers = new ArrayList<GenericServer>();

	public static enum SourceColumn {

		Refresh, Name, Type, URL, Enabled
	};
	public static final List<SortKey> SORT_KEYS;
	private ServerList serverList;
	private ArrayList<SourceColumn> tableColumns;

	static {
		List<SortKey> sortKeys = new ArrayList<SortKey>(2);
		sortKeys.add(new SortKey(SourceColumn.Name.ordinal(), SortOrder.ASCENDING));
		sortKeys.add(new SortKey(SourceColumn.Type.ordinal(), SortOrder.ASCENDING));

		SORT_KEYS = Collections.<SortKey>unmodifiableList(sortKeys);
	}

	public SourceTableModel(ServerList serverList) {
		super();
		this.serverList = serverList;
		init();
	}

	public void init() {
		tableColumns = new ArrayList<SourceColumn>();
		for (SourceColumn sourceColumn : SourceColumn.values()) {
			if (sourceColumn != SourceColumn.Type || serverList.hasTypes()) {
				tableColumns.add(sourceColumn);
			}
		}
		this.servers.clear();
		this.servers.addAll(serverList.getAllServers());
		this.fireTableDataChanged();
	}

	public int getRowCount() {
		return servers.size();
	}

	public int getColumnIndex(SourceColumn sourceColumn) {
		return tableColumns.indexOf(sourceColumn);
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (tableColumns.get(col)) {
			case Enabled:
				return Boolean.class;
			case Type:
				return ServerTypeI.class;
			default:
				return String.class;
		}
	}

	public int getColumnCount() {
		return tableColumns.size();
	}

	@Override
	public String getColumnName(int col) {
		return tableColumns.get(col).toString();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex >= tableColumns.size()) {
			System.out.println("row " + rowIndex + ", columnIndex " + columnIndex + " is out of range");
			return null;
		}
		switch (tableColumns.get(columnIndex)) {
			case Refresh:
				return "";
			case Name:
				return servers.get(rowIndex).serverName;
			case Type:
				return servers.get(rowIndex).serverType;
			case URL:
				return servers.get(rowIndex).URL;
			case Enabled:
				return servers.get(rowIndex).isEnabled();
			default:
				throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if (!servers.get(row).isDefault()
				&& col == tableColumns.indexOf(SourceColumn.Name)) {
			return true;
		}

		if (col == tableColumns.indexOf(SourceColumn.Enabled)) {
			return true;
		}

		if (servers.get(row).isEnabled() && col == tableColumns.indexOf(SourceColumn.Refresh)) {
			return true;
		}
		
		return false;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		final GenericServer server = servers.get(row);
		switch (tableColumns.get(col)) {
			case Refresh:
				// Plugin is used as a Server Object and its serverType is null
				if (server.serverType != null) {
					if (!server.isEnabled()
							|| DataLoadPrefsView.getSingleton().confirmRefresh()) {
						DataLoadPrefsView.getSingleton().updateSource(server.URL,
								server.serverType, server.serverName, server.URL, server.mirrorURL);
					}
				} else {
					BundleRepositoryPrefsView.getSingleton().updateSource(server.URL,
							server.serverType, server.serverName, server.URL, server.mirrorURL);
				}
				break;
			case Enabled:
				if (((Boolean) value).booleanValue()) {
					discoverServer(server);
					server.setEnabled(true);
				} else {
					if (server.serverType == null || DataLoadPrefsView.getSingleton().confirmDelete()) {
						serverList.fireServerInitEvent(server, LoadUtils.ServerStatus.NotResponding, true);
						server.setEnabled(false);
					}
				}
				break;
			case Name:
				server.setName((String) value);
				break;
			case URL:
				//do nothing
				break;
			case Type:
				//do nothing
				break;
			default:
				throw new IllegalArgumentException("columnIndex " + col + " not editable");
		}

		this.fireTableDataChanged();
	}

	private void discoverServer(final GenericServer server) {
		CThreadWorker<Void, Void> worker = new CThreadWorker<Void, Void>("discover server") {

			@Override
			protected Void runInBackground() {
				GeneralLoadUtils.discoverServer(server);
				return null;
			}

			@Override
			protected void finished() {
			}
		};
		CThreadHolder.getInstance().execute(server, worker);
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		/*
		 * It is easier to rebuild than try and find out what changed
		 */
		this.init();
	}

	public void switchRows(int rowIndex) {
		if (rowIndex < 0 || rowIndex >= servers.size() - 1) {
			return;
		}
		GenericServer firstServer = servers.get(rowIndex);
		servers.set(rowIndex, servers.get(rowIndex + 1));
		servers.set(rowIndex + 1, firstServer);
		this.fireTableDataChanged();
	}

	public List<GenericServer> getServers() {
		return servers;
	}
}

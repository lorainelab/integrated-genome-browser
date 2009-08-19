package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.general.ServerList;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.table.AbstractTableModel;


/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class SourceTableModel extends AbstractTableModel implements PreferenceChangeListener {
	private static final long serialVersionUID = 1l;
	private List<GenericServer> servers = new ArrayList<GenericServer>();

	public static enum SourceColumn { Name, Type, URL, Login, Password, Enabled };

	public SourceTableModel() {
		init();
	}

	public void init() {
		this.servers.clear();
		this.servers.addAll(ServerList.getAllServers());
		this.fireTableDataChanged();
	}

	public int getRowCount() {
		return servers.size();
	}
	
	@Override
    public Class<?> getColumnClass(int c) {
		switch (SourceColumn.valueOf(this.getColumnName(c))) {
			case Enabled:
				return Boolean.class;
			default:
				return String.class;
		}
    }


	public int getColumnCount() { return SourceColumn.values().length; }

	@Override
	public String getColumnName(int col) { return SourceColumn.values()[col].toString(); }

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (SourceColumn.valueOf(this.getColumnName(columnIndex))) {
			case Enabled:
				return servers.get(rowIndex).enabled;
			case Name:
				return servers.get(rowIndex).serverName;
			case Type:
				return servers.get(rowIndex).serverType;
			case URL:
				return servers.get(rowIndex).URL;
			case Login:
				return servers.get(rowIndex).login;
			case Password:
				return servers.get(rowIndex).password != null && !servers.get(rowIndex).password.equals("") ? "****" : ""; 				
			default:
				throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
		}
	}

	@Override
    public boolean isCellEditable(int row, int col) {
		SourceColumn c = SourceColumn.valueOf(this.getColumnName(col));
		return c != SourceColumn.Type && ServerList.inServerPrefs(servers.get(row).URL);
    }
    

	@Override
    public void setValueAt(Object value, int row, int col) {
        GenericServer server = servers.get(row);
        String existingDirectoryOrURL = server.URL;
        
        switch (SourceColumn.valueOf(this.getColumnName(col))) {
        case Enabled:
			server.enabled = Boolean.class.cast(value);
			break;
		case Name:
			server.serverName = String.class.cast(value);
			break;
		case Type:
			if (server.serverType.equals(LoadUtils.ServerType.QuickLoad)) {
				server.serverType = LoadUtils.ServerType.QuickLoad;
			} else if  (server.serverType.equals(LoadUtils.ServerType.DAS)) {
				server.serverType = LoadUtils.ServerType.DAS;
			} else if  (server.serverType.equals(LoadUtils.ServerType.DAS2)) {
				server.serverType = LoadUtils.ServerType.DAS2;
			}
			break;
		case URL:
			server.URL = String.class.cast(value);
			break;
		case Login:
			server.login = String.class.cast(value);
			break;
		case Password:
			server.password = String.class.cast(value);
			break;
		
		default:
			throw new IllegalArgumentException("columnIndex " + col + " is out of range");
        }
        
		changePreference(existingDirectoryOrURL, server);
    }
    

	private void changePreference(String existingDirectoryOrURL, GenericServer server) {

		if (!existingDirectoryOrURL.equals(server.URL)) {
			ServerList.removeServerFromPrefs(server.URL);
		}
		ServerList.addServerToPrefs(server);
		
		this.fireTableDataChanged();
		
		
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		/* It is easier to rebuild than try and find out what changed */
		this.init();
		this.fireTableDataChanged();
	}

}

package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.util.LoadUtils;
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
	static final long serialVersionUID = 1l;
	static final String[] headings = {"Name", "Type", "URL", "Login", "Password", "Enabled"};
	
	public static final int NAME     = 0;
	public static final int TYPE     = 1;
	public static final int URL      = 2;
	public static final int LOGIN    = 3;
	public static final int PASSWORD = 4;
	public static final int ENABLED  = 5;
	
	
	private List<GenericServer> servers = new ArrayList<GenericServer>();

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
		switch (c) {
		case ENABLED:
			return Boolean.class;
		case NAME:
			return String.class;
		case TYPE:
			return String.class;
		case URL:
			return String.class;
		case LOGIN:
			return String.class;
		case PASSWORD:
			return String.class;
		default:
			throw new IllegalArgumentException("col " + c + " is out of range");
	}
    }


	public int getColumnCount() { return 6; }

	@Override
	public String getColumnName(int col) { return headings[col]; }

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case ENABLED:
				return servers.get(rowIndex).enabled;
			case NAME:
				return servers.get(rowIndex).serverName;
			case TYPE:
				return servers.get(rowIndex).serverType;
			case URL:
				return servers.get(rowIndex).URL;
			case LOGIN:
				return servers.get(rowIndex).login;
			case PASSWORD:
				return servers.get(rowIndex).password != null && !servers.get(rowIndex).password.equals("") ? "****" : ""; 				
			default:
				throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
		}
	}

	@Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }
    

	@Override
    public void setValueAt(Object value, int row, int col) {
        GenericServer server = servers.get(row);
        String existingDirectoryOrURL = server.URL;
        String existingServerType = server.serverType.toString();
        
        switch (col) {
        case ENABLED:
			server.enabled = Boolean.class.cast(value);
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		case NAME:
			server.serverName = String.class.cast(value);
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		case TYPE:
			if (server.serverType.equals(LoadUtils.ServerType.QuickLoad)) {
				server.serverType = LoadUtils.ServerType.QuickLoad;
			} else if  (server.serverType.equals(LoadUtils.ServerType.DAS)) {
				server.serverType = LoadUtils.ServerType.DAS;
			} else if  (server.serverType.equals(LoadUtils.ServerType.DAS2)) {
				server.serverType = LoadUtils.ServerType.DAS2;
			}
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		case URL:
			server.URL = String.class.cast(value);
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		case LOGIN:
			server.login = String.class.cast(value);
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		case PASSWORD:
			server.password = String.class.cast(value);
	        changePreference(existingDirectoryOrURL, existingServerType, server);
			return;
		
		default:
			throw new IllegalArgumentException("columnIndex " + col + " is out of range");
        }
        

    }
    

	private void changePreference(String existingDirectoryOrURL, String existingServerType, GenericServer server) {

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

package com.affymetrix.igb.prefs;

import com.affymetrix.genometry.util.LoadUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.util.StringEncrypter;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import com.affymetrix.igb.view.DataLoadPrefsView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.table.AbstractTableModel;

import static com.affymetrix.igb.IGBConstants.UTF8;

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
	
    public Class getColumnClass(int c) {
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

    public boolean isCellEditable(int row, int col) {
        return true;
    }
    

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

		Preferences prefServers = UnibrowPrefsUtil.getServersNode();
		Preferences individualServerPref = prefServers.node(existingServerType);
		try {
			// Remove the entry from the key-value pair
			individualServerPref.remove(URLEncoder.encode(existingDirectoryOrURL, UTF8));
			
			// Now add the key-value pair of URL and server name
			individualServerPref.put(URLEncoder.encode(server.URL, UTF8), server.serverName);
			
			// Under a child node called 'login' add the key-value pair of URL and login
			individualServerPref.node("login").put(URLEncoder.encode(server.URL, UTF8), server.login != null ? server.login : "");

			// Encrypt the password
			String passwordEncrypted = "";
			StringEncrypter encrypter = null;
			try {
				encrypter = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME);
				passwordEncrypted = encrypter.encrypt(server.password != null ? server.password : "");
			} catch (Exception e) {
			}
			
			// Under a child node called 'password' add the key-value pair of URL and encrypted password
			individualServerPref.node("password").put(URLEncoder.encode(server.URL, UTF8), passwordEncrypted);

			// Under a child node called 'enabled' add the key-value pair of URL and enabled boolean
			individualServerPref.node("enabled").put(URLEncoder.encode(server.URL, UTF8), new Boolean(server.enabled).toString());
			
			individualServerPref.flush();
			
		} catch (BackingStoreException ex) {
			Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
		} catch (UnsupportedEncodingException e) {
			Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, e);
		}
		this.fireTableDataChanged();
		
		
	}
	

	private GenericServer getServer(int row) {
		return servers.get(row);
	}





	public void preferenceChange(PreferenceChangeEvent evt) {
		/* It is easier to rebuild than try and find out what changed */
		this.init();
		this.fireTableDataChanged();
	}

}

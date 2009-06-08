package com.affymetrix.igb.prefs;

import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public final class SourceTableModel extends AbstractTableModel implements PreferenceChangeListener {
	static final long serialVersionUID = 1l;
	static final String[] headings = {"Name", "Type", "URL"};
	
	private List<Server> servers = new ArrayList<Server>();
	private static List<Server> oldStyleServers = new ArrayList<Server>();

	public SourceTableModel() {
		try {
			init();
			for (String name : UnibrowPrefsUtil.getServersNode().childrenNames()) {
				Preferences node = UnibrowPrefsUtil.getServersNode().node(name);
				node.addPreferenceChangeListener(this);
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(SourceTableModel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void init() {
		this.servers.clear();
		this.servers.addAll(oldStyleServers);
		try {
			for (String name : UnibrowPrefsUtil.getServersNode().childrenNames()) {
				Preferences node = UnibrowPrefsUtil.getServersNode().node(name);
				String[] keys = node.keys();
				for (String url : keys) {
					servers.add(new Server(node.get(url, ""), URLDecoder.decode(url, "UTF-8"), name));
				}
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(SourceTableModel.class.getName()).log(Level.SEVERE, null, ex);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(SourceTableModel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public int getRowCount() { return servers.size(); }

	public int getColumnCount() { return 3; }

	@Override
	public String getColumnName(int col) { return headings[col]; }

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return servers.get(rowIndex).name;
			case 1:
				return servers.get(rowIndex).type;
			case 2:
				return servers.get(rowIndex).url;
			default:
				throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
		}
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		/* It is easier to rebuild than try and find out what changed */
		this.init();
		this.fireTableDataChanged();
	}

	/**
	 * Nasty hack to list servers read by XmlPrefsParser.  This method
	 * assumes two things:
	 * <ol>
	 * <li>Servers are added before SourcesTableModel is instantiated</li>
	 * <li>List of servers added will never be changed at runtime</li>
	 * </ol>
	 * <p />
	 * This is (hopefully) a temporary hack which will go away once
	 * there is one ServerList class to rule them all.
	 * 
	 * @param name the name of the server
	 * @param url the url of the server
	 * @param type the type of server
	 */
	public static void add(String name, String url, String type) {
		oldStyleServers.add(new Server(name, url, type));
	}

	private static final class Server {
		protected final String name, url, type;

		protected Server(String name, String url, String type) {
			this.name = name;
			this.url = url;
			this.type = type;
		}
	}
}

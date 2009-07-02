package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
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
	
	private List<GenericServer> servers = new ArrayList<GenericServer>();

	public SourceTableModel() {
		init();
	}

	private void init() {
		this.servers.clear();
		this.servers.addAll(ServerList.getServers());
	}

	public int getRowCount() {
		//return servers.size();
		return servers.size();
	}

	public int getColumnCount() { return 3; }

	@Override
	public String getColumnName(int col) { return headings[col]; }

	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return servers.get(rowIndex).serverName;
			case 1:
				return servers.get(rowIndex).serverType;
			case 2:
				return servers.get(rowIndex).URL;
			default:
				throw new IllegalArgumentException("columnIndex " + columnIndex + " is out of range");
		}
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		/* It is easier to rebuild than try and find out what changed */
		this.init();
		this.fireTableDataChanged();
	}

}

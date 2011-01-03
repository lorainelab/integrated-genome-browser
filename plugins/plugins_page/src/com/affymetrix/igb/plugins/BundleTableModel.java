package com.affymetrix.igb.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class BundleTableModel extends DefaultTableModel implements Constants {
	private static final long serialVersionUID = 1L;
	private static final ArrayList<BundleColumn> columns = new ArrayList<BundleColumn>();
	static {
		columns.add(new BundleColumn() { // active
			@Override
			public String getTitle() { return PluginsView.BUNDLE.getString("activeColumn"); }
			@Override
			public boolean isEditable() { return true; }
			@Override
			public Object getValue(Bundle bundle) { return bundle.getState() == Bundle.ACTIVE; }
			@Override
			public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {
				if (bundle.getState() == Bundle.ACTIVE) {
					try {
						pluginsHandler.clearError();
						bundle.stop();
					}
					catch (BundleException bex) {
						pluginsHandler.displayError(bex.getMessage());
					}
				}
				else if (bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED)  {
					try {
						pluginsHandler.clearError();
						bundle.start();
					}
					catch (BundleException bex) {
						pluginsHandler.displayError(bex.getMessage());
//						Throwable cause = bex;
//						int preventLoop = 0;
//						while (cause.getCause() != null && !cause.equals(cause.getCause()) && preventLoop < 16) {
//							cause = bex.getCause();
//							preventLoop++;
//						}
//						displayError(cause.getMessage());
					}
				}
			}
		});
	columns.add(new BundleColumn() { // symbolic name
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_SYMBOLICNAME); }
		@Override
		public Object getValue(Bundle bundle) { return bundle.getSymbolicName(); }
	});
	columns.add(new BundleColumn() { // description
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_DESCRIPTION); }
		@Override
		public Object getValue(Bundle bundle) {
			Object description = bundle.getHeaders().get(BUNDLE_DESCRIPTION);
			return description == null ? "" : description.toString();
		}
	});
	columns.add(new BundleColumn() { // version
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_VERSION); }
		@Override
		public Object getValue(Bundle bundle) { return bundle.getVersion().toString(); }
	});
	columns.add(new BundleColumn() { // latest version
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString("latestVersionColumn"); }
		@Override
		public Object getValue(Bundle bundle) { return (pluginsHandler.isUpdatable(bundle) ? "<html><b><u>" : "") + pluginsHandler.getLatestVersion(bundle) + (pluginsHandler.isUpdatable(bundle) ? "</u></b></html>" : "");}
	});
	columns.add(new BundleColumn() { // location
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString("locationColumn"); }
		@Override
		public Object getValue(Bundle bundle) { return pluginsHandler.isTier2Bundle(bundle) ? PluginsView.BUNDLE.getString("preinstalled") : bundle.getLocation(); }
	});
	columns.add(new BundleColumn() { // install
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString("installColumn"); }
		@Override
		public boolean isEditable() { return true; }
		@Override
		public Object getValue(Bundle bundle) { return bundle.getState() != Bundle.UNINSTALLED; }
		@Override
		public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {
			if (bundle.getState() == Bundle.UNINSTALLED) {
				pluginsHandler.installBundle(bundle);
			}
			else {
				try {
					bundle.uninstall();
				}
				catch (BundleException bex) {
					pluginsHandler.displayError(bex.getMessage());
				}
			}
		}
		public boolean tier2OK() { return false; }
	});
	}
	private static IPluginsHandler pluginsHandler;

	public static void setPluginsHandler(IPluginsHandler _pluginsHandler) {
		pluginsHandler = _pluginsHandler;
	}

	public BundleTableModel() {
		super();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columns.get(columnIndex).getCellClass();
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columns.get(columnIndex).getTitle();
	}

	@Override
	public int getRowCount() {
		if (pluginsHandler.getFilteredBundles() == null) {
			return 0;
		}
		return pluginsHandler.getFilteredBundles().size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columns.get(columnIndex).getValue(pluginsHandler.getFilteredBundles().get(rowIndex));
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		Bundle bundle = pluginsHandler.getFilteredBundles().get(rowIndex);
		return columns.get(columnIndex).isEditable() &&
			(columns.get(columnIndex).tier2OK() || !pluginsHandler.isTier2Bundle(bundle));
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		columns.get(columnIndex).setValue(pluginsHandler.getFilteredBundles().get(rowIndex), aValue, pluginsHandler);
	}

	public List<Integer> getCheckboxColumnIndexes() {
		ArrayList<Integer> checkboxColumns = new ArrayList<Integer>();
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).isEditable()) {
				checkboxColumns.add(i);
			}
		}
		return checkboxColumns;
	}

	public int getLatestColumnIndex() {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getTitle().equals(PluginsView.BUNDLE.getString("latestVersionColumn"))) {
				return i;
			}
		}
		return -1;
	}
}

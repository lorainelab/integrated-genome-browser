package com.affymetrix.igb.plugins;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class BundleTableModel extends DefaultTableModel implements Constants {
	private static final long serialVersionUID = 1L;
	private static final int WIDE_COLUMN_MULTIPLIER = 5;
	private static final int NARROW_COLUMN = 60;

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
			public void formatColumn(JTable jTable, TableColumn tc) {
				tc.setCellEditor(jTable.getDefaultEditor(Boolean.class)); 
				tc.setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
				tc.setMinWidth(NARROW_COLUMN);
				tc.setMaxWidth(NARROW_COLUMN);
				tc.setPreferredWidth(NARROW_COLUMN);
			}
		});
	columns.add(new BundleColumn() { // symbolic name
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_SYMBOLICNAME); }
		@Override
		public Object getValue(Bundle bundle) {
//			Dictionary headers = bundle.getHeaders();
//			String bundleDocURL = (String)headers.get(BUNDLE_DOCURL);
			return bundle.getSymbolicName();
		}
	});
	columns.add(new BundleColumn() { // description
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_DESCRIPTION); }
		@Override
		public Object getValue(Bundle bundle) {
			Object description = bundle.getHeaders().get(BUNDLE_DESCRIPTION);
			return description == null ? "" : description.toString();
		}
		public void formatColumn(JTable jTable, TableColumn tc) {
			tc.setPreferredWidth(tc.getPreferredWidth() * WIDE_COLUMN_MULTIPLIER);
		}
	});
	columns.add(new BundleColumn() { // version
		@Override
		public String getTitle() { return PluginsView.BUNDLE.getString(BUNDLE_VERSION); }
		@Override
		public Object getValue(Bundle bundle) { return new VersionInfo(bundle, pluginsHandler);
		}
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
		public void formatColumn(JTable jTable, TableColumn tc) {
			tc.setCellEditor(jTable.getDefaultEditor(Boolean.class)); 
			tc.setCellRenderer(jTable.getDefaultRenderer(Boolean.class));
			tc.setCellRenderer(
					new TableCellRenderer() {
						@Override
						public Component getTableCellRendererComponent(JTable jTable, Object value,
								boolean isSelected, boolean hasFocus, int row, int column) {
							TableCellRenderer tableCellRenderer = pluginsHandler.isTier2Bundle(pluginsHandler.getBundleAtRow(row)) ?
								new DefaultTableCellRenderer.UIResource() {
									private static final long serialVersionUID = 1L;
									public void setValue(Object value) {
										super.setValue(value);
										setText(PluginsView.BUNDLE.getString("builtIn"));
									}
								} : 
								jTable.getDefaultRenderer(Boolean.class);
							return tableCellRenderer.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, column);
						}
					}
			);
			tc.setMinWidth(NARROW_COLUMN);
			tc.setMaxWidth(NARROW_COLUMN);
			tc.setPreferredWidth(NARROW_COLUMN);
		}
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
		return pluginsHandler.getFilteredBundleCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columns.get(columnIndex).getValue(pluginsHandler.getFilteredBundle(rowIndex));
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		Bundle bundle = pluginsHandler.getFilteredBundle(rowIndex);
		return columns.get(columnIndex).isEditable() &&
			(columns.get(columnIndex).tier2OK() || !pluginsHandler.isTier2Bundle(bundle));
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		columns.get(columnIndex).setValue(pluginsHandler.getFilteredBundle(rowIndex), aValue, pluginsHandler);
	}

	public int getColumnIndex(String key) {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getTitle().equals(PluginsView.BUNDLE.getString(key))) {
				return i;
			}
		}
		return -1;
	}

	public void setJTable(JTable jTable) {
		for (int i = 0; i < columns.size(); i++) {
			BundleColumn bundleColumn = columns.get(i);
			TableColumn checkboxColumn = jTable.getColumnModel().getColumn(i);
			bundleColumn.formatColumn(jTable, checkboxColumn);
		}
	}
}

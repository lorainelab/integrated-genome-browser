package com.affymetrix.igb.plugins;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleTableModel extends DefaultTableModel implements Constants {
	private static final long serialVersionUID = 1L;
	private static final int WIDE_COLUMN_MULTIPLIER = 5;
	private static final int NARROW_COLUMN = 60;

	public static class NameInfoPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final HashMap<Bundle, NameInfoPanel> PANEL_MAP = new HashMap<Bundle, NameInfoPanel>(); // kludge
		private final JLabel text;
		private final JLabel icon;

		public static NameInfoPanel getPanel(Bundle bundle) {
			return PANEL_MAP.get(bundle);
		}

		public NameInfoPanel(Bundle bundle) {
			super();
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			text = new JLabel(bundle.getSymbolicName() + " ");
			add(text);
			if (bundle.getHeaders().get(Constants.BUNDLE_DOCURL) != null) {
				icon = new JLabel(pluginsHandler.getIcon("info"));
				add(icon);
			}
			else {
				icon = null;
			}
			PANEL_MAP.put(bundle, this);
		}

        public boolean isOnInfoIcon(int x, int y) {
        	if (icon == null) {
        		return false;
        	}
        	Rectangle iconBounds = icon.getBounds();
        	return
        		x >= iconBounds.getX() && x <= iconBounds.getX() + icon.getWidth() &&
        		y >= iconBounds.getY() && y <= iconBounds.getY() + icon.getHeight();
        }

        public String toString() {
        	return text.getText() + " " + (icon != null);
        }
	}

	public static class NameInfoRenderer implements TableCellRenderer, UIResource {
		private static final long serialVersionUID = 1L;
		private static final HashMap<Bundle, NameInfoPanel> PANEL_MAP = new HashMap<Bundle, NameInfoPanel>(); // kludge
		private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

		public static NameInfoPanel getPanel(Bundle bundle) {
			return PANEL_MAP.get(bundle);
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			NameInfoPanel nameInfoPanel = new NameInfoPanel((Bundle)value);
			if (isSelected) {
				nameInfoPanel.setForeground(table.getSelectionForeground());
				nameInfoPanel.setBackground(table.getSelectionBackground());
			} else {
				nameInfoPanel.setForeground(table.getForeground());
				nameInfoPanel.setBackground(table.getBackground());
			}
            if (hasFocus) {
            	nameInfoPanel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
            	nameInfoPanel.setBorder(noFocusBorder);
            }

            return nameInfoPanel;
		}
	}

	private static abstract class BundleColumn {
		public abstract String getTitle();
		public Class<?> getCellClass() { return JLabel.class; }
		public boolean isEditable() { return false; }
		public abstract Object getValue(Bundle bundle);
		public void setValue(Bundle bundle, Object aValue, IPluginsHandler pluginsHandler) {}
		public boolean tier2OK() { return true; }
		public void formatColumn(JTable jTable, TableColumn tc) {}
	}

	private static class VersionInfo {
		private final Bundle bundle;
		private final IPluginsHandler pluginsHandler;
		public VersionInfo(Bundle bundle, IPluginsHandler pluginsHandler) {
			super();
			this.bundle = bundle;
			this.pluginsHandler = pluginsHandler;
		}
		public Version getVersion() {
			return bundle.getVersion();
		}
		public Version getLatestVersion() {
			return pluginsHandler.getLatestVersion(bundle);
		}
		public String toString() {
			return pluginsHandler.isUpdatable(bundle) ?
					"<html>" + getVersion() + " (<b>" + getLatestVersion() + "</b>)</html>)" :
					"" + getVersion();
		}
	}

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
			@Override
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
		public Class<?> getCellClass() { return NameInfoPanel.class; }
		@Override
		public Object getValue(Bundle bundle) { return bundle; }
		@Override
		public void formatColumn(JTable jTable, TableColumn tc) {
			tc.setCellRenderer(new NameInfoRenderer());
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
		@Override
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
		@Override
		public boolean tier2OK() { return false; }
		@Override
		public void formatColumn(JTable jTable, TableColumn tc) {
			tc.setCellEditor(jTable.getDefaultEditor(Boolean.class)); 
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

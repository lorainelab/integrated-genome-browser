package com.affymetrix.igb.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.bundlerepository.impl.wrapper.RepositoryAdminWrapper;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

public class PluginsView extends JPanel implements Constants {
	private static final long serialVersionUID = 8940192921109810816L;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
	private int ACTIVATE_COLUMN_INDEX;
	private int INSTALL_COLUMN_INDEX;

	private BundleContext bundleContext;
	private final JTable bundleTable;
	private final JCheckBox installedBundlesCheckbox;
	private final JCheckBox uninstalledBundlesCheckbox;
	private final JLabel errors;
	private List<String> requiredBundles;
	private Resource[] allResourceArray = null;
	private ArrayList<Resource> uninstalledResources = null;
	private boolean isShowInstalledBundles = true;
	private boolean isShowUninstalledBundles = false;
	private BundleTableModel bundleTableModel;
	private RepositoryAdmin repoAdmin;
	private BundleListener bundleListener;

	public PluginsView() {
		super();
		ACTIVATE_COLUMN_INDEX = Arrays.asList(COLUMN_NAMES).indexOf(ACTIVATE_COLUMN);
		INSTALL_COLUMN_INDEX = Arrays.asList(COLUMN_NAMES).indexOf(INSTALL_COLUMN);
		this.setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		installedBundlesCheckbox = new JCheckBox(BUNDLE.getString("pluginsInstalledBundles"));
		installedBundlesCheckbox.setToolTipText(BUNDLE.getString("pluginsInstalledBundlesTooltip"));
		installedBundlesCheckbox.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					isShowInstalledBundles = !isShowInstalledBundles;
					reloadBundleTable();
					clearError();
				}
			}
		);
		installedBundlesCheckbox.setSelected(true);
		buttonPanel.add(installedBundlesCheckbox);
		uninstalledBundlesCheckbox = new JCheckBox(BUNDLE.getString("pluginsUninstalledBundles"));
		uninstalledBundlesCheckbox.setToolTipText(BUNDLE.getString("pluginsUninstalledBundlesTooltip"));
		uninstalledBundlesCheckbox.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					isShowUninstalledBundles = !isShowUninstalledBundles;
					if (isShowUninstalledBundles) {
						if (allResourceArray == null) {
							loadAllBundlesInfo();
							filterUninstalledResources();
						}
						hideActivateColumn();
					}
					else {
						showActivateColumn();
					}
					reloadBundleTable();
					clearError();
				}
			}
		);
		buttonPanel.add(uninstalledBundlesCheckbox);
		buttonPanel.add(new JLabel("      "));
		errors = new JLabel(BUNDLE.getString("pluginsOSGiNotLoaded"));
		errors.setForeground(Color.RED);
		buttonPanel.add(errors);
		bundleTableModel = new BundleTableModel();
		bundleTable = new JTable(bundleTableModel);
		TableColumn installTableColumn = bundleTable.getColumnModel().getColumn(INSTALL_COLUMN_INDEX); 
		formatCheckboxColumn(installTableColumn);
		TableColumn activeTableColumn = bundleTable.getColumnModel().getColumn(ACTIVATE_COLUMN_INDEX); 
		formatCheckboxColumn(activeTableColumn);

		this.add("Center", new JScrollPane(bundleTable));
		this.add("South", buttonPanel);

		bundleListener = 
	    	new BundleListener() {
				public void bundleChanged(BundleEvent arg0) {
					if (bundleContext != null) {
						reloadBundleTable();
					}
				}
			};
	}

	public void setRequiredBundles(String[] requiredBundlesArray) {
		requiredBundles = Arrays.asList(requiredBundlesArray);
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
//		ServiceReference sr = bundleContext.getServiceReference(org.osgi.service.obr.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
//		ServiceReference sr = bundleContext.getServiceReference(org.apache.felix.bundlerepository.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
		repoAdmin = new RepositoryAdminWrapper((org.apache.felix.bundlerepository.RepositoryAdmin) new RepositoryAdminImpl(bundleContext, new Logger(bundleContext)));
		bundleContext.addBundleListener(bundleListener);
		clearError();
		reloadBundleTable();
	}

	public void deactivate() {
		isShowInstalledBundles = false;
		isShowUninstalledBundles = false;
		bundleContext.removeBundleListener(bundleListener);
		bundleContext = null;
	}

	private void formatCheckboxColumn(TableColumn tc) {
		tc.setCellEditor(bundleTable.getDefaultEditor(Boolean.class)); 
		tc.setCellRenderer(bundleTable.getDefaultRenderer(Boolean.class));
		tc.setMinWidth(60);
		tc.setMaxWidth(60);
		tc.setPreferredWidth(60);
	}

	private void showActivateColumn() {
		TableColumn activeTableColumn = new TableColumn(ACTIVATE_COLUMN_INDEX);
		formatCheckboxColumn(activeTableColumn);
		bundleTable.addColumn(activeTableColumn);
	}

	private void hideActivateColumn() {
		TableColumn tcol = bundleTable.getColumnModel().getColumn(ACTIVATE_COLUMN_INDEX);
		bundleTable.removeColumn(tcol);
	}

	public void clearError() {
		errors.setText("");
	}

	public void displayError(String errorText) {
		errors.setText(errorText);
	}

	private void loadAllBundlesInfo() {
		allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
	}

	private void filterUninstalledResources() {
		if (allResourceArray == null) {
			loadAllBundlesInfo();
			filterUninstalledResources();
		}
		uninstalledResources = new ArrayList<Resource>();
		for (Resource resource : allResourceArray) {
			@SuppressWarnings("unchecked")
			Map<String, Object> bundleInfo = resource.getProperties();
			Bundle bundle = getBundleLoaded((String)bundleInfo.get(Resource.SYMBOLIC_NAME), (Version)bundleInfo.get(Resource.VERSION));
			if (bundle == null) {
				uninstalledResources.add(resource);
			}
		}
		
	}

	private static final String BUNDLE_COLUMN = BUNDLE_SYMBOLICNAME;
	private static final String VERSION_COLUMN = BUNDLE_VERSION;
	private static final String DESCRIPTION_COLUMN = BUNDLE_DESCRIPTION;
	private static final String STATUS_COLUMN = BUNDLE.getString("pluginsStatusColumn");
	private static final String INSTALL_COLUMN = BUNDLE.getString("pluginsInstallColumn");
	private static final String ACTIVATE_COLUMN = BUNDLE.getString("pluginsActiveColumn");
	private static final String[] COLUMN_NAMES = {BUNDLE_COLUMN, VERSION_COLUMN, DESCRIPTION_COLUMN, STATUS_COLUMN, INSTALL_COLUMN, ACTIVATE_COLUMN};
	private static final Class<?>[] COLUMN_CLASSES = {JLabel.class, JLabel.class, JLabel.class, JLabel.class, JCheckBox.class, JCheckBox.class};
	private static final HashMap<Integer, String> BUNDLE_STATES = new HashMap<Integer, String>();
	static {
		BUNDLE_STATES.put(Bundle.UNINSTALLED, "UNINSTALLED");
		BUNDLE_STATES.put(Bundle.INSTALLED, "INSTALLED");
		BUNDLE_STATES.put(Bundle.RESOLVED, "RESOLVED");
		BUNDLE_STATES.put(Bundle.STARTING, "STARTING");
		BUNDLE_STATES.put(Bundle.STOPPING, "STOPPING");
		BUNDLE_STATES.put(Bundle.ACTIVE, "ACTIVE");
	}

	private void reloadBundleTable() {
		bundleTable.invalidate();
		bundleTable.repaint();
	}

	private Bundle getBundleLoaded(String symbolicName, Version version) {
		for (Bundle bundle : bundleContext.getBundles()) {
			if (bundle.getSymbolicName().equals(symbolicName) &&
				bundle.getVersion().equals(version)
				) {
				return bundle;
			}
		}
		return null;
	}

	private boolean isRequiredBundle(Bundle bundle) {
		return (bundle instanceof Framework) ||
			requiredBundles.contains(bundle.getLocation().substring(bundle.getLocation().lastIndexOf('/') + 1));
	}

	@SuppressWarnings("serial")
	private class BundleTableModel extends DefaultTableModel {

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return COLUMN_CLASSES[columnIndex];
		}

		@Override
		public int getColumnCount() {
			return COLUMN_NAMES.length - (isShowUninstalledBundles ? 1 : 0);
		}

		@Override
		public String getColumnName(int columnIndex) {
			return COLUMN_NAMES[columnIndex];
		}

		@Override
		public int getRowCount() {
			int rowCount = 0;
			if (bundleContext != null && isShowInstalledBundles) {
				rowCount += bundleContext.getBundles().length;
			}
			if (uninstalledResources != null && isShowUninstalledBundles) {
				rowCount += uninstalledResources.size();
			}
			return rowCount;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (isShowInstalledBundles && rowIndex < bundleContext.getBundles().length) {
				Bundle bundle = bundleContext.getBundles()[rowIndex];
				switch (columnIndex) {
				case 0:
					return bundle.getSymbolicName();
				case 1:
					return bundle.getVersion().toString();
				case 2:
					return bundle.getHeaders().get(BUNDLE_DESCRIPTION);
				case 3:
					return BUNDLE_STATES.get(bundle.getState());
				case 4:
					return true;
				case 5:
					return bundle.getState() == Bundle.ACTIVE;
				default:
					return null;
				}
			}
			else {
				int useRowIndex = rowIndex - (isShowInstalledBundles ? bundleContext.getBundles().length : 0);
				@SuppressWarnings("unchecked")
				Map<String, Object> bundleInfo = uninstalledResources.get(useRowIndex).getProperties();
				Bundle bundle = getBundleLoaded((String)bundleInfo.get(Resource.SYMBOLIC_NAME), (Version)bundleInfo.get(Resource.VERSION));
				switch (columnIndex) {
				case 0:
					return bundleInfo.get(Resource.SYMBOLIC_NAME);
				case 1:
					return bundleInfo.get(Resource.VERSION);
				case 2:
					return bundleInfo.get(Resource.DESCRIPTION);
				case 3:
					return bundle == null ? "UNINSTALLED" : BUNDLE_STATES.get(bundle.getState());
				case 4:
					return bundle != null;
				default:
					return null;
				}
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (((columnIndex == ACTIVATE_COLUMN_INDEX) && !isShowUninstalledBundles) || 
			(columnIndex == INSTALL_COLUMN_INDEX)) &&
			!(isShowInstalledBundles && rowIndex < bundleContext.getBundles().length && isRequiredBundle(bundleContext.getBundles()[rowIndex]));
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == INSTALL_COLUMN_INDEX) { // install / uninstall bundle
				if (isShowInstalledBundles && rowIndex < bundleContext.getBundles().length) {
					Bundle bundle = bundleContext.getBundles()[rowIndex];
					try {
						bundle.uninstall();
					}
					catch (BundleException bex) {
						displayError(bex.getMessage());
					}
				}
				else {
					int useRowIndex = rowIndex - (isShowInstalledBundles ? bundleContext.getBundles().length : 0);
					Resource resource = uninstalledResources.get(useRowIndex);
					Bundle bundle = getBundleLoaded((String)resource.getProperties().get(Resource.SYMBOLIC_NAME), (Version)resource.getProperties().get(Resource.VERSION));
					if (bundle == null) {
						Resolver resolver = repoAdmin.resolver();
						resolver.add(resource);
						if (resolver.resolve())
						{
						    resolver.deploy(true);
						}
						else
						{
							StringBuffer sb = new StringBuffer(BUNDLE.getString("pluginsBundleLoadError"));
						    sb.append(" -> ");
							boolean started = false;
							for (Requirement req : resolver.getUnsatisfiedRequirements()) {
								if (started) {
								    sb.append(", ");
								}
								started = true;
							    sb.append(req.getComment());
							}
							displayError(sb.toString());
						}
					}
					else {
						try {
							bundle.uninstall();
						}
						catch (BundleException bex) {
							displayError(bex.getMessage());
						}
					}
				}
				filterUninstalledResources();
			}
			else if (columnIndex == ACTIVATE_COLUMN_INDEX) { // start / stop bundle
				Bundle bundle = bundleContext.getBundles()[rowIndex];
				if (bundle.getState() == Bundle.ACTIVE) {
					try {
						clearError();
						bundle.stop();
					}
					catch (BundleException bex) {
						displayError(bex.getMessage());
					}
				}
				else if (bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED)  {
					try {
						clearError();
						bundle.start();
					}
					catch (BundleException bex) {
						displayError(bex.getMessage());
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
		}
	}
}

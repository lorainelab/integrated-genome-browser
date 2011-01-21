package com.affymetrix.igb.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

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
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.RepositoryChangeListener;
import com.affymetrix.igb.plugins.BundleTableModel.NameInfoPanel;

public class PluginsView extends JPanel implements IPluginsHandler, RepositoryChangeListener, Constants {
	private static final long serialVersionUID = 1L;
	private static final String IGB_TIER_HEADER = "IGB-Tier";
	private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	private final Cursor defaultCursor = null;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
	private static final BundleFilter BOTH_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return true;
		}
	};
	private static final BundleFilter INSTALLED_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return isInstalled(bundle);
		}
	};
	private static final BundleFilter UNINSTALLED_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return !isInstalled(bundle);
		}
	};
	private static final BundleFilter NEITHER_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return false;
		}
	};
	private final BundleFilter SYSTEM_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return getTier(bundle) > 1;
		}
	};
	private BundleContext bundleContext;
	private JScrollPane jScrollPane;
	private final BundleTableModel bundleTableModel;
	private final JTable bundleTable;
	private JCheckBox installedBundlesCheckbox;
	private JCheckBox uninstalledBundlesCheckbox;
	private JButton updateAllBundlesButton;
	private JButton updateSelectedBundlesButton;
	private JButton repositoryPrefsButton;
	private JLabel errors;
	private boolean isShowInstalledBundles = true;
	private boolean isShowUninstalledBundles = false;
	private RepositoryAdmin repoAdmin;
	private BundleListener bundleListener;
	private final IGBService igbService;
	private List<Bundle> installedBundles;
	private List<Bundle> repositoryBundles;
	private List<Bundle> unfilteredBundles;
	private List<Bundle> filteredBundles;
	private HashMap<String, Bundle> latest;
	private BundleFilter bundleFilter;

	public PluginsView(IGBService _igbService) {
		super();
		igbService = _igbService;
		latest = new HashMap<String, Bundle>();

		igbService.addRepositoryChangeListener(this);
		setLayout(new BorderLayout());
		BundleTableModel.setPluginsHandler(this); // is there a better way ?
		bundleTableModel = new BundleTableModel();
		bundleTable = new JTable(bundleTableModel) {
			private static final long serialVersionUID = 1L;
			public void valueChanged(ListSelectionEvent e) {
				super.valueChanged(e);
				updateSelectedBundlesButton.setEnabled(isUpdateSelectedBundlesExist());
			}
		};
		bundleTable.setRowSorter(
			new TableRowSorter<TableModel>(bundleTableModel) {
				public boolean isSortable(int column) {
					return column == bundleTableModel.getColumnIndex(BUNDLE_SYMBOLICNAME);
				}
			}
		);
		bundleTable.addMouseListener(
			new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
		            Bundle bundle = getNameInfoBundle(e.getPoint());
		            if (bundle != null) {
			            String bundleDocURL = (String)bundle.getHeaders().get(Constants.BUNDLE_DOCURL);
			            if (bundleDocURL != null) {
				            GeneralUtils.browse(bundleDocURL);
			            }
		            }
				}
				@Override
				public void mouseEntered(MouseEvent e) {
		            Bundle bundle = getNameInfoBundle(e.getPoint());
		            if (bundle == null) {
						setCursor(defaultCursor);
		            }
		            else {
						setCursor(handCursor);
		            }
				}
				@Override
				public void mouseExited(MouseEvent e) {
					setCursor(defaultCursor);
				}
			}
		);

		bundleTable.addMouseMotionListener(
			new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
		            Bundle bundle = getNameInfoBundle(e.getPoint());
		            if (bundle == null) {
						setCursor(defaultCursor);
		            }
		            else {
						setCursor(handCursor);
		            }
				}
			}
		);

		bundleTableModel.setJTable(bundleTable);

		jScrollPane = new JScrollPane(bundleTable);
		add("Center", jScrollPane);
		add("South", getButtonPanel());
		setBundleFilter(getBundleFilter());

		bundleListener = 
	    	new BundleListener() {
				public void bundleChanged(BundleEvent arg0) {
					if (bundleContext != null) {
						setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
						reloadBundleTable();
					}
				}
			};
	}

	private Bundle getNameInfoBundle(Point p) {
        int row = bundleTable.rowAtPoint(p);
        int column = bundleTable.columnAtPoint(p);
		if (column == bundleTableModel.getColumnIndex(BUNDLE_SYMBOLICNAME)) {
            Bundle bundle = getBundleAtRow(row);
            Rectangle r = bundleTable.getCellRect(row, column, false);
            NameInfoPanel nameInfoPanel = NameInfoPanel.getPanel(bundle); // kludge
            if (nameInfoPanel.isOnInfoIcon(p.x - r.x, p.y - r.y)) {
            	return bundle;
            }
		}
		return null;
	}

	private JPanel getButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		installedBundlesCheckbox = new JCheckBox(BUNDLE.getString("installedBundles"));
		installedBundlesCheckbox.setToolTipText(BUNDLE.getString("installedBundlesTooltip"));
		installedBundlesCheckbox.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					isShowInstalledBundles = !isShowInstalledBundles;
					setBundleFilter(getBundleFilter());
					reloadBundleTable();
					clearError();
				}
			}
		);
		installedBundlesCheckbox.setSelected(true);
		buttonPanel.add(installedBundlesCheckbox);

		uninstalledBundlesCheckbox = new JCheckBox(BUNDLE.getString("uninstalledBundles"));
		uninstalledBundlesCheckbox.setToolTipText(BUNDLE.getString("uninstalledBundlesTooltip"));
		uninstalledBundlesCheckbox.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					isShowUninstalledBundles = !isShowUninstalledBundles;
					setBundleFilter(getBundleFilter());
					reloadBundleTable();
					clearError();
				}
			}
		);
		buttonPanel.add(uninstalledBundlesCheckbox);

		updateAllBundlesButton = new JButton(BUNDLE.getString("updateAllBundles"));
		updateAllBundlesButton.setToolTipText(BUNDLE.getString("updateAllBundlesTooltip"));
		updateAllBundlesButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateAllBundles();
				}
			}
		);
		updateAllBundlesButton.setEnabled(false);
		buttonPanel.add(updateAllBundlesButton);

		updateSelectedBundlesButton = new JButton(BUNDLE.getString("updateSelectedBundles"));
		updateSelectedBundlesButton.setToolTipText(BUNDLE.getString("updateSelectedBundlesTooltip"));
		updateSelectedBundlesButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateSelectedBundles();
				}
			}
		);
		updateSelectedBundlesButton.setEnabled(false);
		buttonPanel.add(updateSelectedBundlesButton);

		buttonPanel.add(new JLabel("      "));
		errors = new JLabel(BUNDLE.getString("OSGiNotLoaded"));
		errors.setForeground(Color.RED);
		buttonPanel.add(errors);

		buttonPanel.add(Box.createHorizontalGlue());

		repositoryPrefsButton = new JButton(BUNDLE.getString("repositoryButton"));
		repositoryPrefsButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					final Object src = evt.getSource();

					if (src == PluginsView.this.repositoryPrefsButton) {
						// Go to repository prefs tab.
						igbService.displayRepositoryPreferences();
					}
				}
			}
		);
		repositoryPrefsButton.setToolTipText(BUNDLE.getString("repositoryTooltip"));
		buttonPanel.add(repositoryPrefsButton);

		return buttonPanel;
	}

	public ImageIcon getIcon(String name) {
		return igbService.getIcon(name);
	}

	public Bundle getBundleAtRow(int row) {
		int modelRow = bundleTable.convertRowIndexToModel(row);
		return getFilteredBundle(modelRow);
	}

	public static boolean isInstalled(Bundle bundle) {
		return bundle.getState() != Bundle.UNINSTALLED;
	}

	private void installBundleIfNecessary(Bundle bundle) {
		if (isInstalled(bundle)) {
			Bundle latestBundle = latest.get(bundle.getSymbolicName());
			if (!bundle.equals(latestBundle) && !isInstalled(latestBundle)) {
				try {
					bundle.uninstall();
				}
				catch (BundleException x) {
					x.printStackTrace(System.err);
				}
				installBundle(latestBundle);
			}
		}
	}

	private void updateAllBundles() {
		if (filteredBundles != null) {
			for (Bundle bundle : filteredBundles) {
				installBundleIfNecessary(bundle);
			}
		}
	}

	private void updateSelectedBundles() {
		int[] rowIndices = bundleTable.getSelectedRows();
		for (int i = 0; i < rowIndices.length; i++) {
			Bundle bundle = getBundleAtRow(rowIndices[i]);
			installBundleIfNecessary(bundle);
		}
	}

	private boolean isUpdateBundlesExist() {
		boolean updateBundlesExist = false;
		if (filteredBundles != null) {
			for (Bundle bundle : filteredBundles) {
				if (isInstalled(bundle) && !bundle.equals(latest.get(bundle.getSymbolicName()))) {
					updateBundlesExist = true;
				}
			}
		}
		return updateBundlesExist;
	}

	private boolean isUpdateSelectedBundlesExist() {
		boolean updateSelectedBundlesExist = false;
		int[] rowIndices = bundleTable.getSelectedRows();
		for (int i = 0; i < rowIndices.length; i++) {
			Bundle bundle = getBundleAtRow(rowIndices[i]);
			if (latest.get(bundle.getSymbolicName()) == null) {
				System.out.println("isUpdateSelectedBundlesExist - no latest for " + bundle.getSymbolicName());
			}
			if (isInstalled(bundle) && !bundle.equals(latest.get(bundle.getSymbolicName()))) {
				updateSelectedBundlesExist = true;
			}
		}
		return updateSelectedBundlesExist;
	}

	public Version getLatestVersion(Bundle bundle) {
		return latest.get(bundle.getSymbolicName()).getVersion();
	}

	private boolean isLatest(Bundle bundle) {
		return bundle.getVersion().equals(getLatestVersion(bundle));
	}

	public boolean isUpdatable(Bundle bundle) {
		return bundle != null && isInstalled(bundle) && !isLatest(bundle);
	}

	public Bundle getFilteredBundle(int index) {
		if (index < 0 || index >= filteredBundles.size()) {
			return null;
		}
		return filteredBundles.get(index);
	}

	public int getFilteredBundleCount() {
		if (filteredBundles == null) {
			return 0;
		}
		return filteredBundles.size();
	}

	public int getTier(Bundle bundle) {
		if (bundle.getBundleId() == 0) { // system bundle
			return 0;
		}
		int tier = 3;
		String tierString = ((String)bundle.getHeaders().get(IGB_TIER_HEADER));
		if (tierString != null) {
			try {
				tier = Integer.parseInt(tierString.trim());
			}
			catch (Exception x) {}
		}
		return tier;
	}

	public boolean isTier2Bundle(Bundle bundle) {
		return getTier(bundle) == 2;
	}

	private BundleFilter getBundleFilter() {
		BundleFilter bundleFilter = null;
		if (isShowInstalledBundles && isShowUninstalledBundles) {
			bundleFilter = BOTH_BUNDLE_FILTER;
		}
		else if (isShowInstalledBundles && !isShowUninstalledBundles) {
			bundleFilter = INSTALLED_BUNDLE_FILTER;
		}
		else if (!isShowInstalledBundles && isShowUninstalledBundles) {
			bundleFilter = UNINSTALLED_BUNDLE_FILTER;
		}
		else if (!isShowInstalledBundles && !isShowUninstalledBundles) {
			bundleFilter = NEITHER_BUNDLE_FILTER;
		}
		return bundleFilter;
	}

	public void setRepositoryBundles() {
		Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
		List<Bundle> repositoryBundles = new ArrayList<Bundle>();
		for (Resource resource : allResourceArray) {
			repositoryBundles.add(new ResourceWrapper(resource));
		}
		setRepositoryBundles(repositoryBundles);
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
//		ServiceReference sr = bundleContext.getServiceReference(org.osgi.service.obr.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
//		ServiceReference sr = bundleContext.getServiceReference(org.apache.felix.bundlerepository.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
		repoAdmin = new RepositoryAdminWrapper((org.apache.felix.bundlerepository.RepositoryAdmin) new RepositoryAdminImpl(bundleContext, new Logger(bundleContext)));
		for (String url : igbService.getRepositories()) {
			repositoryAdded(url);
		}
		setRepositoryBundles();
		bundleContext.addBundleListener(bundleListener);
		reloadBundleTable();
		clearError();
	}

	private void reloadBundleTable() {
		filterBundles();
		bundleTableModel.fireTableDataChanged();
		bundleTable.invalidate();
		bundleTable.repaint();
	}

	public void deactivate() {
		isShowInstalledBundles = false;
		isShowUninstalledBundles = false;
		bundleContext.removeBundleListener(bundleListener);
		bundleContext = null;
	}

	public void clearError() {
		errors.setText("");
	}

	public void displayError(String errorText) {
		errors.setText(errorText);
	}

	public void installBundle(Bundle bundle) {
		Resource resource = ((ResourceWrapper)bundle).getResource();
		Resolver resolver = repoAdmin.resolver();
		resolver.add(resource);
		if (resolver.resolve())
		{
		    resolver.deploy(true);
		}
		else
		{
			StringBuffer sb = new StringBuffer(PluginsView.BUNDLE.getString("bundleLoadError"));
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

	public void setInstalledBundles(List<Bundle> installedBundles) {
		this.installedBundles = installedBundles;
		setUnfilteredBundles();
	}

	public void setRepositoryBundles(List<Bundle> repositoryBundles) {
		this.repositoryBundles = repositoryBundles;
		setUnfilteredBundles();
	}

	private void addBundle(Bundle bundle) {
		String symbolicName = bundle.getSymbolicName();
		Version version = bundle.getVersion();
		for (Bundle unfilteredBundle : unfilteredBundles) {
			if (symbolicName.equals(unfilteredBundle.getSymbolicName()) &&
					version.equals(unfilteredBundle.getVersion())
				) {
				return;
			}
		}
		unfilteredBundles.add(bundle);
		if (latest.get(symbolicName) == null || version.compareTo(latest.get(symbolicName).getVersion()) > 0) {
			latest.put(symbolicName, bundle);
		}
	}

	private void setUnfilteredBundles() {
		unfilteredBundles = new ArrayList<Bundle>();
		latest.clear();
		if (installedBundles != null) {
			for (Bundle bundle : installedBundles) {
				addBundle(bundle);
			}
		}
		if (repositoryBundles != null) {
			for (Bundle bundle : repositoryBundles) {
				addBundle(bundle);
			}
		}
	}

	private void filterBundles() {
		filteredBundles = new ArrayList<Bundle>();
		for (Bundle bundle : unfilteredBundles) {
			if (SYSTEM_BUNDLE_FILTER.filterBundle(bundle) && bundleFilter.filterBundle(bundle)) {
				filteredBundles.add(bundle);
			}
		}
		updateAllBundlesButton.setEnabled(isUpdateBundlesExist());
	}

	public void setBundleFilter(BundleFilter bundleFilter) {
		this.bundleFilter = bundleFilter;
	}

	@Override
	public boolean repositoryAdded(String url) {
		boolean addedOK = false;
		try {
			repoAdmin.addRepository(new URL(url + "/repository.xml"));
			addedOK = true;
		}
		catch (ConnectException x) {
			displayError("some plugin repositories have failed");
		}
		catch (MalformedURLException x) {
			igbService.displayError("Invalid plugin repository URL: " + url);
			x.printStackTrace();
		}
		catch (Exception x) {
			igbService.failRepository(url);
			displayError("error loading repositories");
			x.printStackTrace();
		}
		setRepositoryBundles();
		return addedOK;
	}

	@Override
	public void repositoryRemoved(String url) {
		try {
			repoAdmin.removeRepository(new URL(url + "/repository.xml"));
		}
		catch (MalformedURLException x) {}
		setRepositoryBundles();
	}
}

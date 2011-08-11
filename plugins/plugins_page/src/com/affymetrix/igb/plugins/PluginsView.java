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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;

import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.bundlerepository.impl.wrapper.RepositoryAdminWrapper;
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

import com.affymetrix.genometryImpl.event.RepositoryChangeListener;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBox;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.plugins.BundleTableModel;
import com.affymetrix.igb.plugins.BundleTableModel.NameInfoPanel;

/**
 * Tab Panel for managing plugins / bundles.
 */
public class PluginsView extends IGBTabPanel implements IPluginsHandler, RepositoryChangeListener, Constants {
	private static final long serialVersionUID = 1L;
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("plugins");
	private static final ResourceBundle BUNDLE_PROPERTIES = ResourceBundle.getBundle("bundles");
	private static final int TAB_POSITION = 6;

	private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	private final Cursor defaultCursor = null;
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
			if (bundle.getBundleId() == 0) { // system bundle
				return false;
			}
			try {
				BUNDLE_PROPERTIES.getString(bundle.getSymbolicName() + ";" + bundle.getVersion());
			}
			catch (MissingResourceException x) {
				return true;
			}
			return false;
		}
	};
	private BundleContext bundleContext;
	private JScrollPane jScrollPane;
	private final BundleTableModel bundleTableModel;
	private final JTable bundleTable;
	private JRPCheckBox installedBundlesCheckbox;
	private JRPCheckBox uninstalledBundlesCheckbox;
	private JRPButton updateAllBundlesButton;
	private JRPButton updateSelectedBundlesButton;
	private JRPButton repositoryPrefsButton;
	private JLabel errors;
	private boolean isShowInstalledBundles = true;
	private boolean isShowUninstalledBundles = true;
	private RepositoryAdmin repoAdmin;
	private BundleListener bundleListener;
	private List<Bundle> installedBundles;
	private List<Bundle> repositoryBundles;
	private List<Bundle> unfilteredBundles; // all methods that access filteredBundles should be synchronized
	private List<Bundle> filteredBundles; // all methods that access filteredBundles should be synchronized
	private HashMap<String, Bundle> latest;
	private BundleFilter bundleFilter;

	public PluginsView(IGBService igbService) {
		super(igbService, BUNDLE.getString("viewTab"), BUNDLE.getString("viewTab"), false, TAB_POSITION);
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
		bundleTable.setAutoCreateRowSorter(true);
		bundleTable.getRowSorter().setSortKeys(BundleTableModel.SORT_KEYS);
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

	/**
	 * get the Bundle on the line where the cursor is
	 * @param p the point where the cursor is
	 * @return the bundle on the line where the cursor is
	 */
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

	/**
	 * Create the button panel for the Plugin View
	 * @return the JPanel with all the buttons
	 */
	private JPanel getButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		installedBundlesCheckbox = new JRPCheckBox("PluginsView_installedBundlesCheckbox", BUNDLE.getString("installedBundles"));
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

		uninstalledBundlesCheckbox = new JRPCheckBox("PluginsView_uninstalledBundlesCheckbox", BUNDLE.getString("uninstalledBundles"));
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
		uninstalledBundlesCheckbox.setSelected(true);
		buttonPanel.add(uninstalledBundlesCheckbox);

		updateAllBundlesButton = new JRPButton("PluginsView_updateAllBundlesButton", BUNDLE.getString("updateAllBundles"));
		updateAllBundlesButton.setToolTipText(BUNDLE.getString("updateAllBundlesTooltip"));
		updateAllBundlesButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateAllBundles();
					clearError();
				}
			}
		);
		updateAllBundlesButton.setEnabled(false);
		buttonPanel.add(updateAllBundlesButton);

		updateSelectedBundlesButton = new JRPButton("PluginsView_updateSelectedBundlesButton", BUNDLE.getString("updateSelectedBundles"));
		updateSelectedBundlesButton.setToolTipText(BUNDLE.getString("updateSelectedBundlesTooltip"));
		updateSelectedBundlesButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateSelectedBundles();
					clearError();
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

		repositoryPrefsButton = new JRPButton("PluginsView_repositoryPrefsButton", BUNDLE.getString("repositoryButton"));
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

	@Override
	public ImageIcon getIcon(String name) {
		return igbService.getIcon(name);
	}

	@Override
	public Bundle getBundleAtRow(int row) {
		int modelRow = bundleTable.convertRowIndexToModel(row);
		return getFilteredBundle(modelRow);
	}

	/**
	 * determins if a given bundle is installed
	 * @param bundle the bundle to check
	 * @return true if the bundle is installed, false otherwise
	 */
	private static boolean isInstalled(Bundle bundle) {
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

	private synchronized void updateAllBundles() {
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

	private synchronized boolean isUpdateBundlesExist() {
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
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "isUpdateSelectedBundlesExist - no latest for " + bundle.getSymbolicName());
			}
			if (isInstalled(bundle) && !bundle.equals(latest.get(bundle.getSymbolicName()))) {
				updateSelectedBundlesExist = true;
			}
		}
		return updateSelectedBundlesExist;
	}

	@Override
	public Version getLatestVersion(Bundle bundle) {
		return latest.get(bundle.getSymbolicName()).getVersion();
	}

	/**
	 * determines whether the bundle is the latest version
	 * checks all the bundles with the same Symbolic name for
	 * the highest version number
	 * @param bundle the bundle to check
	 * @return true if the bundle is the latest version, false otherwise
	 */
	private boolean isLatest(Bundle bundle) {
		return bundle.getVersion().equals(getLatestVersion(bundle));
	}

	@Override
	public boolean isUpdatable(Bundle bundle) {
		return bundle != null && isInstalled(bundle) && !isLatest(bundle);
	}

	@Override
	public synchronized Bundle getFilteredBundle(int index) {
		if (index < 0 || index >= filteredBundles.size()) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "getFilteredBundle() error at index=" + index + ", filteredBundles.size()=" + filteredBundles.size());
			return null;
		}
		return filteredBundles.get(index);
	}

	@Override
	public synchronized int getFilteredBundleCount() {
		if (filteredBundles == null) {
			return 0;
		}
		return filteredBundles.size();
	}

	/**
	 * get the bundle filter for the given user settings
	 * @return the bundle filter
	 */
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

	/**
	 * gets the full set of all bundles in all the bundle repositories
	 * in the Preferences tab
	 */
	private void setRepositoryBundles() {
		Resource[] allResourceArray = repoAdmin.discoverResources("(symbolicname=*)");
		List<Bundle> repositoryBundles = new ArrayList<Bundle>();
		for (Resource resource : allResourceArray) {
			repositoryBundles.add(new ResourceWrapper(resource));
		}
		setRepositoryBundles(repositoryBundles);
	}

	/**
	 * save the OSGi BundleContext
	 * @param bundleContext the bundle context from the OSGi implementation
	 */
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		setInstalledBundles(Arrays.asList(bundleContext.getBundles()));
//		ServiceReference sr = bundleContext.getServiceReference(org.osgi.service.obr.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
//		ServiceReference sr = bundleContext.getServiceReference(org.apache.felix.bundlerepository.RepositoryAdmin.class.getName());
//		repoAdmin = (RepositoryAdmin)bundleContext.getService(sr);
		repoAdmin = new RepositoryAdminWrapper((org.apache.felix.bundlerepository.RepositoryAdmin) new RepositoryAdminImpl(bundleContext, new org.apache.felix.utils.log.Logger(bundleContext)));
		for (String url : igbService.getRepositories()) {
			repositoryAdded(url);
		}
		setRepositoryBundles();
		bundleContext.addBundleListener(bundleListener);
		reloadBundleTable();
		clearError();
	}

	/**
	 * reload the table data due to any changes
	 */
	private void reloadBundleTable() {
		filterBundles();
		bundleTableModel.fireTableDataChanged();
		bundleTable.invalidate();
		bundleTable.repaint();
	}

	/**
	 * called before the page is closed
	 */
	public void deactivate() {
		isShowInstalledBundles = false;
		isShowUninstalledBundles = false;
		bundleContext.removeBundleListener(bundleListener);
		bundleContext = null;
	}

	private void clearError() {
		errors.setText("");
	}

	private void displayError(String errorText) {
		errors.setText(errorText);
		Logger.getLogger(PluginsView.class.getName()).log(Level.SEVERE, errorText);
	}

	@Override
	public void installBundle(Bundle bundle) {
		Resource resource = ((ResourceWrapper)bundle).getResource();
		Resolver resolver = repoAdmin.resolver();
		resolver.add(resource);
		if (resolver.resolve())
		{
		    resolver.deploy(true);
		    clearError();
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

	@Override
	public void uninstallBundle(Bundle bundle) {
		try {
			bundle.uninstall();
		    clearError();
		}
		catch (BundleException bex) {
			displayError(bex.getMessage());
		}
	}

	/**
	 * saves the currently installed bundles
	 * @param installedBundles the currently installed bundles
	 */
	private void setInstalledBundles(List<Bundle> installedBundles) {
		this.installedBundles = installedBundles;
		setUnfilteredBundles();
	}

	/**
	 * saves the current set of bundles in all repositories
	 * @param repositoryBundles the bundles in all repositories
	 */
	private void setRepositoryBundles(List<Bundle> repositoryBundles) {
		this.repositoryBundles = repositoryBundles;
		setUnfilteredBundles();
	}

	/**
	 * add a new bundle (installed or repository)
	 * @param bundle the bundle to add
	 */
	private synchronized void addBundle(Bundle bundle) {
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

	/**
	 * update the set of all bundles (unfiltered) due to
	 * a change
	 */
	private synchronized void setUnfilteredBundles() {
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

	/**
	 * filter the unfiltered bundles using the current
	 * bundle filter
	 */
	private synchronized void filterBundles() {
		filteredBundles = new ArrayList<Bundle>();
		for (Bundle bundle : unfilteredBundles) {
			if (SYSTEM_BUNDLE_FILTER.filterBundle(bundle) && bundleFilter.filterBundle(bundle)) {
				filteredBundles.add(bundle);
			}
		}
		updateAllBundlesButton.setEnabled(isUpdateBundlesExist());
	}

	/**
	 * set the bundle filter to use
	 * @param bundleFilter the bundle filter
	 */
	private void setBundleFilter(BundleFilter bundleFilter) {
		this.bundleFilter = bundleFilter;
	}

	@Override
	public boolean repositoryAdded(final String url) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				try {
					repoAdmin.addRepository(new URL(url + "/repository.xml"));
					setRepositoryBundles();
					reloadBundleTable();
				}
				catch (ConnectException x) {
					displayError("plugin repository failed: " + url);
				}
				catch (MalformedURLException x) {
					ErrorHandler.errorPanel("Invalid plugin repository URL: " + url);
					x.printStackTrace();
				}
				catch (Exception x) {
					igbService.failRepository(url);
					displayError("error loading repositories");
					x.printStackTrace();
				}
				return null;
			}
		};
		igbService.getPrimaryExecutor(this).execute(worker);
		return true;
	}

	@Override
	public void repositoryRemoved(String url) {
		try {
			repoAdmin.removeRepository(new URL(url + "/repository.xml"));
		}
		catch (MalformedURLException x) {}
		setRepositoryBundles();
		reloadBundleTable();
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
}

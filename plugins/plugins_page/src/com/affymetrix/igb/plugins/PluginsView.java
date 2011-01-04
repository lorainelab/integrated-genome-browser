package com.affymetrix.igb.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
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
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.bundlerepository.impl.wrapper.RepositoryAdminWrapper;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.RepositoryChangeListener;

public class PluginsView extends JPanel implements IPluginsHandler, RepositoryChangeListener, Constants {
	private static final long serialVersionUID = 1L;
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
			return bundle.getState() != Bundle.UNINSTALLED;
		}
	};
	private static final BundleFilter UNINSTALLED_BUNDLE_FILTER = new BundleFilter() {
		@Override
		public boolean filterBundle(Bundle bundle) {
			return bundle.getState() == Bundle.UNINSTALLED;
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
			return !tier1Bundles.contains(bundle.getSymbolicName());
		}
	};
	private final Set<String> tier1Bundles;
	private final Set<String> tier2Bundles;
	private BundleContext bundleContext;
	private JScrollPane jScrollPane;
	private final BundleTableModel bundleTableModel;
	private final JTable bundleTable;
	private final JCheckBox installedBundlesCheckbox;
	private final JCheckBox uninstalledBundlesCheckbox;
	private final JButton updateAllBundlesButton;
	private final JButton repositoryPrefsButton;
	private final JLabel errors;
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
	private Cursor defaultCursor;

	public PluginsView(IGBService _igbService) {
		super();
		this.igbService = _igbService;
		latest = new HashMap<String, Bundle>();
		this.tier1Bundles = igbService.getTier1Bundles();
		this.tier2Bundles = igbService.getTier2Bundles();

		igbService.addRepositoryChangeListener(this);
		this.setLayout(new BorderLayout());
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

		buttonPanel.add(new JLabel("      "));
		errors = new JLabel(BUNDLE.getString("OSGiNotLoaded"));
		errors.setForeground(Color.RED);
		buttonPanel.add(errors);
		BundleTableModel.setPluginsHandler(this); // is there a better way ?
		bundleTableModel = new BundleTableModel();
		bundleTable = new JTable(bundleTableModel);
/*
		bundleTable.setRowSorter(
			new TableRowSorter<TableModel>(bundleTableModel) {
				public boolean isSortable(int column) {
					return column == bundleTableModel.getColumnIndex(BUNDLE_SYMBOLICNAME);
				}
			}
		);
*/
		defaultCursor = getCursor();

		MouseAdapter mouseAdapter = new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
	    		Bundle bundle = getBundle(e);
		    	if (isVersionColumn(e) && isUpdatable(bundle)) {
	    			installBundle(latest.get(bundle.getSymbolicName()));
		    	}
		    }
		    public void mouseEntered(MouseEvent e) {
		    	any(e);
		    }
		    public void mouseDragged(MouseEvent e) {
		    	any(e);
		    }
		    public void mouseMoved(MouseEvent e) {
		    	any(e);
		    }
		    private void any(MouseEvent e) {
	    		Bundle bundle = getBundle(e);
		    	if (isVersionColumn(e) && isUpdatable(bundle)) {
		    		setCursor(new Cursor(Cursor.HAND_CURSOR));
		    	}
		    	else {
			    	setCursor(defaultCursor);
		    	}
		    }
		    public void mouseExited(MouseEvent e) {
		    	setCursor(defaultCursor);
		    }
		};
		bundleTable.addMouseListener(mouseAdapter);
		bundleTable.addMouseMotionListener(mouseAdapter);

		bundleTableModel.setJTable(bundleTable);

		jScrollPane = new JScrollPane(bundleTable);
		this.add("Center", jScrollPane);
		this.add("South", buttonPanel);
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

	private Bundle getBundle(MouseEvent e) {
        Bundle bundle = null;
        if (e.getComponent().isEnabled())
        {
            Point p = e.getPoint();
            int row = bundleTable.rowAtPoint(p);
            if (row >= 0 && row < filteredBundles.size()) {
            	bundle = filteredBundles.get(row);
            }
        }
        return bundle;
	}

	private boolean isVersionColumn(MouseEvent e) {
        boolean latestColumn = false;
        if (e.getComponent().isEnabled())
        {
            Point p = e.getPoint();
            int column = bundleTable.columnAtPoint(p);
            if (column == bundleTableModel.getColumnIndex(BUNDLE_VERSION)) {
            	latestColumn = true;
            }
        }
        return latestColumn;
	}

	private void updateAllBundles() {
		if (installedBundles != null) {
			for (Bundle bundle : installedBundles) {
				if (!latest.get(bundle.getSymbolicName()).equals(bundle)) {
	    			installBundle(latest.get(bundle.getSymbolicName()));
				}
			}
		}
	}

	private boolean isUpdateBundlesExist() {
		boolean updateBundlesExist = false;
		if (installedBundles != null) {
			for (Bundle bundle : installedBundles) {
				if (!latest.get(bundle.getSymbolicName()).equals(bundle)) {
					updateBundlesExist = true;;
				}
			}
		}
		return updateBundlesExist;
	}

	public Version getLatestVersion(Bundle bundle) {
		return latest.get(bundle.getSymbolicName()).getVersion();
	}

	private boolean isLatest(Bundle bundle) {
		return bundle.getVersion().equals(getLatestVersion(bundle));
	}

	public boolean isUpdatable(Bundle bundle) {
		return bundle != null && bundle.getState() != Bundle.UNINSTALLED && !isLatest(bundle);
	}

	public List<Bundle> getFilteredBundles() {
		return filteredBundles;
	}

	public boolean isTier2Bundle(Bundle bundle) {
		return tier2Bundles.contains(bundle.getSymbolicName());
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
		bundleTableModel.setRowCount(0);
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
		updateAllBundlesButton.setEnabled(isUpdateBundlesExist());
	}

	private void filterBundles() {
		filteredBundles = new ArrayList<Bundle>();
		for (Bundle bundle : unfilteredBundles) {
			if (SYSTEM_BUNDLE_FILTER.filterBundle(bundle) && bundleFilter.filterBundle(bundle)) {
				filteredBundles.add(bundle);
			}
		}
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

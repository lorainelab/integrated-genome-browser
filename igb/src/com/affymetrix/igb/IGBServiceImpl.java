package com.affymetrix.igb;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.action.UCSCViewAction;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.view.PluginInfo;
import com.affymetrix.igb.view.SearchView;

public class IGBServiceImpl implements IGBService, BundleActivator {

	private static Felix felix;
	private static IGBService service;
	private IGBServiceImpl() {}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final void loadFelix() {
		Map configMap = new StringMap(false);
		for (String key : BUNDLE.getString("pluginsConfigList").split(",")) {
			configMap.put(key, BUNDLE.getString(key));
		}
        List list = new ArrayList();
        service = new IGBServiceImpl();
        list.add(service);
        configMap.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        felix = new Felix(configMap);
	}

	public static void startOSGi() {
		loadFelix();

        try
        {
            felix.start();
            BundleContext bundleContext = felix.getBundleContext();
			bundleContext.registerService(IGBService.class.getName(), service, new Properties());
			String pluginsIGBServerURL = BUNDLE.getString("pluginsIGBServerURL");
			for (String pluginName : service.getRequiredBundles()) {
				Bundle bundle = bundleContext.installBundle(pluginsIGBServerURL + "/required/" + pluginName);
				bundle.start();
			}
        }
        catch (Exception ex)
        {
			Logger.getLogger(service.getClass().getName()).log(
					Level.WARNING, "Could not create framework, plugins disabled: {0}", ex.getMessage());
        }
    }

	public static void stopOSGi() {
        try
        {
        	felix.stop();
        	felix = null;
        }
        catch (Exception ex)
        {
			Logger.getLogger(service.getClass().getName()).log(
					Level.WARNING, "Could not stop framework, plugins disabled: {0}", ex.getMessage());
        }
	}

	public String[] getRequiredBundles() {
		return BUNDLE.getString("pluginsRequiredList").split(",");
	}

	public boolean addMenu(JMenu new_menu) {
		String menuName = new_menu.getName();
		JMenuBar main_menu_bar = MenuUtil.getMainMenuBar();
		int num_menus = main_menu_bar.getMenuCount();
	    for (int i=0; i<num_menus; i++) {
	      JMenu menu_i = main_menu_bar.getMenu(i);
	      if (menuName.equals(menu_i.getName())) {
	        menu_i.getName();
	        return false; // already a menu with this name
	      }
	    }

	    // Add the new menu, but keep the "Help" menu in last place
	    if (num_menus > 0 && "Help".equals(main_menu_bar.getMenu(num_menus-1).getName())) {
	    	main_menu_bar.add(new_menu, num_menus-1);
	    } else {
	    	main_menu_bar.add(new_menu);
	    }
	    main_menu_bar.validate();
	    return true;
	}

	public boolean removeMenu(String menuName) {
		JMenuBar main_menu_bar = MenuUtil.getMainMenuBar();
		int num_menus = main_menu_bar.getMenuCount();
	    for (int i=0; i<num_menus; i++) {
	      JMenu menu_i = main_menu_bar.getMenu(i);
	      if (menuName.equals(menu_i.getName())) {
	    	main_menu_bar.remove(i);
	        return true;
	      }
	    }
	    return false; // not found
	}

	private HashMap<String, JComponent> addedPlugins = new HashMap<String, JComponent>();
	public void addPlugIn(JComponent plugIn, String tabName) {
		IGB.singleton_igb.loadPlugIn(new PluginInfo(plugIn.getClass().getName(), tabName, true), plugIn);
		addedPlugins.put(tabName, plugIn);
	}

	public boolean removePlugIn(String name) {
		if (name == null) {
			return false;
		}
		JTabbedPane tab_pane = IGB.singleton_igb.getTabPane() ;
		Map<Component, Frame> comp2window = IGB.singleton_igb.getComp2Window();
		JComponent plugIn = addedPlugins.get(name);
		Frame frame = comp2window.get(plugIn);
		if (frame == null) {
			for (int i = 0; i < tab_pane.getTabCount(); i++) {
				if (name.equals(tab_pane.getTitleAt(i))) {
					tab_pane.remove(i);
					return true;
				}
			}
		}
		else {
			frame.dispose();
			comp2window.remove(plugIn);
		}
		PreferenceUtils.saveComponentState(name, PreferenceUtils.COMPONENT_STATE_TAB); // default - can't delete state
		return false;
	}
/*
	private JComponent getView(String viewName) {
		Class<?> viewClass;
		try {
			viewClass = Class.forName(viewName);
		}
		catch (ClassNotFoundException x) {
			System.out.println("IGBServiceImpl.getView() failed for " + viewName);
			return null;
		}
		for (Object plugin : IGB.singleton_igb.getPlugins()) {
			if (viewClass.isAssignableFrom(plugin.getClass())) {
				return (JComponent)plugin;
			}
		}
		return null;
	}
*/
	public void displayError(String title, String errorText) {
		ErrorHandler.errorPanel(title, errorText);
	}

	public void displayError(String errorText) {
		ErrorHandler.errorPanel(errorText);
	}

	public void addNotLockedUpMsg(String message) {
		Application.getSingleton().addNotLockedUpMsg(message);
	}

	public void removeNotLockedUpMsg(String message) {
		Application.getSingleton().removeNotLockedUpMsg(message);
	}

	public int searchForRegexInResidues(
			boolean forward, Pattern regex, String residues, int residue_offset, List<GlyphI> glyphs, Color hitColor) {
		return SearchView.searchForRegexInResidues(
				forward, regex, residues, residue_offset, Application.getSingleton().getMapView().getAxisTier(), glyphs, hitColor);
	}

	private BioSeq getViewSeq() {
		return Application.getSingleton().getMapView().getViewSeq();
	}

	public boolean isSeqResiduesAvailable() {
		BioSeq vseq = getViewSeq();
		return vseq != null && vseq.isComplete();
	}

	public int getSeqResiduesMin() {
		return getViewSeq().getMin();
	}

	public int getSeqResiduesMax() {
		return getViewSeq().getMax();
	}

	public String getSeqResidues() {
		return getViewSeq().getResidues();
	}

	public void updateMap() {
		Application.getSingleton().getMapView().getSeqMap().updateWidget();
	}

	public void removeGlyphs(List<GlyphI> glyphs) {
		Application.getSingleton().getMapView().getSeqMap().removeItem(glyphs);
	}

	public String getUCSCQuery() {
		return UCSCViewAction.getUCSCQuery();
	}
}

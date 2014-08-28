package com.affymetrix.igb.view;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.weblink.WebLink;
import com.affymetrix.igb.prefs.WebLinkUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

final class LinkControl implements ContextualPopupListener {

        @Override
	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_syms, SeqSymmetry primary_sym) {
		if (primary_sym == null || selected_syms.size() != 1) {
			return;
		}
		if(primary_sym instanceof CdsSeqSymmetry) {
			primary_sym = ((CdsSeqSymmetry) primary_sym).getPropertySymmetry();
		}
		
		Map<String, String> menu_items = new LinkedHashMap<String, String>(); // map of menu url->name, or url -> url if there is no name

		// DAS files can contain links for each individual feature.
		// These are stored in the "link" property
		Object links = null;
		if (primary_sym instanceof SymWithProps) {
			links = ((SymWithProps) primary_sym).getProperty("link");
			if (links != null) {
				generateMenuItemsFromLinks(links, primary_sym, menu_items);
			}
		}

//		generateMenuItemsFromWebLinks(primary_sym, menu_items);

		makeMenuItemsFromMap(primary_sym, popup);

	}

	@SuppressWarnings("unchecked")
	private void generateMenuItemsFromLinks(Object links, SeqSymmetry primary_sym, Map<String, String> menu_items) {
		if (links instanceof String) {
			Object link_names = null;
			if (primary_sym instanceof SymWithProps) {
				link_names = ((SymWithProps) primary_sym).getProperty("link_name");
			}
			String url = (String) links;
			url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
			if (link_names instanceof String) {
				menu_items.put(url, (String) link_names);
			} else {
				menu_items.put(url, url);
			}
		} else if (links instanceof List) {
			List<String> urls = (List<String>) links;
			for (String url : urls) {
				url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
				menu_items.put(url, url);
			}
		} else if (links instanceof Map) {
			Map<String, String> name2url = (Map<String, String>) links;
			for (Map.Entry<String, String> entry : name2url.entrySet()) {
				String name = entry.getKey();
				String url = entry.getValue();
				url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
				menu_items.put(url, name);
			}
		}
	}

//	private void generateMenuItemsFromWebLinks(SeqSymmetry primary_sym, Map<String, String> menu_items) {
//		// by using a Map to hold the urls, any duplicated urls will be filtered-out.
//		for (WebLink webLink : WebLink.getWebLinks(primary_sym)) {
//			// Generally, just let any link replace an existing link that has the same URL.
//			// But, if the new one has no name, and the old one does, then keep the old one.
//			String new_name = webLink.getName();
//			String url = webLink.getURLForSym(primary_sym);
//			String old_name = menu_items.get(url);
//			if (old_name == null || "".equals(old_name)) {
//				menu_items.put(url, new_name);
//			}
//		}
//	}

	private static void makeMenuItemsFromMap(SeqSymmetry primary_sym, JPopupMenu popup) {
		List<WebLink> results = new ArrayList<WebLink>();
		results.addAll(WebLinkUtils.getServerList().getWebLinks(primary_sym));
		results.addAll(WebLinkUtils.getLocalList().getWebLinks(primary_sym));
		if (results.isEmpty()) {
			return;
		}

		String name, url;
		JMenuItem mi;
		if (results.size() == 1) {
			for (WebLink webLink : results) {
				url = webLink.getURLForSym(primary_sym);
				name = webLink.getName();
				if (name == null || name.equals(url)) {
					name = "Search Web";
				}

				mi = makeMenuItem(name, url);
                                mi.setIcon(CommonUtils.getInstance().getIcon(webLink.getImageIconPath()));
//				if (!isInternetReachable()) {
//					name += " (No Internet)";
//					mi = makeMenuItem(name, url);
//					mi.setEnabled(false);
//				}

				//mi.setIcon(MenuUtil.getIcon("16x16/actions/search.png"));
				popup.add(mi,2);

			}
		} else {
			name = "Search Web";
			JMenu linkMenu = new JMenu(name);
                        linkMenu.setIcon(CommonUtils.getInstance().getIcon("16x16/actions/searchweb.png"));
//			linkMenu.getPopupMenu().setBorder(popup.getBorder());
//			//linkMenu.setIcon(MenuUtil.getIcon("16x16/actions/search.png"));
//			if (!isInternetReachable()) {
//				name += " (No Internet)";
//				linkMenu = new JMenu(name);
//				linkMenu.setEnabled(false);
//			}
			popup.add(linkMenu,2);

			for (WebLink webLink : results) {
				url = webLink.getURLForSym(primary_sym);
				name = webLink.getName();
				if (name == null || name.equals(url)) {
					name = "Unnamed link to web";
				}
				mi = makeMenuItem(name, url);
                                mi.setIcon(CommonUtils.getInstance().getIcon(webLink.getImageIconPath()));
				linkMenu.add(mi);
			}
		}
	}

	private static JMenuItem makeMenuItem(String name, final String url) {
		JMenuItem linkMI = new JMenuItem(name);
		if (url != null) {
			linkMI.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					GeneralUtils.browse(url);
				}
			});
		}
		return linkMI;
	}

//	private static boolean isInternetReachable() {
//		try {
//			//make a URL to a known source
//			URL url = new URL("http://bioviz.org/igb/");
//
//			//open a connection to that source
//			HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
//
//			//trying to retrieve data from the source. If there
//			//is no connection, this line will fail
//			/*Object objData = */ urlConnect.getContent();
//
//		} catch (UnknownHostException e) {
//			return false;
//		} catch (IOException e) {
//			return false;
//		}
//		return true;
//	}
}

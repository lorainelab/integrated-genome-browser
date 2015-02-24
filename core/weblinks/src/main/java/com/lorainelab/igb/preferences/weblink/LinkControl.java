package com.lorainelab.igb.preferences.weblink;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.google.common.base.Strings;
import com.lorainelab.igb.preferences.weblink.model.WebLink;
import com.lorainelab.igb.services.IgbService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

@Component(name = LinkControl.COMPONENT_NAME, immediate = true)
public class LinkControl implements ContextualPopupListener {

    public static final String COMPONENT_NAME = "LinkControl";
    private static final String searchWebIconPath = "16x16/actions/searchweb.png";
    private IgbService igbService;

    public LinkControl() {
    }

    @Activate
    public void activate() {
        igbService.getSeqMapView().addPopupListener(this);
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Override
    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_syms, SeqSymmetry primary_sym) {
        if (primary_sym == null || selected_syms.size() != 1) {
            return;
        }
        if (primary_sym instanceof CdsSeqSymmetry) {
            primary_sym = ((CdsSeqSymmetry) primary_sym).getPropertySymmetry();
        }

        Map<String, String> menu_items = new LinkedHashMap<>(); // map of menu url->name, or url -> url if there is no name

        // DAS files can contain links for each individual feature.
        // These are stored in the "link" property
        Object links = null;
        if (primary_sym instanceof SymWithProps) {
            links = ((SymWithProps) primary_sym).getProperty("link");
            if (links != null) {
                generateMenuItemsFromLinks(links, primary_sym, menu_items);
            }
        }

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

    private static void makeMenuItemsFromMap(SeqSymmetry primary_sym, JPopupMenu popup) {
        List<WebLink> results = new ArrayList<>();
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
                popup.add(mi, 2);

            }
        } else {
            name = "Search Web";
            JMenu linkMenu = new JMenu(name);
            linkMenu.setIcon(CommonUtils.getInstance().getIcon(searchWebIconPath));
            popup.add(linkMenu, 2);

            for (WebLink webLink : results) {
                url = webLink.getURLForSym(primary_sym);
                name = webLink.getName();
                if (name == null || name.equals(url)) {
                    name = "Unnamed link to web";
                }
                mi = makeMenuItem(name, url);
                if (!Strings.isNullOrEmpty(webLink.getImageIconPath())) {
                    mi.setIcon(CommonUtils.getInstance().getIcon(webLink.getImageIconPath()));
                }
                linkMenu.add(mi);
            }
        }
    }

    private static JMenuItem makeMenuItem(String name, final String url) {
        JMenuItem linkMI = new JMenuItem(name);
        if (url != null) {
            linkMI.addActionListener(evt -> GeneralUtils.browse(url));
        }
        return linkMI;
    }

}

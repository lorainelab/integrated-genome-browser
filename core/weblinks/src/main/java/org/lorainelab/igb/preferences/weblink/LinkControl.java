package org.lorainelab.igb.preferences.weblink;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.symmetry.impl.CdsSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JPopupMenu;
import org.lorainelab.igb.menu.api.AnnotationContextMenuProvider;
import org.lorainelab.igb.menu.api.model.AnnotationContextEvent;
import org.lorainelab.igb.menu.api.model.ContextMenuItem;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.menu.api.model.MenuSection;
import org.lorainelab.igb.preferences.weblink.model.WebLink;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class LinkControl implements AnnotationContextMenuProvider {

    private static final String SEARCH_WEB_ICONPATH = "searchweb.png";
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MenuIcon.class);

    public LinkControl() {
    }

    @Activate
    private void activate() {
    }

    @Override
    public Optional<List<MenuItem>> buildMenuItem(AnnotationContextEvent event) {
        if (event.getSelectedItems().isEmpty()) {
            return Optional.empty();
        }
        SeqSymmetry primarySym = event.getSelectedItems().get(0);
        if (primarySym instanceof CdsSeqSymmetry) {
            primarySym = ((CdsSeqSymmetry) primarySym).getPropertySymmetry();
        }
        return buildContextMenuItem(primarySym);
    }

    public void popupNotify(JPopupMenu popup, SeqSymmetry primarySym) {
        if (primarySym == null) {
            return;
        }

    }

    private Optional<List<MenuItem>> buildContextMenuItem(SeqSymmetry primarySym) {
        List<WebLink> results = new ArrayList<>();
        results.addAll(WebLinkUtils.getServerList().getWebLinks(primarySym));
        results.addAll(WebLinkUtils.getLocalList().getWebLinks(primarySym));
        if (results.isEmpty()) {
            return Optional.empty();
        }

        String name;
        ContextMenuItem contextMenuItem;
        if (results.size() == 1) {
            for (WebLink webLink : results) {
                name = webLink.getName();
                final String url = webLink.getURLForSym(primarySym);
                if (name == null || name.equals(url)) {
                    name = "Search Web";
                }
                contextMenuItem = new ContextMenuItem(name, (Void t) -> {
                    GeneralUtils.browse(url);
                    return t;
                });
                contextMenuItem.setMenuSection(MenuSection.INFORMATION);
                try (InputStream resourceAsStream = LinkControl.class.getClassLoader().getResourceAsStream(SEARCH_WEB_ICONPATH)) {
                    contextMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                return Optional.of(Arrays.asList(contextMenuItem));

            }
        } else {
            Set<MenuItem> childMenuItems = Sets.newHashSet();

            for (WebLink webLink : results) {
                name = webLink.getName();
                String url = webLink.getURLForSym(primarySym);
                if (name == null || name.equals(url)) {
                    name = "Unnamed link to web";
                }

                ContextMenuItem childContextMenu = new ContextMenuItem(name, (Void t) -> {
                    GeneralUtils.browse(url);
                    return t;
                });
                childContextMenu.setMenuSection(MenuSection.INFORMATION);
                if (!Strings.isNullOrEmpty(webLink.getImageIconPath())) {
                    try (InputStream resourceAsStream = LinkControl.class.getClassLoader().getResourceAsStream(webLink.getImageIconPath())) {
                        childContextMenu.setMenuIcon(new MenuIcon(resourceAsStream));
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                childMenuItems.add(childContextMenu);
            }
            name = "Search Web";
            ContextMenuItem parentContextMenu = new ContextMenuItem(name, childMenuItems);
            parentContextMenu.setMenuSection(MenuSection.INFORMATION);
            try (InputStream resourceAsStream = LinkControl.class.getClassLoader().getResourceAsStream(SEARCH_WEB_ICONPATH)) {
                parentContextMenu.setMenuIcon(new MenuIcon(resourceAsStream));
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            return Optional.ofNullable(Arrays.asList(parentContextMenu));
        }
        return Optional.empty();
    }

}

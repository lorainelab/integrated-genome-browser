package com.affymetrix.igb.window.service.def;

import com.affymetrix.common.PreferenceUtils;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel.TabState;
import org.lorainelab.igb.services.window.tabs.TabHolder;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * TabHolder implementation for all tabs that are in separate popup windows
 */
public class WindowTabs implements TabHolder {

    private Set<IgbTabPanel> addedPlugins;
    private final TabStateHandler tabStateHandler;

    public WindowTabs(TabStateHandler _tabStateHandler) {
        super();
        tabStateHandler = _tabStateHandler;
        addedPlugins = new HashSet<>();
    }

    /**
     * open a tab panel in a new popup window, and set its close operation to
     * put it into the default state
     *
     * @param comp the tab panel
     */
    private void openCompInWindow(final IgbTabPanel comp) {
        final String name = comp.getName();
        final String display_name = comp.getDisplayName();

//		Icon temp_icon = null;
//		if (temp_icon == null) {
//			temp_icon = comp.getIcon();
//		}
        final JFrame frame = new JFrame(display_name);
        comp.setFrame(frame);

//			final Image icon = temp_icon;
//			if (icon != null) {
//				frame.setIconImage(icon);
//			}
        final Container cont = frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        cont.add(comp);
        comp.setVisible(true);
        frame.pack(); // pack() to set frame to its preferred size

        Rectangle defRect = (comp.isCheckMinimumWindowSize() && comp.getTrayRectangle() != null) ? comp.getTrayRectangle() : frame.getBounds();
        Rectangle pos = PreferenceUtils.retrieveWindowLocation(name, defRect);
        if (pos != null) {
            PreferenceUtils.setWindowSize(frame, pos);
        }
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                tabStateHandler.setDefaultState(comp);
            }
        });
        PreferenceUtils.saveComponentState(name, TabState.COMPONENT_STATE_WINDOW.name());
    }

    public void restoreWindowPosition(IgbTabPanel tabPanel) {
        Rectangle pos = PreferenceUtils.retrieveWindowLocation(tabPanel.getName(), tabPanel.getFrame().getBounds());
        if (pos != null) {
            PreferenceUtils.setWindowSize(tabPanel.getFrame(), pos);
        }
    }

    @Override
    public void addTab(final IgbTabPanel plugin) {
        addedPlugins.add(plugin);
        Runnable r = () -> openCompInWindow(plugin);
        SwingUtilities.invokeLater(r);
    }

    @Override
    public void removeTab(final IgbTabPanel plugin) {
        // save the current size into the preferences, so the window
        // will re-open with this size next time
        addedPlugins.remove(plugin);
        JFrame frame = plugin.getFrame();
        if (frame != null) {
            final Container cont = frame.getContentPane();
            PreferenceUtils.saveWindowLocation(frame, plugin.getName());
            cont.remove(plugin);
            cont.validate();
            frame.dispose();
            plugin.setFrame(null);
            PreferenceUtils.saveComponentState(plugin.getName(), TabState.COMPONENT_STATE_WINDOW.name());
        }
    }

    @Override
    public Set<IgbTabPanel> getIGBTabPanels() {
        return addedPlugins;
    }

    @Override
    public void restoreState() {
        for (final IgbTabPanel tabPanel : addedPlugins) {
            Runnable r = () -> restoreWindowPosition(tabPanel);
            SwingUtilities.invokeLater(r);
        }
    }

    @Override
    public void resize() {
    }

    @Override
    public void close() {
    }

    @Override
    public void selectTab(IgbTabPanel panel) {
    }

    @Override
    public String getName() {
        return "Window";
    }
}

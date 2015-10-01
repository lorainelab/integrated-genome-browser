package com.lorainelab.igb.plugins;

import javax.swing.ImageIcon;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * interface to give the TableModel access to the data in the plugins view
 */
public interface IPluginsHandler {

    /**
     * install a new bundle / plugin
     *
     * @param bundle the bundle to install
     */
    public void installBundle(Bundle bundle);

    /**
     * uninstall an installed bundle / plugin
     *
     * @param bundle the bundle to uninstall
     */
    public void uninstallBundle(Bundle bundle);

    /**
     * return a bundle in the filtered bundles given the index
     *
     * @param index the index into the filtered bundles
     * @return the bundle at the index
     */
    public Bundle getFilteredBundle(int index);

    /**
     * get the number of filtered bundles, these are the bundles shown in the
     * table
     *
     * @return the count of filtered bundles
     */
    public int getFilteredBundleCount();

    /**
     * get the icon with the given name
     *
     * @param name the name of the icon
     * @return the icon
     */
    public ImageIcon getIcon(String name);

    /**
     * get the bundle at the specified row in the table
     *
     * @param row the row in the table
     * @return the bundle at that row
     */
    public Bundle getBundleAtRow(int row);

    /**
     * determines if the specified bundle can be updated to a newer version
     *
     * @param bundle the bundle to check
     * @return if there is a newer version of the bundle that can be installed
     */
    public boolean isUpdatable(Bundle bundle);

    /**
     * get the latest version of the specified bundle
     *
     * @param bundle the bundle to check
     * @return the latest version of the bundle in all the repositories
     */
    public Version getLatestVersion(Bundle bundle);

    /**
     * get the repository of the specified bundle
     *
     * @param bundle the bundle to check
     * @return the repository
     */
    public String getRepository(Bundle bundle);
}

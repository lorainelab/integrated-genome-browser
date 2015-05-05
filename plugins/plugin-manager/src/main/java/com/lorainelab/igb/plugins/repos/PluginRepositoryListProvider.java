package com.lorainelab.igb.plugins.repos;

import com.lorainelab.igb.plugins.repos.view.BundleRepositoryTableModel;
import com.lorainelab.igb.preferences.model.PluginRepository;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface PluginRepositoryListProvider {

    public Set<PluginRepository> getPluginRepositories();

    public void addPluginRepository(PluginRepository pluginRepository);

    public void removePluginRepository(PluginRepository pluginRepository);

    public void updatePluginRepoPrefs(PluginRepository pluginRepository);

    public void pluginRepositoryRefreshed(PluginRepository pluginRepository);

    public void pluginRepoAvailabilityChanged(PluginRepository pluginRepository);

    public BundleRepositoryTableModel getBundleRepositoryTableModel();

}

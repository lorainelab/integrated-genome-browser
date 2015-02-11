package com.lorainelab.igb.plugins.repos;

import com.lorainelab.igb.preferences.model.PluginRepository;
import java.util.List;

/**
 *
 * @author dcnorris
 */
public interface PluginRepositoryListProvider {

    public List<PluginRepository> getPluginRepositories();

    public void addPluginRepository(PluginRepository pluginRepository);

    public void removePluginRepository(PluginRepository pluginRepository);

    public void updatePluginRepoPrefs(PluginRepository pluginRepository);

    public void pluginRepositoryRefreshed();

    public void pluginRepoAvailabilityChanged(PluginRepository pluginRepository);

}

package org.lorainelab.igb.plugin.manager.repos;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.util.GeneralUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.lorainelab.igb.plugin.manager.RepositoryInfoManager;
import org.lorainelab.igb.plugin.manager.repos.view.BundleRepositoryTableModel;
import org.lorainelab.igb.igb.preferences.IgbPreferencesService;
import org.lorainelab.igb.igb.preferences.model.IgbPreferences;
import org.lorainelab.igb.igb.preferences.model.PluginRepository;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = PluginRepositoryList.COMPONENT_NAME, immediate = true, provide = PluginRepositoryList.class)
public class PluginRepositoryList {

    public static final String COMPONENT_NAME = "PluginRepositoryList";
    private static final Logger logger = LoggerFactory.getLogger(PluginRepositoryList.class);
    private IgbPreferencesService igbPreferencesService;
    private final Set<PluginRepository> pluginRepositories;
    private RepositoryInfoManager repositoryInfoManager;
    private BundleRepositoryTableModel bundleRepositoryTableModel;

    public PluginRepositoryList() {
        pluginRepositories = Sets.newTreeSet((PluginRepository p1, PluginRepository p2) -> p1.getName().compareTo(p2.getName()));
    }

    @Activate
    public void activate() {
        Optional<IgbPreferences> prefs = igbPreferencesService.fromDefaultPreferences();
        if (prefs.isPresent()) {
            prefs.get().getRepository().stream().forEach(repo -> {
                pluginRepositories.add(repo);
                if (!preferenceNodeExist(repo)) {
                    addRepositoryToPrefs(repo);
                }
            });
        }
        //Load persisted servers from java persistence api
        loadPersistedRepos();
        pluginRepositories.stream().forEach(repo -> {
            if (repo.isEnabled()) {
                repositoryInfoManager.addPluginRepository(repo);
            }
        });
        bundleRepositoryTableModel = new BundleRepositoryTableModel(this);
    }

    private void loadPersistedRepos() {
        try {
            for (String url : PreferenceUtils.getRepositoriesNode().childrenNames()) {
                Preferences node = PreferenceUtils.getRepositoriesNode().node(url);
                PluginRepository PluginRepository = getPluginRepositoryFromPreferencesNode(node);
                removeDuplicatesFromDefaultRepositories(PluginRepository);
                pluginRepositories.add(PluginRepository);
            }
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void removeDuplicatesFromDefaultRepositories(PluginRepository pr) {
        Iterator<PluginRepository> i = pluginRepositories.iterator();
        while (i.hasNext()) {
            PluginRepository pluginRepo = i.next();
            if (pluginRepo.getUrl().equals(pr.getUrl())) {
                i.remove();
            }
        }
    }

    @Reference(optional = false)
    public void setIgbPreferencesService(IgbPreferencesService igbPreferencesService) {
        this.igbPreferencesService = igbPreferencesService;
    }

    @Reference(optional = false)
    public void setPluginsView(RepositoryInfoManager repositoryInfoManager) {
        this.repositoryInfoManager = repositoryInfoManager;
    }

    public Set<PluginRepository> getPluginRepositories() {
        return pluginRepositories;
    }

    public void addPluginRepository(PluginRepository pluginRepository) {
        pluginRepositories.add(pluginRepository);
        if (pluginRepository.isEnabled()) {
            if (repositoryInfoManager.addPluginRepository(pluginRepository)) {
                addRepositoryToPrefs(pluginRepository);
            } else {
                pluginRepositories.remove(pluginRepository);
            }
        }
        bundleRepositoryTableModel.updateRepositories(pluginRepositories);
    }

    public void removePluginRepository(PluginRepository pluginRepository) {
        pluginRepositories.remove(pluginRepository);
        removeRepositoryToPrefs(pluginRepository);
        repositoryInfoManager.removePluginRepository(pluginRepository);
        bundleRepositoryTableModel.updateRepositories(pluginRepositories);
    }

    private boolean preferenceNodeExist(PluginRepository pluginRepository) {
        try {
            String hashedUrlValue = getRepoUrlHash(pluginRepository);
            return PreferenceUtils.getRepositoriesNode().nodeExists(hashedUrlValue);
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

    private void addRepositoryToPrefs(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        HashFunction hf = Hashing.md5();
        String hashedUrlValue = hf.hashString(url, Charsets.UTF_8).toString();
        Preferences node = PreferenceUtils.getRepositoriesNode().node(hashedUrlValue);
        node.put("name", pluginRepository.getName());
        node.put("url", GeneralUtils.URLEncode(url));
        node.put("enabled", pluginRepository.getEnabled());
    }

    public void updatePluginRepoPrefs(PluginRepository pluginRepository) {
        addRepositoryToPrefs(pluginRepository);
    }

    private void removeRepositoryToPrefs(PluginRepository pluginRepository) {
        String hashedUrlValue = getRepoUrlHash(pluginRepository);
        try {
            PreferenceUtils.getRepositoriesNode().node(hashedUrlValue).removeNode();
        } catch (BackingStoreException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private String getRepoUrlHash(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        HashFunction hf = Hashing.md5();
        String hashedUrlValue = hf.hashString(url, Charsets.UTF_8).toString();
        return hashedUrlValue;
    }

    private PluginRepository getPluginRepositoryFromPreferencesNode(Preferences node) {
        String reporUrl = GeneralUtils.URLDecode(node.get("url", ""));
        String repoName = node.get("name", "");
        Boolean enabled = node.getBoolean("enabled", false);
        PluginRepository pluginRepository = new PluginRepository();
        pluginRepository.setName(repoName);
        pluginRepository.setEnabled(enabled);
        pluginRepository.setUrl(reporUrl);
        return pluginRepository;
    }

    public void pluginRepositoryRefreshed(PluginRepository pluginRepository) {
        removePluginRepository(pluginRepository);
        //allow time for any async operations from remove to complete
        Timer timer = new Timer(250, evt -> {
            addPluginRepository(pluginRepository);
            pluginRepository.setEnabled(true);
            pluginRepoAvailabilityChanged(pluginRepository);
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void pluginRepoAvailabilityChanged(PluginRepository pluginRepository) {
        if (pluginRepository.isEnabled()) {
            repositoryInfoManager.addPluginRepository(pluginRepository);
        } else {
            repositoryInfoManager.removePluginRepository(pluginRepository);
        }
    }

    public BundleRepositoryTableModel getBundleRepositoryTableModel() {
        return bundleRepositoryTableModel;
    }

}

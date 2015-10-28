/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.igb.plugins;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.thread.CThreadHolder;
import com.affymetrix.genometry.thread.CThreadWorker;
import com.google.common.collect.Lists;
import com.lorainelab.igb.preferences.model.PluginRepository;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = RepositoryInfoManager.class)
public class RepositoryInfoManager {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryInfoManager.class);
    private static final String REPOSITORY_XML_FILE_PATH = "/repository.xml";
    private static final String DEFAULT_REPO_INFO = "Local";

    private BundleInfoManager bundleInfoManager;
    private RepositoryAdmin repoAdmin;
    private final List<PluginRepository> externalRepositories;

    public RepositoryInfoManager() {
        externalRepositories = Lists.newArrayList();
    }

    @Reference
    public void setBundleInfoManager(BundleInfoManager bundleInfoManager) {
        this.bundleInfoManager = bundleInfoManager;
    }

    @Reference(optional = false)
    public void setRepositoryAdmin(RepositoryAdmin repositoryAdmin) {
        repoAdmin = repositoryAdmin;
    }

    public boolean addPluginRepository(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        CThreadWorker< Void, Void> worker = new CThreadWorker< Void, Void>("repositoryAdded") {
            @Override
            protected Void runInBackground() {
                try {
                    repoAdmin.addRepository(new URL(url + REPOSITORY_XML_FILE_PATH));
                    externalRepositories.add(pluginRepository);
                } catch (ConnectException ex) {
                    logger.error(ex.getMessage(), ex);
                } catch (MalformedURLException ex) {
                    logger.error(ex.getMessage(), ex);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                return null;
            }

            @Override
            protected void finished() {
                bundleInfoManager.reloadRepositoryBundles();
            }
        };
        CThreadHolder.getInstance().execute(this, worker);
        return true;
    }

    public void removePluginRepository(PluginRepository pluginRepository) {
        String url = pluginRepository.getUrl();
        try {
            repoAdmin.removeRepository(new URL(url + REPOSITORY_XML_FILE_PATH).toURI().toString());
            externalRepositories.remove(pluginRepository);
            bundleInfoManager.reloadRepositoryBundles();
        } catch (MalformedURLException | URISyntaxException ex) {
            logger.error("Error removing repository.", ex);
        }
    }

    public String getBundlesRepositoryName(Bundle bundle) {
        String repository = "";
        if (bundle != null) {
            String location = bundle.getLocation();
            if (location != null) {
                for (PluginRepository repo : externalRepositories) {
                    if (location.startsWith(repo.getUrl())) {
                        repository = repo.getName();
                    }
                }
                if (repository.isEmpty()) {
                    for (Bundle b : bundleInfoManager.getRepositoryManagedBundles()) {
                        if (b != null && b.getLocation() != null && bundle.getSymbolicName().equals(b.getSymbolicName()) && bundle.getVersion().equals(b.getVersion())) {
                            for (PluginRepository repo : externalRepositories) {
                                String repoUrl = repo.getUrl();
                                if (b.getLocation().startsWith(repoUrl)) {
                                    repository = repo.getName();
                                }
                            }
                        }
                    }
                }
                if (repository.isEmpty()) {
                    repository = DEFAULT_REPO_INFO;
                }
            }
        }
        return repository;
    }

}

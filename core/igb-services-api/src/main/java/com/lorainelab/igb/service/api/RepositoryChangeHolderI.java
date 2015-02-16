package com.lorainelab.igb.service.api;

import com.affymetrix.genometry.event.RepositoryChangeListener;
import java.util.Map;

public interface RepositoryChangeHolderI extends RepositoryChangeListener {

    public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);

    public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);

    public Map<String, String> getRepositories();

    public void failRepository(String url);
}

package com.affymetrix.igb.service.api;

import java.util.Map;

import com.affymetrix.genometryImpl.event.RepositoryChangeListener;

public interface RepositoryChangeHolderI extends RepositoryChangeListener {
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public Map<String, String> getRepositories();
	public void failRepository(String url);
	public void displayRepositoryPreferences();
}

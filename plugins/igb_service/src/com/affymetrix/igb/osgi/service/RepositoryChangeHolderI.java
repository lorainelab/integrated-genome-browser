package com.affymetrix.igb.osgi.service;

import java.util.List;

import com.affymetrix.genometryImpl.event.RepositoryChangeListener;

public interface RepositoryChangeHolderI extends RepositoryChangeListener {
	public void addRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public void removeRepositoryChangeListener(RepositoryChangeListener repositoryChangeListener);
	public List<String> getRepositories();
	public void failRepository(String url);
	public void displayRepositoryPreferences();
}

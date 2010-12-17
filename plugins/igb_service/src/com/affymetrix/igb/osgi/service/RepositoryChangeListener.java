package com.affymetrix.igb.osgi.service;

public interface RepositoryChangeListener {
	public boolean repositoryAdded(String url);
	public void repositoryRemoved(String url);
}

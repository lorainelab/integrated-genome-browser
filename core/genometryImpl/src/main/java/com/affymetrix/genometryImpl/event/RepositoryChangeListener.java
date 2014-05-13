package com.affymetrix.genometryImpl.event;

/**
 * Standard Listener interface for being notified when there is a
 * change - add / delete - of the Bundle Repositories referenced in the
 * Preferences tab.
 */
public interface RepositoryChangeListener {
	/**
	 * Called when the user adds a bundle repository on the
	 * Preferences tab
	 * @param url - the URL of the bundle repository
	 * @return true if the add was successful, false otherwise
	 */
	public boolean repositoryAdded(String url);
	/**
	 * Called when the user removes a bundle repository on the
	 * Preferences tab
	 * @param url - the URL of the bundle repository
	 */
	public void repositoryRemoved(String url);
}

package com.affymetrix.igb.osgi.service;

public interface ExtensionPointRegisterListener<T> {
	public void extensionPointAdded(ExtensionFactory<T> extensionFactory);
	public void extensionPointRemoved(ExtensionFactory<T> extensionFactory);
}

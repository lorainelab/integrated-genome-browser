package com.affymetrix.igb.osgi.service;

import java.util.HashSet;
import java.util.Set;

public class ExtensionPoint<T> {
	private final Set<ExtensionFactory<T>> extensionSet;
	private final Set<ExtensionPointRegisterListener<T>> extensionPointRegisterListeners;
	private Set<T> extensions;

	public ExtensionPoint() {
		super();
		extensionSet = new HashSet<ExtensionFactory<T>>();
		extensionPointRegisterListeners = new HashSet<ExtensionPointRegisterListener<T>>();
		extensions = new HashSet<T>();
	}

	private synchronized void reloadExtensions() {
		extensions = new HashSet<T>();
		for (ExtensionFactory<T> extensionFactory : extensionSet) {
			extensions.add(extensionFactory.createInstance());
		}
	}

	public synchronized void registerExtension(ExtensionFactory<T> extensionFactory) {
		extensionSet.add(extensionFactory);
		extensions.add(extensionFactory.createInstance());
		for (ExtensionPointRegisterListener<T> extensionPointRegisterListener : extensionPointRegisterListeners) {
			extensionPointRegisterListener.extensionPointAdded(extensionFactory);
		}
	}

	public synchronized void removeExtension(ExtensionFactory<T> extensionFactory) {
		extensionSet.remove(extensionFactory);
		reloadExtensions();
		for (ExtensionPointRegisterListener<T> extensionPointRegisterListener : extensionPointRegisterListeners) {
			extensionPointRegisterListener.extensionPointRemoved(extensionFactory);
		}
	}

	public Set<T> getExtensions() {
		return extensions;
	}

	public void addExtensionPointRegisterListener(ExtensionPointRegisterListener<T> extensionPointRegisterListener) {
		extensionPointRegisterListeners.add(extensionPointRegisterListener);
	}

	public void removeExtensionPointRegisterListener(ExtensionPointRegisterListener<T> extensionPointRegisterListener) {
		extensionPointRegisterListeners.remove(extensionPointRegisterListener);
	}
}

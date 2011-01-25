package com.affymetrix.igb.osgi.service;

import java.util.HashSet;
import java.util.Set;

public class ExtensionPoint<T> {
	private final Set<ExtensionFactory<T>> extensionSet;
	private final Set<ExtensionPointRegisterListener> extensionPointRegisterListeners;

	public ExtensionPoint() {
		super();
		extensionSet = new HashSet<ExtensionFactory<T>>();
		extensionPointRegisterListeners = new HashSet<ExtensionPointRegisterListener>();
	}

	public void registerExtension(ExtensionFactory<T> extensionFactory) {
		extensionSet.add(extensionFactory);
		for (ExtensionPointRegisterListener extensionPointRegisterListener : extensionPointRegisterListeners) {
			extensionPointRegisterListener.extensionPointAdded();
		}
	}

	public Set<T> getExtensions() {
		Set<T> extensions = new HashSet<T>();
		for (ExtensionFactory<T> extensionFactory : extensionSet) {
			extensions.add(extensionFactory.createInstance());
		}
		return extensions;
	}

	public void addExtensionPointRegisterListener(ExtensionPointRegisterListener extensionPointRegisterListener) {
		extensionPointRegisterListeners.add(extensionPointRegisterListener);
	}

	public void removeExtensionPointRegisterListener(ExtensionPointRegisterListener extensionPointRegisterListener) {
		extensionPointRegisterListeners.remove(extensionPointRegisterListener);
	}
}

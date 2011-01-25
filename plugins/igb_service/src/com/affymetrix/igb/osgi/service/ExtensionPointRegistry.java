package com.affymetrix.igb.osgi.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ExtensionPointRegistry {
	private static ExtensionPointRegistry instance = new ExtensionPointRegistry();
	private final HashMap<String, ExtensionPoint<?>> extensionPointMap;
	private final HashMap<String, Set<ExtensionPointRegisterListener>> extensionPointRegisterListeners;
	
	private ExtensionPointRegistry() {
		super();
		extensionPointMap = new HashMap<String, ExtensionPoint<?>>();
		extensionPointRegisterListeners = new HashMap<String, Set<ExtensionPointRegisterListener>>();
	}

	public static ExtensionPointRegistry getInstance() {
		return instance;
	}

	public ExtensionPoint<?> getExtensionPoint(String name) {
		return extensionPointMap.get(name);
	}

	public void registerExtensionPoint(String name, ExtensionPoint<?> extensionPoint) {
		extensionPointMap.put(name, extensionPoint);
		Set<ExtensionPointRegisterListener> extensionPointRegisterListenerSet = extensionPointRegisterListeners.get(name);
		if (extensionPointRegisterListenerSet != null) {
			for (ExtensionPointRegisterListener extensionPointRegisterListener : extensionPointRegisterListenerSet) {
				extensionPointRegisterListener.extensionPointAdded();
			}
		}
	}

	public void addExtensionPointRegisterListener(String extensionName, ExtensionPointRegisterListener extensionPointRegisterListener) {
		Set<ExtensionPointRegisterListener> extensionPointRegisterListenerSet = extensionPointRegisterListeners.get(extensionName);
		if (extensionPointRegisterListenerSet == null) {
			extensionPointRegisterListenerSet = new HashSet<ExtensionPointRegisterListener>();
			extensionPointRegisterListeners.put(extensionName, extensionPointRegisterListenerSet);
		}
		extensionPointRegisterListenerSet.add(extensionPointRegisterListener);
	}

	public void removeExtensionPointRegisterListener(String extensionName, ExtensionPointRegisterListener extensionPointRegisterListener) {
		Set<ExtensionPointRegisterListener> extensionPointRegisterListenerSet = extensionPointRegisterListeners.get(extensionName);
		if (extensionPointRegisterListenerSet != null) {
			extensionPointRegisterListenerSet.remove(extensionPointRegisterListener);
		}
	}
}

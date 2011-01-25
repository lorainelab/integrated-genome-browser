package com.affymetrix.igb.osgi.service;

import java.util.HashMap;

public class ExtensionPointRegistry {
	private static ExtensionPointRegistry instance = new ExtensionPointRegistry();
	private final HashMap<String, ExtensionPoint<?>> extensionPointMap;
	
	private ExtensionPointRegistry() {
		super();
		extensionPointMap = new HashMap<String, ExtensionPoint<?>>();
	}

	public static ExtensionPointRegistry getInstance() {
		return instance;
	}

	public ExtensionPoint<?> getExtensionPoint(String name) {
		return extensionPointMap.get(name);
	}

	public void registerExtensionPoint(String name, ExtensionPoint<?> extensionPoint) {
		extensionPointMap.put(name, extensionPoint);
	}
}

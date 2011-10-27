package com.affymetrix.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionPointImplHolder<T> {
	private static final Map<Class<?>, ExtensionPointImplHolder<?>> instances = new HashMap<Class<?>, ExtensionPointImplHolder<?>>();
	private List<T> extensionPointImpls = new ArrayList<T>();
	public static <Z> ExtensionPointImplHolder<Z> getInstance(Class<Z> clazz) {
		@SuppressWarnings("unchecked")
		ExtensionPointImplHolder<Z> instance = (ExtensionPointImplHolder<Z>)instances.get(clazz);
		if (instance == null) {
			instance = new ExtensionPointImplHolder<Z>();
			instances.put(clazz, instance);
		}
		return instance;
	}

	public List<T> getExtensionPointImpls() {
		return extensionPointImpls;
	}

	public void addExtensionPointImpl(T t) {
		extensionPointImpls.add(t);
	}

	public void removeExtensionPointImpl(T t) {
		extensionPointImpls.remove(t);
	}
}

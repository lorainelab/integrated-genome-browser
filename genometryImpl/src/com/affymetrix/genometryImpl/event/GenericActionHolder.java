package com.affymetrix.genometryImpl.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericActionHolder {
	private static GenericActionHolder instance = new GenericActionHolder();
	private final List<GenericActionListener> listeners = new ArrayList<GenericActionListener>();
	
	private GenericActionHolder() {
		super();
	}
	
	public static GenericActionHolder getInstance() {
		return instance;
	}

	private Map<String, GenericAction> genericActions = new HashMap<String, GenericAction>();

	public void addGenericAction(GenericAction genericAction) {
		genericActions.put(genericAction.getId(), genericAction);
//		if (genericAction.getText() != null) {
//			PreferenceUtils.getAccelerator(genericAction.getText());
//		}
		for (GenericActionListener listener : listeners) {
			listener.onCreateGenericAction(genericAction);
		}
	}

	public void removeGenericAction(GenericAction genericAction) {
		genericActions.remove(genericAction);
	}

	public GenericAction getGenericAction(String name) {
		return genericActions.get(name);
	}

	public Set<String> getGenericActionIds() {
		return genericActions.keySet();
	}

	public void addGenericActionListener(GenericActionListener listener) {
		listeners.add(listener);
		for (GenericAction genericAction : genericActions.values()) {
			listener.onCreateGenericAction(genericAction);
		}
	}

	public void removeGenericActionListener(GenericActionListener listener) {
		listeners.remove(listener);
	}

	public void notifyActionPerformed(GenericAction action) {
		for (GenericActionListener listener : listeners) {
			listener.notifyGenericAction(action);
		}
	}
}

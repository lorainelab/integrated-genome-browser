package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.Action;
import javax.swing.KeyStroke;

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

	/**
	 * Add one to the collection and let everyone know.
	 * All registered listeners will hear about this.
	 * BUG: Not checking that the action has a valid identifier
	 * which is used as a key to the collection.
	 * @param genericAction to add.
	 */
	public void addGenericAction(GenericAction genericAction) {
		genericActions.put(genericAction.getId(), genericAction);
		KeyStroke k = PreferenceUtils.getAccelerator(genericAction.getClass().getName());
		genericAction.putValue(Action.ACCELERATOR_KEY, k);
		for (GenericActionListener listener : listeners) {
			listener.onCreateGenericAction(genericAction);
		}
	}

	/**
	 * Add one to the collection and keep it a secret.
	 * Don't tell the listeners.
	 * This is experimental.
	 * We are trying to avoid some actions from showing up
	 * in the list of shortcuts in the preferences
	 * and (hence) in the tool bar.
	 * What about the tutorials?
	 * <p>Don't think it's working. - elb
	 * @param genericAction to add.
	 */
	public void addGenericActionSilently(GenericAction genericAction) {
		genericActions.put(genericAction.getId(), genericAction);
		KeyStroke k = PreferenceUtils.getAccelerator(genericAction.getClass().getName());
		genericAction.putValue(Action.ACCELERATOR_KEY, k);
	}

	public void removeGenericAction(GenericAction genericAction) {
		genericActions.remove(genericAction.getId());
	}

	public GenericAction getGenericAction(String name) {
		return genericActions.get(name);
	}

	public Set<String> getGenericActionIds() {
		return new CopyOnWriteArraySet<String>(genericActions.keySet());
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

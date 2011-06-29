package com.affymetrix.genometryImpl.event;

import java.util.ArrayList;
import java.util.List;

public class TierMaintenanceListenerHolder {
	private static TierMaintenanceListenerHolder instance = new TierMaintenanceListenerHolder();
	private TierMaintenanceListenerHolder() {
		super();
	}
	public static TierMaintenanceListenerHolder getInstance() {
		return instance;
	}
	private List<TierMaintenanceListener> tierMaintenanceListeners = new ArrayList<TierMaintenanceListener>();

	public void addTierMaintenanceListener(TierMaintenanceListener tierMaintenanceListener) {
		tierMaintenanceListeners.add(tierMaintenanceListener);
	}

	public void removeTierMaintenanceListener(TierMaintenanceListener tierMaintenanceListener) {
		tierMaintenanceListeners.remove(tierMaintenanceListener);
	}

	public List<TierMaintenanceListener> getTierMaintenanceListeners() {
		return tierMaintenanceListeners;
	}

	public void fireTierAdded() {
		for (TierMaintenanceListener tierMaintenanceListener : tierMaintenanceListeners) {
			tierMaintenanceListener.TierAdded();
		}
	}

	public void fireTierRemoved() {
		for (TierMaintenanceListener tierMaintenanceListener : tierMaintenanceListeners) {
			tierMaintenanceListener.TierRemoved();
		}
	}
}

package com.affymetrix.genometryImpl.event;

public interface TierMaintenanceListener {
	public void tierAdded();
	public void tierRemoved();
	public void dataRefreshed();
}

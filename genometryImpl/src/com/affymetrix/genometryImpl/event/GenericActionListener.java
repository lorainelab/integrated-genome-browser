package com.affymetrix.genometryImpl.event;

public interface GenericActionListener {
	public void onCreateGenericAction(GenericAction genericAction);
	public void notifyGenericAction(String actionClassName);
}

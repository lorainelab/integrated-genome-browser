package com.affymetrix.genometry.event;

public interface GenericActionListener {

    public void onCreateGenericAction(GenericAction genericAction);

    public void notifyGenericAction(GenericAction genericAction);
}

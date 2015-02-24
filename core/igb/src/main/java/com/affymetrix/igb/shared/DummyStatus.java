package com.affymetrix.igb.shared;

import com.lorainelab.igb.services.search.IStatus;

public class DummyStatus implements IStatus {

    private static DummyStatus instance = new DummyStatus();

    public static DummyStatus getInstance() {
        return instance;
    }

    private DummyStatus() {
        super();
    }

    @Override
    public void setStatus(String text) {
    }
}

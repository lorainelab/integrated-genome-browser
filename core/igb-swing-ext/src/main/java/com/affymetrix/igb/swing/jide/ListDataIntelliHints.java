package com.affymetrix.igb.swing.jide;

import com.affymetrix.igb.swing.JRPTextField;

public abstract class ListDataIntelliHints<T> {
    public ListDataIntelliHints(JRPTextField searchTF, String[] strings) {
    }

    protected void acceptHint(Object context) {
    }

    protected void setListData(Object[] toArray) {
    }

    public abstract boolean updateHints(Object context);
}

package com.affymetrix.genometry.event;

import java.util.EventObject;

@SuppressWarnings("serial")
public final class GenericServerInitEvent extends EventObject {

    public GenericServerInitEvent(Object src) {
        super(src);
    }
}

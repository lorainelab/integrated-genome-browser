package com.affymetrix.genometryImpl.event;

import java.util.EventObject;

public final class GenericServerInitEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	public GenericServerInitEvent(Object src) {
		super(src);
	}
}

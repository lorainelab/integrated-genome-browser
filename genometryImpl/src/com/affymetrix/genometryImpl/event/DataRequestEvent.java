/**
 *   Copyright (c) 2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.event;

import java.util.EventObject;
import com.affymetrix.genometry.SeqSpan;

/**
 *  Event representing a request for more data based on the current range along a sequence that is being viewed
 *
 *  Initially intended to be a user-initiated event based on current view in main IGB SeqMapView (SeqMapView is source)
 *  The idea is that components that support range-based partial loading of annotation tracks can listen
 *     for DataRequestEvents and load data based on the current view (and probably what has already been loaded)
 *  Initial listeners: Das2LoadView (DAS/2 annotations), TdbGraphLoadView (graph slices), LoadFileAction (graph slices)
 *     [note that all of these initial listeners end up feeding into calls to DAS/2 servers, to try and avoid
 *        reimplementing range-based data loading and optimizations]
 *
 */
public class DataRequestEvent extends EventObject {
	SeqSpan seq_span;
	static final long serialVersionUID = 1L;

	public DataRequestEvent(Object src, SeqSpan span) {
		super(src);
		seq_span = span;
	}

	public SeqSpan getRequestSpan() { return seq_span; }

}

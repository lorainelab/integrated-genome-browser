package com.affymetrix.igb.shared;

import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public interface SeqMapViewExtendedI extends SeqMapViewI {
	boolean autoChangeView();

	int getAverageSlots();
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.shared;

import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.view.load.VirtualFeature;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 *
 * @author lorainelab
 */
public class DataManagementTableInfo {

	public static List<String> getDataManagementTableTrackNames() {
		ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
		for (VirtualFeature vFeature : GeneralLoadView.getLoadView().getTableModel().virtualFeatures) {
			builder.add(vFeature.getFeature().featureName);
		}
		return builder.build();
	}
}

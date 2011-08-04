/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.RefreshStatus;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.util.List;

/**
 *
 * @author David
 */
public class VirtualFeature {

	private final GenericFeature gFeature;
	private ITrackStyle style;
	public boolean isPrimary;
	public boolean isParent;

	public VirtualFeature(GenericFeature feature) {
		this.gFeature = feature;
		this.isParent = false;
		if (gFeature.getMethods().size() > 0) {
			String uri = gFeature.getURI().toString();
			this.style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(uri);
		}
	}

	public VirtualFeature(GenericFeature feature, ITrackStyle tstyle) {
		this.gFeature = feature;
		this.style = tstyle;
		this.isParent = false;
	}

	public GenericFeature getFeature() {
		return gFeature;
	}

	public ServerType getServer() {
		return gFeature.gVersion.gServer.serverType;
	}

	public LoadStrategy getLoadStrategy() {
		return gFeature.getLoadStrategy();
	}

	public ITrackStyle getStyle() {
		return style;
	}

	public RefreshStatus getLastRefreshStatus() {
		return gFeature.getLastRefreshStatus();
	}

	public List<LoadStrategy> getLoadChoices() {
		return gFeature.getLoadChoices();
	}
}

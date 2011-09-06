/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.general.GenericFeature;
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
	private boolean isPrimary;

	public VirtualFeature(GenericFeature feature, ITrackStyle style) {
		this.gFeature = feature;
		this.style = style;
		this.isPrimary = true;
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

	public void setPrimary(boolean primary) {
		this.isPrimary = primary;
	}

	public boolean isPrimary() {
		return this.isPrimary;
	}

	public void setStyle(ITrackStyle style) {
		this.style = style;
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

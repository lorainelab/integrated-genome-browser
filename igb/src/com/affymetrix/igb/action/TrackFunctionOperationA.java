package com.affymetrix.igb.action;

import java.util.List;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.symloader.Delegate;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

public abstract class TrackFunctionOperationA extends GenericAction {
	private static final long serialVersionUID = 1L;
	protected final SeqMapViewI gviewer;
	private final Operator operator;

	public TrackFunctionOperationA(SeqMapViewI gviewer, Operator operator) {
		super();
		this.gviewer = gviewer;
		this.operator = operator;
	}

	protected void addTier(List<? extends GlyphI> tiers) {
		java.util.List<String> symsStr = new java.util.ArrayList<String>();
		java.util.List<GenericFeature> features = new java.util.ArrayList<GenericFeature>();
		StringBuilder meth = new StringBuilder();
		meth.append(operator.getName()).append(": ");


		for (GlyphI tier : gviewer.getSelectedTiers()) {
			symsStr.add(((TierGlyph)tier).getAnnotStyle().getMethodName());
			meth.append(((TierGlyph)tier).getAnnotStyle().getTrackName()).append(", ");
			features.add(((TierGlyph)tier).getAnnotStyle().getFeature());
		}

		String method = TrackStyle.getUniqueName(GeneralUtils.URLEncode(meth.toString()));
		//TODO : Remove below conditions afte view mode refactoring is complete.
		if(!method.contains("$.")){
			method += "$.";
		}
		DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(method).setViewMode("default");

		GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(GenometryModel.getGenometryModel().getSelectedSeqGroup(), GeneralLoadView.getLoadView().getSelectedSpecies());
		java.net.URI uri = java.net.URI.create("file:/"+method);

		GenericFeature feature = new GenericFeature(meth.toString(), null, version, new Delegate(uri, meth.toString(), version, operator, symsStr, features), null, false);
		version.addFeature(feature);
		feature.setVisible(); // this should be automatically checked in the feature tree

		ServerList.getServerInstance().fireServerInitEvent(ServerList.getServerInstance().getLocalFilesServer(), LoadUtils.ServerStatus.Initialized, true, true);

//		SeqGroupView.getInstance().setSelectedGroup(feature.gVersion.group.getID());

		GeneralLoadView.getLoadView().createFeaturesTable();

		GeneralLoadUtils.loadAndDisplayAnnotations(feature);
	}

	@Override
	public String getText() {
		return "";
	}
}

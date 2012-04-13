package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.view.SeqMapView;

public class SelectAllAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static SelectAllAction ACTION;
	private static Map<FileTypeCategory, SelectAllAction> CATEGORY_ACTION = 
		new HashMap<FileTypeCategory, SelectAllAction>();
	private FileTypeCategory category;

	public static SelectAllAction getAction() {
		if (ACTION == null) {
			ACTION = new SelectAllAction(Application.getSingleton().getMapView(), null);
		}
		return ACTION;
	}

	public static SelectAllAction getAction(final FileTypeCategory category) {
		SelectAllAction selectAllAction = CATEGORY_ACTION.get(category);
		if (selectAllAction == null) {
			selectAllAction = new SelectAllAction(Application.getSingleton().getMapView(), category);
			CATEGORY_ACTION.put(category, selectAllAction);
		}
		return ACTION;
	}

	protected SelectAllAction(SeqMapView gviewer, FileTypeCategory category) {
		super(gviewer, IGBConstants.BUNDLE.getString("selectAllAction") + (category == null ? "" : category.toString()), null, null);
		this.category = category;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		gviewer.selectAll(category);
	}
}

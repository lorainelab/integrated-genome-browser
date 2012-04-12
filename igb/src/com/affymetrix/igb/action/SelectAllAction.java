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

	public static SelectAllAction getAction(final FileTypeCategory _category) {
		SelectAllAction selectAllAction = CATEGORY_ACTION.get(_category);
		if (selectAllAction == null) {
			selectAllAction = new SelectAllAction(Application.getSingleton().getMapView(), _category) {
				private static final long serialVersionUID = 1L;
				@Override
				public String getText() {
					return IGBConstants.BUNDLE.getString("selectAllAction") + " " + _category.toString();
				}
			};
			CATEGORY_ACTION.put(_category, selectAllAction);
		}
		return ACTION;
	}

	protected SelectAllAction(SeqMapView gviewer, FileTypeCategory category) {
		super(gviewer);
		this.category = category;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		gviewer.selectAll(category);
	}

	@Override
	public String getText() {
		return IGBConstants.BUNDLE.getString("selectAllAction");
	}

	@Override
	public String getIconPath() {
		return null;
	}
}

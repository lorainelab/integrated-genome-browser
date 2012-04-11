package com.affymetrix.igb.action;

import java.util.Map;

import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public final class LoadRefTrackAction extends AbstractLoadFileAction {
	private static final long serialVersionUID = 1L;
	private static final LoadRefTrackAction ACTION = new LoadRefTrackAction();

	public static LoadRefTrackAction getAction() {
		return ACTION;
	}

	@Override
	protected void addSupportedFiles() {
		Map<String, String[]> nameToExtensionMap = FileTypeHolder.getInstance().getSequenceToExtensionMap();
		for (String name : nameToExtensionMap.keySet()) {
			chooser.addChoosableFileFilter(new UniFileFilter(
					nameToExtensionMap.get(name),
					name + " Files"));
		}
	}

	@Override
	protected String getID() {
		return "openRefSeq";
	}

	@Override
	protected String getFriendlyNameID() {
		return "openRefSeq";
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Open16.gif";
	}

	@Override
	public String getText() {
		return BUNDLE.getString("openRefTrackFile");
	}

	@Override
	protected boolean loadSequenceAsTrack() {
		return false;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}

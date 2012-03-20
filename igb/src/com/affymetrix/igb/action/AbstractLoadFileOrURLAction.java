
package com.affymetrix.igb.action;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.OpenURIAction;


/**
 *
 * @author hiralv
 */
public abstract class AbstractLoadFileOrURLAction extends OpenURIAction {
	private static final long serialVersionUID = 1L;

	public AbstractLoadFileOrURLAction(){
		super(IGBServiceImpl.getInstance());
	}
	
	@Override
	protected void addSupportedFiles() {
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap();
		for (String name : nameToExtensionMap.keySet()) {
			chooser.addChoosableFileFilter(new UniFileFilter(
					nameToExtensionMap.get(name).toArray(new String[]{}),
					name + " Files"));
		}
		
		chooser.addChoosableFileFilter(new UniFileFilter(
				new String[]{"igb", "py"},
				"Script File"));
	}

	@Override
	protected String getFriendlyNameID(){
		return "openURI";
	}
	
	@Override
	protected boolean loadSequenceAsTrack() {
		return true;
	}
	
	protected abstract String getID();
}

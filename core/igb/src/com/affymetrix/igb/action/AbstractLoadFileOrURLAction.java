
package com.affymetrix.igb.action;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessorHolder;
import com.affymetrix.igb.IGBServiceImpl;
import com.affymetrix.igb.shared.OpenURIAction;


/**
 *
 * @author hiralv
 */
public abstract class AbstractLoadFileOrURLAction extends OpenURIAction {
	private static final long serialVersionUID = 1L;

	
	public AbstractLoadFileOrURLAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup){
		super(IGBServiceImpl.getInstance(), text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
	}
	
	@Override
	protected void addSupportedFiles() {
		Map<String, List<String>> nameToExtensionMap = FileTypeHolder.getInstance().getNameToExtensionMap(null);
		for (String name : nameToExtensionMap.keySet()) {
			chooser.addChoosableFileFilter(new UniFileFilter(
					nameToExtensionMap.get(name).toArray(new String[]{}), name + " Files"));
		}
		chooser.addChoosableFileFilter(new UniFileFilter(
				ScriptProcessorHolder.getInstance().getScriptExtensions(), "Script File"));
	}

	@Override
	protected String getFriendlyNameID(){
		return "openURI";
	}
	
	@Override
	protected boolean loadSequenceAsTrack() {
		return !chooser.optionChooser.getLoadAsSeqCB().isSelected();
	}
	
	protected abstract String getID();
}

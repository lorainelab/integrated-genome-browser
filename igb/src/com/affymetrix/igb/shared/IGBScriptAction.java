/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;

/**
 *
 * @author dcnorris
 */
public class IGBScriptAction {
	
	public static void executeScriptAction(String tutorialArg) {
		RecordPlaybackHolder.getInstance().doSingleAction(tutorialArg);
	}
}

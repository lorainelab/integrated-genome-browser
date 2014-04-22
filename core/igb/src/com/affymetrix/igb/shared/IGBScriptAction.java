/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import com.affymetrix.igb.swing.ScriptManager;

/**
 *
 * @author dcnorris
 */
public class IGBScriptAction {
	
	public static void executeScriptAction(String tutorialArg) {
		ScriptManager.getInstance().runScriptString(tutorialArg, "igb");
	}
}

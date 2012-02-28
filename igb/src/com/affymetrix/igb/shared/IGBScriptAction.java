/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.shared;

import com.affymetrix.igb.util.ScriptFileLoader;

/**
 *
 * @author dcnorris
 */
public class IGBScriptAction {

	private static final long serialVersionUID = 1L;
	
	public static void executeScriptAction(String tutorialArg) {
		ScriptFileLoader.doSingleAction(tutorialArg);
	}
}

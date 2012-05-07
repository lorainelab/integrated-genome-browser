package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genoviz.swing.recordplayback.JRPFileChooser;
import com.affymetrix.genoviz.swing.recordplayback.ScriptManager;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessor;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessorHolder;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.JFileChooser;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class SaveScriptAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final SaveScriptAction ACTION = new SaveScriptAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SaveScriptAction getAction() {
		return ACTION;
	}

	private SaveScriptAction() {
		super(BUNDLE.getString("saveScript"), null, null, null, KeyEvent.VK_C, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		JRPFileChooser chooser = new JRPFileChooser("saveScript");
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new UniFileFilter(
				ScriptProcessorHolder.getInstance().getScriptExtensions(),
				"Script File"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.rescanCurrentDirectory();
		int option = chooser.showSaveDialog(Application.getSingleton().getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				File f = chooser.getSelectedFile();
				String path = f.getAbsolutePath();
				int pos = path.lastIndexOf('.');
				String extension = path.substring(pos + 1);
				ScriptProcessor scriptManager = ScriptProcessorHolder.getInstance().getScriptProcessor(extension);
				FileWriter fstream = new FileWriter(f);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(ScriptManager.getInstance().getScript(scriptManager));
				out.close();
			}
			catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving script to file", x);
			}
		}
	}
}

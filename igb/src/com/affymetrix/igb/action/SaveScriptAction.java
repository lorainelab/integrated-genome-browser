package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;
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

	public static SaveScriptAction getAction() {
		return ACTION;
	}

	private SaveScriptAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		JFileChooser chooser = new UniFileChooser("Python File", "py");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.rescanCurrentDirectory();
		int option = chooser.showSaveDialog(Application.getSingleton().getFrame().getContentPane());
		if (option == JFileChooser.APPROVE_OPTION) {
			try {
				File f = chooser.getSelectedFile();
				FileWriter fstream = new FileWriter(f);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(RecordPlaybackHolder.getInstance().getScript());
				out.close();
			}
			catch (Exception x) {
				ErrorHandler.errorPanel("ERROR", "Error saving script to file", x);
			}
		}
	}

	@Override
	public String getText() {
		return BUNDLE.getString("saveScript");
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Export16.gif";
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_C;
	}
}

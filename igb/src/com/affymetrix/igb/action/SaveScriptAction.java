package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.RecordPlaybackHolder;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

public class SaveScriptAction extends AbstractAction {
	private static final long serialVersionUID = 1l;


	public SaveScriptAction() {
		super(BUNDLE.getString("saveScript"),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
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
}

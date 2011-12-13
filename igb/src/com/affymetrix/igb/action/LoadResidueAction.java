
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.Point;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/**
 *
 * @author hiralv
 */
public class LoadResidueAction extends GenericAction {

	@Override
	public String getText() {
		return "Load Residue";
	}
	
	public void loadResidue(SeqSpan span, boolean tryFull) {
		Application app = Application.getSingleton();
		JFrame frame = (app == null) ? null : app.getFrame();
		final JDialog dialog = new JDialog(frame, "Loading...");
		JLabel message = new JLabel("Residues are being loaded please wait");
		JProgressBar progressBar = new JProgressBar();
		
		//progressBar.setMaximumSize(new Dimension(150, 5));
		progressBar.setIndeterminate(true);
		
		Box box = Box.createVerticalBox();
		box.add(Box.createGlue());
		box.add(message);
		box.add(progressBar);
		
		dialog.getContentPane().add(box, "Center");
		
		//dialog.setUndecorated(true);
		dialog.setResizable(false);
		dialog.validate();
		dialog.pack();
		
		if(frame != null){
			Point location = frame.getLocation();
			dialog.setLocation(location.x + frame.getWidth()/2 - dialog.getWidth()/2, location.y + frame.getHeight()/2 - dialog.getHeight()/2);
		}
		

		dialog.setVisible(true);
		GeneralLoadView.getLoadView().loadResidues(span, tryFull);
		
		dialog.setVisible(false);
		dialog.dispose();
		
		actionDone();
	}
}

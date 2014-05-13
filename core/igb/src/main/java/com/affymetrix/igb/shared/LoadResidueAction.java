
package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.ThreadUtils;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadView;

/**
 *
 * @author hiralv
 */
public class LoadResidueAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	final SeqSpan span;
	final boolean tryFull;
	
	public LoadResidueAction(final SeqSpan span, boolean tryFull){
		super("Load Residue", null, null);
		this.span = span;
		this.tryFull = tryFull;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		
		// Hack for complete sequence
		SeqSpan newSpan = span;
		BioSeq seq = span.getBioSeq();
		if(span.getMin() == seq.getMin() && span.getMax() == seq.getMax()){
			newSpan = new SimpleSeqSpan(span.getMin(), span.getMax() - 1, seq);
		}
		
		//Check if sequence is already loaded
		if (!seq.isAvailable(newSpan)) {
			boolean new_residue_loaded = false;
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

			if (frame != null) {
				dialog.setLocationRelativeTo(frame);
			}


			dialog.setVisible(true);
			new_residue_loaded = GeneralLoadView.getLoadView().loadResidues(span, tryFull);

			dialog.setVisible(false);
			dialog.dispose();
			
			if (new_residue_loaded) {
				ThreadUtils.runOnEventQueue(new Runnable() {

					public void run() {
						final SeqMapView smv = IGB.getSingleton().getMapView();
						smv.setAnnotatedSeq(span.getBioSeq(), true, true, true);
					}
				});
			}
		}

		actionDone();
	}
}

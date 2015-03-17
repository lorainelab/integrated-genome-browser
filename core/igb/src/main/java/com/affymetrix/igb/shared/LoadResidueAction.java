package com.affymetrix.igb.shared;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.ThreadUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.event.ActionEvent;
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

    private static final long serialVersionUID = 1L;
    final SeqSpan span;
    final boolean tryFull;

    public LoadResidueAction(final SeqSpan span, boolean tryFull) {
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
        if (span.getMin() == seq.getMin() && span.getMax() == seq.getMax()) {
            newSpan = new SimpleSeqSpan(span.getMin(), span.getMax() - 1, seq);
        }

        //Check if sequence is already loaded
        if (!seq.isAvailable(newSpan)) {
            boolean new_residue_loaded = false;
            IGB app = IGB.getInstance();
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
                ThreadUtils.runOnEventQueue(() -> {
                    final SeqMapView smv = IGB.getInstance().getMapView();
                    smv.setAnnotatedSeq(span.getBioSeq(), true, true, true);
                });
            }
        }

        actionDone();
    }
}

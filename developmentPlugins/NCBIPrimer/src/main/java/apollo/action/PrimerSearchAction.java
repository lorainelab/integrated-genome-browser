package apollo.action;

import apollo.analysis.NCBIPrimerBlastOpts;
import apollo.analysis.RemotePrimerBlastNCBI;
import apollo.datamodel.Sequence;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.lorainelab.igb.genoviz.extensions.api.SeqMapViewI;
import com.affymetrix.igb.shared.SequenceLoader;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author hiralv
 */
public class PrimerSearchAction extends GenericAction {

    private static final long serialVersionUID = 1l;

    private final SeqMapViewI smv;
    private final NCBIPrimerBlastOpts ncbiPrimerBlastOpts;

    public PrimerSearchAction(SeqMapViewI smv, NCBIPrimerBlastOpts ncbiPrimerBlastOpts) {
        super("Primer Blast Refseq mRNA (refseq_rna)", "16x16/actions/ncbi.png", null);
        this.smv = smv;
        this.ncbiPrimerBlastOpts = ncbiPrimerBlastOpts;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        try {
            final SeqSymmetry residues_sym = getResidueSym();
            final BioSeq aseq = smv.getAnnotatedSeq();

            final GenericActionDoneCallback doneback = new GenericActionDoneCallback() {
                public void actionDone(GenericAction action) {
                    try {
                        String residues = SeqUtils.getResidues(residues_sym, aseq);
                        Sequence seq = new Sequence(aseq.getID(), residues);

                        StrandedFeatureSetI sf = new StrandedFeatureSet();
                        sf.setRefSequence(seq);

                        RemotePrimerBlastNCBI primer = new RemotePrimerBlastNCBI(ncbiPrimerBlastOpts.getOptions());
                        String url = primer.runAnalysis(sf, seq);

                        GeneralUtils.browse(url);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };

            SequenceLoader worker = new SequenceLoader("RemoteBlastNCBI", residues_sym.getSpan(aseq), doneback);
            CThreadHolder.getInstance().execute(this, worker);
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem occured in copying sequences to sequence viewer", ex, Level.WARNING);
        }
    }

    private SeqSymmetry getResidueSym() {
        SeqSymmetry residues_sym = null;
        List<SeqSymmetry> syms = smv.getSelectedSyms();
        if (syms.size() >= 1) {
            if (syms.size() == 1) {
                residues_sym = syms.get(0);
            } else {
                BioSeq aseq = smv.getAnnotatedSeq();
                if (syms.size() > 1 || smv.getSeqSymmetry() != null) {
                    MutableSeqSymmetry temp = new SimpleMutableSeqSymmetry();
                    SeqUtils.union(syms, temp, aseq);

                    // Make all reverse children forward
                    residues_sym = new SimpleMutableSeqSymmetry();
                    SeqSymmetry child;
                    SeqSpan childSpan;
                    for (int i = 0; i < temp.getChildCount(); i++) {
                        child = temp.getChild(i);
                        childSpan = child.getSpan(aseq);
                        ((MutableSeqSymmetry) residues_sym).addChild(new MutableSingletonSeqSymmetry(childSpan.getMin(), childSpan.getMax(), childSpan.getBioSeq()));
                    }
                    ((MutableSeqSymmetry) residues_sym).addSpan(temp.getSpan(aseq));
                }
            }
        } else {
            residues_sym = smv.getSeqSymmetry();
        }
        return residues_sym;
    }
}

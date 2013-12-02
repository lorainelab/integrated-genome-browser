package apollo.action;

import apollo.analysis.RemoteBlastNCBI;
import apollo.datamodel.Sequence;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionDoneCallback;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.SequenceLoader;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author hiralv
 */
public abstract class BlastSearchAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	
	protected final SeqMapViewI smv;
	protected final RemoteBlastNCBI.BlastType blastType;
	
	public BlastSearchAction(SeqMapViewI smv, RemoteBlastNCBI.BlastType blastType) {
		super(blastType.toString().toUpperCase() + " nr protein database", null, null);
		this.smv = smv;
		this.blastType = blastType;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			final SeqSymmetry residues_sym = getResidueSym();
			final BioSeq aseq = smv.getAnnotatedSeq();
			final SeqSpan span = residues_sym.getSpan(aseq);
			
			final GenericActionDoneCallback doneback = new GenericActionDoneCallback() {
				public void actionDone(GenericAction action) {
					try {
						StrandedFeatureSetI sf = new StrandedFeatureSet();
						String id = MessageFormat.format("{0} {1}:{2}-{3} {4} {5}", 
								new Object[]{residues_sym.getID(), aseq, span.getStart(), span.getEnd(), span.isForward() ? "+" : "-", aseq.getSeqGroup().getOrganism()});
						Sequence seq = new Sequence(id, getSequence(residues_sym));
						
						RemoteBlastNCBI blast = new RemoteBlastNCBI(blastType, new RemoteBlastNCBI.BlastOptions());
						String url = blast.runAnalysis(sf, seq, 1);
						
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
	
    public abstract String getSequence(SeqSymmetry residues_sym);
	
	protected SeqSymmetry getResidueSym() {
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

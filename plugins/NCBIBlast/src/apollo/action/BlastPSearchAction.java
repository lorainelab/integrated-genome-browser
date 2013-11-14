package apollo.action;

import apollo.analysis.RemoteBlastNCBI;
import com.affymetrix.genometryImpl.AminoAcid;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public class BlastPSearchAction extends BlastSearchAction {
	private static final long serialVersionUID = 1l;
	
	public BlastPSearchAction(SeqMapViewI smv) {
		super(smv, RemoteBlastNCBI.BlastType.blastp);
	}

	@Override
	protected SeqSymmetry getResidueSym() {
		SeqSymmetry residues_sym = super.getResidueSym();
		SeqSpan span = residues_sym.getSpan(smv.getAnnotatedSeq());
		int remainder = span.getLength() % 3;
		if(remainder != 0) {
			if(span.isForward()) {
				residues_sym = new SingletonSeqSymmetry(span.getMin(), span.getMax() + 3 - remainder, span.getBioSeq());
			} else {
				residues_sym = new SingletonSeqSymmetry(span.getMax() + 3 - remainder, span.getMin(), span.getBioSeq());
			}
		}
		return residues_sym;
	}
	
	@Override
	public String getSequence(SeqSymmetry residues_sym) {
		SeqSpan span = residues_sym.getSpan(smv.getAnnotatedSeq());
		String residues = SeqUtils.getResidues(residues_sym, smv.getAnnotatedSeq());
		return AminoAcid.getAminoAcid(residues, 1, span.isForward(), "");
	}
}


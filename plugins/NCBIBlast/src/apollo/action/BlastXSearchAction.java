package apollo.action;

import apollo.analysis.RemoteBlastNCBI;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.osgi.service.SeqMapViewI;

/**
 *
 * @author hiralv
 */
public class BlastXSearchAction extends BlastSearchAction {
	private static final long serialVersionUID = 1l;
		
	public BlastXSearchAction(SeqMapViewI smv) {
		super(smv, RemoteBlastNCBI.BlastType.blastx);
	}

	@Override
	public String getSequence(SeqSymmetry residues_sym) {
		return SeqUtils.getResidues(residues_sym, smv.getAnnotatedSeq());
	}
}

package apollo;

import apollo.action.BlastSearchAction;
import apollo.analysis.BlastRunOpts;
import com.affymetrix.genometryImpl.event.ContextualPopupListener;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author hiralv
 */
public class NCBIBlastPopupListener implements ContextualPopupListener {
	private final SeqMapViewI smv;
	private final BlastRunOpts blastRunOpts;
	
	public NCBIBlastPopupListener(SeqMapViewI smv, BlastRunOpts blastRunOpts){
		this.smv = smv;
		this.blastRunOpts = blastRunOpts;
	}

	@Override
	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
		if (!selected_items.isEmpty() && !(selected_items.get(0) instanceof GraphSym)) {
			JMenuItem remote_ncbi_blast_action = new JMenuItem(new BlastSearchAction(smv, blastRunOpts));
			remote_ncbi_blast_action.setIcon(null);
			popup.add(remote_ncbi_blast_action);
		}
	}
}

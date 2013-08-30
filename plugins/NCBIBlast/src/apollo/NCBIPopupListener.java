package apollo;

import apollo.action.BlastSearchAction;
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
public class NCBIPopupListener implements ContextualPopupListener {

	public NCBIPopupListener(SeqMapViewI smv){
		BlastSearchAction.init(smv);
	}

	@Override
	public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
		if (!selected_items.isEmpty() && !(selected_items.get(0) instanceof GraphSym)) {
			JMenuItem remote_ncbi_blast_action = new JMenuItem(BlastSearchAction.getAction());
			remote_ncbi_blast_action.setIcon(null);
			popup.add(remote_ncbi_blast_action);
		}
	}
}

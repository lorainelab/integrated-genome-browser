package apollo;

import apollo.action.BlastPSearchAction;
import apollo.action.BlastSearchAction;
import apollo.action.BlastXSearchAction;
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

    public NCBIBlastPopupListener(SeqMapViewI smv) {
        this.smv = smv;
    }

    @Override
    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
        if (!selected_items.isEmpty() && !(selected_items.get(0) instanceof GraphSym)) {
            BlastSearchAction blastXAction = new BlastXSearchAction(smv);
            JMenuItem remote_ncbi_blast_action = new JMenuItem(blastXAction);
            remote_ncbi_blast_action.setEnabled(blastXAction.isEnabled());
            popup.add(remote_ncbi_blast_action,14);


            BlastSearchAction blastPAction = new BlastPSearchAction(smv);
            JMenuItem remote_ncbi_blastp_action = new JMenuItem(blastPAction);
            remote_ncbi_blastp_action.setEnabled(blastPAction.isEnabled());
            popup.add(remote_ncbi_blastp_action,16);


        }
    }
}

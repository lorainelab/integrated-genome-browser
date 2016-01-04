package apollo;

import apollo.action.BlastPSearchAction;
import apollo.action.BlastSearchAction;
import apollo.action.BlastXSearchAction;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.event.ContextualPopupListener;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.lorainelab.igb.igb.genoviz.extensions.SeqMapViewI;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author hiralv
 */
public class NCBIBlastPopupListener implements ContextualPopupListener, AxisPopupListener {

    private final SeqMapViewI smv;
    private final BlastSearchAction blastXAction, blastPAction;

    public NCBIBlastPopupListener(SeqMapViewI smv) {
        this.smv = smv;
        blastXAction = new BlastXSearchAction(smv);
        blastPAction = new BlastPSearchAction(smv);
    }

    @Override
    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {
        if (!selected_items.isEmpty() && !(selected_items.get(0) instanceof GraphSym)) {
            JMenuItem remote_ncbi_blast_action = new JMenuItem(blastXAction);
            remote_ncbi_blast_action.setEnabled(blastXAction.isEnabled());
            popup.add(remote_ncbi_blast_action, 14);

            JMenuItem remote_ncbi_blastp_action = new JMenuItem(blastPAction);
            remote_ncbi_blastp_action.setEnabled(blastPAction.isEnabled());
            popup.add(remote_ncbi_blastp_action, 16);

        }
    }

    @Override
    public void addPopup(JPopupMenu popup) {
        popup.add(blastXAction);
        popup.add(blastPAction);
    }
}

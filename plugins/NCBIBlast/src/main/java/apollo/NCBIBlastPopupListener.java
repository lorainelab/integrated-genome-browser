package apollo;

import apollo.action.BlastPSearchAction;
import apollo.action.BlastSearchAction;
import apollo.action.BlastXSearchAction;
import com.affymetrix.genometry.event.AxisPopupListener;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JPopupMenu;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import org.lorainelab.igb.menu.api.AnnotationContextMenuProvider;
import org.lorainelab.igb.menu.api.model.AnnotationContextEvent;
import org.lorainelab.igb.menu.api.model.ContextMenuItem;
import org.lorainelab.igb.menu.api.model.MenuIcon;
import org.lorainelab.igb.menu.api.model.MenuItem;
import org.lorainelab.igb.menu.api.model.MenuSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public class NCBIBlastPopupListener implements AnnotationContextMenuProvider, AxisPopupListener {

    private static final Logger LOG = LoggerFactory.getLogger(NCBIBlastPopupListener.class);
    private static final String BLASTP_NR_PROTEIN_DATABASE = "BLASTP nr protein database";
    private static final String BLASTX_NR_PROTEIN_DATABASE = "BLASTX nr protein database";
    private final SeqMapViewI smv;
    private final BlastSearchAction blastXAction, blastPAction;
    private static final String NCBI_ICONPATH = "ncbi.png";

    public NCBIBlastPopupListener(SeqMapViewI smv) {
        this.smv = smv;
        blastXAction = new BlastXSearchAction(smv);
        blastPAction = new BlastPSearchAction(smv);
    }

    @Override
    public void addPopup(JPopupMenu popup) {
        popup.add(blastXAction);
        popup.add(blastPAction);
    }

    @Override
    public Optional<List<MenuItem>> buildMenuItem(AnnotationContextEvent event) {
        List<SeqSymmetry> selectedItems = event.getSelectedItems();
        if (!selectedItems.isEmpty() && !(selectedItems.get(0) instanceof GraphSym)) {
            ContextMenuItem blastXMenuItem = new ContextMenuItem(BLASTX_NR_PROTEIN_DATABASE, (Void t) -> {
                blastXAction.actionPerformed(null);
                return t;
            });
            ContextMenuItem blastPMenuItem = new ContextMenuItem(BLASTP_NR_PROTEIN_DATABASE, (Void t) -> {
                blastPAction.actionPerformed(null);
                return t;
            });
            List<MenuItem> contextMenuItems = new ArrayList<>();
            if (blastXAction.isEnabled()) {
                blastXMenuItem.setMenuSection(MenuSection.APP);
                try (InputStream resourceAsStream = NCBIBlastPopupListener.class.getClassLoader().getResourceAsStream(NCBI_ICONPATH)) {
                    blastXMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                contextMenuItems.add(blastXMenuItem);
            }
            if (blastPAction.isEnabled()) {
                blastPMenuItem.setMenuSection(MenuSection.APP);
                try (InputStream resourceAsStream = NCBIBlastPopupListener.class.getClassLoader().getResourceAsStream(NCBI_ICONPATH)) {
                    blastPMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                contextMenuItems.add(blastPMenuItem);
            }
            return Optional.of(contextMenuItems);
        }
        return Optional.empty();
    }

}

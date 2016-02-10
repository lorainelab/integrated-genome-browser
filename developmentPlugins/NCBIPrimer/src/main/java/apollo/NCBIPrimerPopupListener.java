package apollo;

import apollo.action.PrimerSearchAction;
import apollo.analysis.NCBIPrimerBlastOpts;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import org.lorainelab.igb.menu.api.AnnotationContextMenuProvider;
import org.lorainelab.igb.menu.api.model.AnnotationContextEvent;
import org.lorainelab.igb.menu.api.model.ContextMenuItem;
import org.lorainelab.igb.menu.api.model.ContextMenuSection;
import org.lorainelab.igb.menu.api.model.MenuIcon;

/**
 *
 * @author hiralv
 */
public class NCBIPrimerPopupListener implements AnnotationContextMenuProvider {

    private static final String NCBI_ICONPATH = "ncbi.png";
    private final SeqMapViewI smv;
    private final NCBIPrimerBlastOpts ncbiPrimerBlastOpts;

    public NCBIPrimerPopupListener(SeqMapViewI smv, NCBIPrimerBlastOpts ncbiPrimerBlastOpts) {
        this.smv = smv;
        this.ncbiPrimerBlastOpts = ncbiPrimerBlastOpts;
    }

    @Override
    public Optional<List<ContextMenuItem>> buildMenuItem(AnnotationContextEvent event) {
        ContextMenuItem primerSearchActionMenuItem = null;
        List<SeqSymmetry> selectedItems = event.getSelectedItems();
        if (!selectedItems.isEmpty() && !(selectedItems.get(0) instanceof GraphSym)) {
            PrimerSearchAction primerSearchAction = new PrimerSearchAction(smv, ncbiPrimerBlastOpts);
            primerSearchActionMenuItem = new ContextMenuItem(PRIMER_MENU_ITEM_TITLE, (Void t) -> {
                primerSearchAction.actionPerformed(null);
                return t;
            });
            primerSearchActionMenuItem.setWeight(MENU_WEIGHT);
            primerSearchActionMenuItem.setMenuSection(ContextMenuSection.APP);
            try (InputStream resourceAsStream = NCBIPrimerPopupListener.class.getClassLoader().getResourceAsStream(NCBI_ICONPATH)) {
                primerSearchActionMenuItem.setMenuIcon(new MenuIcon(resourceAsStream));
            } catch (Exception ex) {
            }
        }
        return Optional.ofNullable(Arrays.asList(primerSearchActionMenuItem));
    }
    private static final String PRIMER_MENU_ITEM_TITLE = "Primer Blast Refseq mRNA (refseq_rna)";
    private static final int MENU_WEIGHT = 18;
}

package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.SHOW_MASK;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.action.ViewReadAlignmentAction;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JPopupMenu;
import org.lorainelab.igb.context.menu.AnnotationContextMenuProvider;
import org.lorainelab.igb.context.menu.MenuSection;
import org.lorainelab.igb.context.menu.model.AnnotationContextEvent;
import org.lorainelab.igb.context.menu.model.ContextMenuItem;

/**
 *
 * @author hiralv
 */
public class ReadAlignmentView implements AnnotationContextMenuProvider {

    public static final String COMPONENT_NAME = "ReadAlignmentView";
    private static final String RESTORE_READ = BUNDLE.getString("restoreAlignment");
    private static final String SHOWM_IS_MATCH = BUNDLE.getString("showMismatch");
    private static final int MENU_WEIGHT = 25;

    public void popupNotify(JPopupMenu popup, List<SeqSymmetry> selected_items, SeqSymmetry primary_sym) {

    }

    @Override
    public Optional<List<ContextMenuItem>> buildMenuItem(AnnotationContextEvent event) {
        final List<SeqSymmetry> selectedItems = event.getSelectedItems();
        if (!selectedItems.isEmpty() && selectedItems.get(0) instanceof SymWithProps) {
            SeqSymmetry selectedSym = selectedItems.get(0);
            SymWithProps symWithProps = (SymWithProps) selectedSym;
            Object prop = symWithProps.getProperty(SHOW_MASK);
            if (prop != null) {
                if (selectedItems.size() == 1) {
                    if (Boolean.parseBoolean(prop.toString())) {
                        ContextMenuItem contextMenuItem = new ContextMenuItem(RESTORE_READ, (Void t) -> {
                            ActionEvent actionEvent = new ActionEvent(null, MENU_WEIGHT, RESTORE_READ);
                            ViewReadAlignmentAction.getReadRestoreAction(selectedItems).actionPerformed(actionEvent);
                            return t;
                        });
                        BioSeq seq = GenometryModel.getInstance().getSelectedSeq().orElse(null);
                        SeqSpan span = selectedSym.getSpan(seq);
                        if (seq.isComplete(span.getMin(), span.getMax())) {
                            contextMenuItem.setWeight(MENU_WEIGHT);
                            contextMenuItem.setMenuSection(MenuSection.SEQUENCE);
                            return Optional.of(Arrays.asList(contextMenuItem));
                        }

                    } else {
                        ContextMenuItem contextMenuItem = new ContextMenuItem(SHOWM_IS_MATCH, (Void t) -> {
                            ActionEvent actionEvent = new ActionEvent(null, MENU_WEIGHT, SHOWM_IS_MATCH);
                            ViewReadAlignmentAction.getMismatchAligmentAction(selectedItems).actionPerformed(actionEvent);
                            return t;
                        });
                        contextMenuItem.setWeight(MENU_WEIGHT);
                        contextMenuItem.setMenuSection(MenuSection.SEQUENCE);
                        return Optional.of(Arrays.asList(contextMenuItem));
                    }
                } else {
                    ContextMenuItem readRestoreActionMenuItem = new ContextMenuItem(RESTORE_READ, (Void t) -> {
                        ActionEvent actionEvent = new ActionEvent(null, MENU_WEIGHT, RESTORE_READ);
                        ViewReadAlignmentAction.getReadRestoreAction(selectedItems).actionPerformed(actionEvent);
                        return t;
                    });
                    ContextMenuItem mismatchAligmentMenuItem = new ContextMenuItem(SHOWM_IS_MATCH, (Void t) -> {
                        ActionEvent actionEvent = new ActionEvent(null, MENU_WEIGHT, SHOWM_IS_MATCH);
                        ViewReadAlignmentAction.getMismatchAligmentAction(selectedItems).actionPerformed(actionEvent);
                        return t;
                    });
                    readRestoreActionMenuItem.setWeight(MENU_WEIGHT);
                    readRestoreActionMenuItem.setMenuSection(MenuSection.SEQUENCE);
                    mismatchAligmentMenuItem.setWeight(MENU_WEIGHT);
                    mismatchAligmentMenuItem.setMenuSection(MenuSection.SEQUENCE);
                    return Optional.of(Arrays.asList(readRestoreActionMenuItem, mismatchAligmentMenuItem));
                }
            }
        }
        return Optional.empty();
    }

}

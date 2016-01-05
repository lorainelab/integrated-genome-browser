package org.lorainelab.igb.snp.convert.action;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.igb.swing.JRPMenuItem;
import org.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import org.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import org.lorainelab.igb.snp.convert.ui.SnpConverterFrame;
import java.awt.event.ActionEvent;

/**
 *
 * @author dcnorris
 */
@Component(name = SnpConverterAction.COMPONENT_NAME, immediate = true, provide = {GenericAction.class, IgbMenuItemProvider.class})
public class SnpConverterAction extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "SnpConverterAction";
    private static final int MENU_WEIGHT = 6;
    private SnpConverterFrame snpConverterFrame;
    private JRPMenuItem snpConverterMenuItem;

    public SnpConverterAction() {
        super("23andMe SNP Converter", null, null);
    }

    @Activate
    public void activate() {
        snpConverterMenuItem = new JRPMenuItem("snpConvertMenuItem", this);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        snpConverterFrame.setVisible(true);
    }

    @Reference
    public void setSnpConverterFrame(SnpConverterFrame snpConverterFrame) {
        this.snpConverterFrame = snpConverterFrame;
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.TOOLS;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return snpConverterMenuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return MENU_WEIGHT;
    }
}

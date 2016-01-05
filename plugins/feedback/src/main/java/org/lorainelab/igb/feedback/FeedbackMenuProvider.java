/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.feedback;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.igb.swing.JRPMenuItem;
import org.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import org.lorainelab.igb.services.window.menus.IgbToolBarParentMenu;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;

/**
 *
 * @author dcnorris
 */
@Component(name = FeedbackMenuProvider.COMPONENT_NAME, immediate = true, provide = IgbMenuItemProvider.class)
public class FeedbackMenuProvider extends GenericAction implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "FeedbackMenuProvider";
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("bundle");
    private final int menuWeight;
    private final JRPMenuItem menuItem;
    private FeedbackWidget feedbackWidget;
    private static final String BIOVIZ_HELP_PAGE = "http://bioviz.org/igb/help.html";

    public FeedbackMenuProvider() {
        super(BUNDLE.getString("menu.name"),
                BUNDLE.getString("menu.tooltip"),
                "16x16/actions/help.png",
                "22x22/actions/help.png",
                KeyEvent.VK_UNDEFINED);
        menuWeight = Integer.parseInt(BUNDLE.getString("menu.weight"));
        feedbackWidget = new FeedbackWidget();
//        menuItem = new JRPMenuItem(getProperty("menu.name"), this, menuWeight);
        menuItem = new JRPMenuItem(BUNDLE.getString("menu.name"), new AbstractAction(BUNDLE.getString("menu.name")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                GeneralUtils.openWebpage(BIOVIZ_HELP_PAGE);
            }
        });
    }

    @Override
    public IgbToolBarParentMenu getParentMenu() {
        return IgbToolBarParentMenu.HELP;
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return menuItem;
    }

    @Override
    public int getMenuItemWeight() {
        return menuWeight;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        feedbackWidget.showPanel();
    }

}

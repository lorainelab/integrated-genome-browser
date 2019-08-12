package com.affymetrix.igb.action;


import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open a window showing information about Integrated Genome Browser.
 *
 * @author pruthakulkarni
 */
public class ShowConsoleAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ShowConsoleAction ACTION = new ShowConsoleAction();
    private static final Logger logger = LoggerFactory.getLogger(ShowConsoleAction.class);

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowConsoleAction getAction() {
        return ACTION;
    }

    private ShowConsoleAction() {
        super(BUNDLE.getString("showConsole"), null,
                "16x16/actions/cosole.png",
                "22x22/actions/cosole.png",
                KeyEvent.VK_A, null, false);
        this.ordinal = 100;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        IgbTabPanel consoleTab = (IGB.getInstance()).getViewByDisplayName("Console");
        (IGB.getInstance()).getWindowService().setTabStateAndMenu(consoleTab, IgbTabPanel.TabState.COMPONENT_STATE_WINDOW);
    }



}

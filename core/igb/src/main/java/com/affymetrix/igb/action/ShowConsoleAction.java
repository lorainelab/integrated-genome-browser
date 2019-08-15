package com.affymetrix.igb.action;


import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import org.lorainelab.igb.services.window.tabs.IgbTabPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open a window showing logging information about Integrated Genome Browser.
 * The action is triggered on selection of Help> Show Console on the IGB platform.
 * Show console action selection will open IGB Console Tab (../../plugins/ConsoleTab) in the window mode.
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
        super(BUNDLE.getString("showConsole"),
                "16x16/actions/console.png",
                "22x22/actions/console.png");
        
        this.ordinal = 120;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        IgbTabPanel consoleTab = (IGB.getInstance()).getViewByDisplayName("Console");
        (IGB.getInstance()).getWindowService().setTabStateAndMenu(consoleTab, IgbTabPanel.TabState.COMPONENT_STATE_WINDOW);
    }



}

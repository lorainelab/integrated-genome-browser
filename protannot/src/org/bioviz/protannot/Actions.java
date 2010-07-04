package org.bioviz.protannot;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;

/**
 *
 * @author hiralv
 */
class Actions {

	private final ProtAnnotMain protannot;

	Actions(ProtAnnotMain protannot){
		this.protannot = protannot;
	}

	AbstractAction getLoadAction(){
		 AbstractAction load_action = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("openFile")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif")) {

            public void actionPerformed(ActionEvent e) {
                    protannot.doLoadFile();
            }
        };
		load_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_O);
		load_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("openFileTip"));
		return load_action;
	}

	AbstractAction getAddServerAction(){
		AbstractAction add_server = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("addServer")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Add16.gif")){
			public void actionPerformed(ActionEvent e){
				protannot.addServer();
			}
		};
		add_server.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_A);
		add_server.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("addServerTip"));
		return add_server;
	}

	AbstractAction getPrintAction(){
		AbstractAction print_action = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("print")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Print16.gif")){

            public void actionPerformed(ActionEvent e) {
                protannot.print();
            }
        };
        print_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_P);
		print_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("printTip"));
		return print_action;
	}

	AbstractAction getExportAction(){
		AbstractAction export_action = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("export")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif")){

            public void actionPerformed(ActionEvent e) {
                protannot.export();
            }
        };
        export_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_T);
		export_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("exportTip"));
		return export_action;
	}

	AbstractAction getPreferencesAction(){
		AbstractAction preference_action = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("preferences")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Preferences16.gif")){

            public void actionPerformed(ActionEvent e) {
                protannot.colorChooser();
            }
        };
        preference_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_E);
		preference_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("preferencesTip"));
		return preference_action;
	}
	
	AbstractAction getExitAction(){
		AbstractAction quit_action = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("exit")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Stop16.gif")){

            public void actionPerformed(ActionEvent e) {
                protannot.close();
            }
        };
        quit_action.putValue(AbstractAction.MNEMONIC_KEY, KeyEvent.VK_X);
		quit_action.putValue(AbstractAction.SHORT_DESCRIPTION, BUNDLE.getString("exitTip"));
		return quit_action;
	}
}

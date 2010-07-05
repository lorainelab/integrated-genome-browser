package org.bioviz.protannot.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;

public class RequestFeatureAction extends AbstractAction {

private static final long serialVersionUID = 1l;

	public RequestFeatureAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("requestAFeature")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_R);
	}

	public void actionPerformed(ActionEvent e) {
		String u = "https://sourceforge.net/tracker/?limit=25&func=&group_id=129420&atid=714747&status=1&category=1449149";
		GeneralUtils.browse(u);
	}
}

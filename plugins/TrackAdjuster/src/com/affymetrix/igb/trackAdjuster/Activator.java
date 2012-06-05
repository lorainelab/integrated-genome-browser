package com.affymetrix.igb.trackAdjuster;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenu;
import com.affymetrix.genoviz.swing.recordplayback.JRPMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.window.service.WindowActivator;
import org.osgi.framework.BundleActivator;

public class Activator extends WindowActivator implements BundleActivator {

	@Override
	protected IGBTabPanel getPage(final IGBService igbService) {
		TrackAdjusterTabGUI.init(igbService);
		final TrackAdjusterTabGUI trackAdjusterTabGUI = TrackAdjusterTabGUI.getSingleton();
// test
		JRPMenu file_menu = igbService.getMenu("edit");
		int index = file_menu.getItemCount() - 1;
		file_menu.insertSeparator(index);
		MenuUtil.insertIntoMenu(file_menu, new JRPMenuItem("",
			new GenericAction("test",null, null) {
				private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						Popup popup = PopupFactory.getSharedInstance().getPopup(trackAdjusterTabGUI, new TrackPropertiesGUI(), 10, 10);
						popup.show();
					}
				}
			),
		index);
// end test
		return trackAdjusterTabGUI;
	}
}

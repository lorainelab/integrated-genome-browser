package com.affymetrix.igb.external;

import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.osgi.service.IGBService;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Base Browser View Component.
 * Shows the view of the current region in other browsers
 * 
 * 
 * @author Ido M. Tamir
 */
public abstract class BrowserView extends JPanel {
	private static final long serialVersionUID = 1L;
	private SwingWorker<Image, Void> worker = null;
	private final Map<String, String> cookieMap = new HashMap<String, String>();
	protected final JRPButton update_button;
	protected final JRPButton settingsButton;
	private BrowserImage browserImage = new BrowserImage();
	private final JScrollPane scroll = new JScrollPane();
//	private final IGBService igbService;
	private final UCSCViewAction ucscViewAction;

	public abstract JDialog getViewHelper(Window window);

	public abstract void initializeCookies();

	public abstract Image getImage(Loc loc, int pixWidth);

	public abstract String getViewName();

	public String getCookie(String key) {
		String value = cookieMap.get(key);
		if (value != null) {
			return value;
		}
		return "";
	}
	
	public Loc getLoc(){
		String ucscQuery = ucscViewAction.getUCSCQuery();
		Loc loc = Loc.fromUCSCQuery(ucscQuery);
		return loc;
	}

	public void setCookie(String key, String value) {
		cookieMap.put(key, value);
	}

	public BrowserView(JComboBox selector,final IGBService igbService,final UCSCViewAction ucscViewAction) {
		super();
//		this.igbService = igbService;
		update_button = new JRPButton(getClass().getSimpleName() + "_updateButton", ExternalViewer.BUNDLE.getString("update"));
		settingsButton = new JRPButton(getClass().getSimpleName() + "_settingsButton", ExternalViewer.BUNDLE.getString("settings"));
		this.ucscViewAction = ucscViewAction;
		initializeCookies();
		this.setLayout(new BorderLayout());
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		scroll.setViewportView(browserImage);
		scroll.getVerticalScrollBar().setEnabled(true);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.add("Center", scroll);
		this.add("South", buttonPanel);

		buttonPanel.add(settingsButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(selector);
		buttonPanel.add(Box.createHorizontalStrut(15));
		buttonPanel.add(update_button);
		
		

		update_button.setToolTipText(ExternalViewer.BUNDLE.getString("updateViewTT"));
		update_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (worker != null) {
					worker.cancel(true);
				}
				final String msg = MessageFormat.format(ExternalViewer.BUNDLE.getString("updatingMessage"), getViewName());
				igbService.addNotLockedUpMsg(msg);
				final int pixWidth = scroll.getViewport().getWidth();
				worker = new SwingWorker<Image, Void>() {

					@Override
					public Image doInBackground() {
						String ucscQuery = ucscViewAction.getUCSCQuery();
						Loc loc = Loc.fromUCSCQuery(ucscQuery);
						if(ucscQuery.length() == 0 || loc.db.length() == 0){
							return BrowserLoader.createErrorImage(ExternalViewer.BUNDLE.getString("resolveError"), pixWidth);
						}
						return getImage(loc, pixWidth);
					}

					@Override
					public void done() {
						try {
							Image image = get();
							browserImage = new BrowserImage();
							browserImage.setImage(image);
							scroll.setViewportView(browserImage);
						} catch (InterruptedException ignore) {
						} catch (CancellationException ignore) {
						} catch (java.util.concurrent.ExecutionException e) {
							String why = null;
							Throwable cause = e.getCause();
							if (cause != null) {
								why = cause.getMessage();
							} else {
								why = e.getMessage();
							}
							Logger.getLogger(BrowserView.class.getName()).log(Level.FINE, why);
						} finally {
							igbService.removeNotLockedUpMsg(msg);
						}
					}
				};
				worker.execute();
			}
		});

		settingsButton.setToolTipText(ExternalViewer.BUNDLE.getString("personalViewTT"));
		settingsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				final Window window = SwingUtilities.getWindowAncestor(BrowserView.this);
				final JDialog helper = getViewHelper(window);
				helper.setSize(500, 400);
				helper.setModalityType(ModalityType.DOCUMENT_MODAL);

				helper.setVisible(true);
			}
		});
	}
}


package com.affymetrix.igb.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import com.jidesoft.status.MemoryStatusBarItem;
import com.affymetrix.common.CommonUtils;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.thread.CThreadEvent;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadListener;
import com.affymetrix.genometryImpl.util.DisplaysError;
import com.affymetrix.genometryImpl.util.StatusAlert;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.CancelAllAction;
import com.affymetrix.igb.action.ThreadHandlerAction;
import java.awt.FontMetrics;
import java.awt.font.TextAttribute;
import java.util.HashMap;

public final class StatusBar extends JPanel implements DisplaysError, CThreadListener {
	private static final long serialVersionUID = 1l;
	private static final HashMap<TextAttribute, Object> textAttrMap = new HashMap<TextAttribute, Object>();
	static {
		textAttrMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		textAttrMap.put(TextAttribute.FOREGROUND, Color.BLUE);
	}
	
//	private static final ImageIcon closeIcon = CommonUtils.getInstance().getIcon("16x16/actions/stop.png");
	private static final ImageIcon errorIcon = CommonUtils.getInstance().getIcon("16x16/actions/stop_hex.gif");
	private static final ImageIcon warningIcon = CommonUtils.getInstance().getIcon("16x16/actions/warning.png");
	private static final ImageIcon infoIcon = CommonUtils.getInstance().getIcon("16x16/actions/info.gif");
	private final JLabel status_ta, messageIcon;
	private final MemoryStatusBarItem memory_item;
	private final JRPButton mainCancel;
	private final JButton statusAlertButton;
	private final JPanel progressPanel;
	private final JProgressBar progressBar;
//	private final JPanel selectionPanel;
//	private final JLabel selLabel;
//	private final JTextField selField;
	private StatusAlert alert;
			
	public StatusBar(final ActionListener statusListener) {
		String tt_status = "Shows Selected Item, or other Message";
//		selLabel = new JLabel(" Selection Info: ");
//		selField = new JTextField(20);
//		selField.setEditable(false);
//		selectionPanel = new JPanel();
//		selectionPanel.add(selLabel);
//		selectionPanel.add(selField);
		status_ta = new JLabel("");
		status_ta.setForeground(Color.black);
		progressPanel = new JPanel();
		progressBar = new JProgressBar();
		memory_item = new MemoryStatusBarItem();
		memory_item.setShowMaxMemory(true);
		statusAlertButton = new JButton();
		statusAlertButton.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel = new JRPButton("StatusBar_mainCancel", CancelAllAction.getAction());
		messageIcon = new JLabel();
		messageIcon.setVisible(false);
		messageIcon.setEnabled(false);
		messageIcon.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setHideActionText(true);
		progressBar.setIndeterminate(true);
//		ThreadHandler.getThreadHandler().addPopupHandler(mainCancel);
		CThreadHolder.getInstance().addListener(this);
		progressBar.addMouseListener(
			new MouseAdapter() {
			    public void mouseClicked(MouseEvent e) {
			    	ThreadHandlerAction.getAction().actionPerformed(null);
			    }
			}
		);
		
		statusAlertButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int ret = alert.actionPerformed();
				statusListener.actionPerformed(new ActionEvent(alert, e.getID(), ""+ret));
			}
		});
			
		status_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//		selField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//		selLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//		selectionPanel.setMinimumSize(new Dimension(420, 1));
//		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

		status_ta.setToolTipText(tt_status);
		progressBar.setMaximumSize(new Dimension(150, 5));
		displayProgress(false);
//		progressBar.setVisible(false);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(false);
	
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(mainCancel)
				.addComponent(progressBar)
				.addComponent(messageIcon)
				.addComponent(status_ta)
//				.addGap(1, 250, Short.MAX_VALUE)
//				.addComponent(selectionPanel)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(memory_item, 1, 200, 200)
				.addComponent(statusAlertButton)
				.addGap(5, 5, 5));

		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(mainCancel)
				.addComponent(progressBar)
				.addComponent(messageIcon)
				.addComponent(status_ta)
//				.addComponent(selectionPanel)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(memory_item)
				.addComponent(statusAlertButton)
				.addGap(5, 5, 5));
		
	}

	/** Sets the String in the status bar.
	 *  HTML can be used if prefixed with "<html>".
	 *  Can be safely called from any thread.
	 *  @param s  a String, null is ok; null will erase the status String.
	 */
	public final void setStatus(String s) {
		if (s == null) {
			s = "";
		}
		
		// Adjust the message to fit with status bar
		int availablePixelSpace = memory_item.getBounds().x - status_ta.getBounds().x;
		
		FontMetrics fm = status_ta.getFontMetrics(status_ta.getFont());
		int stringPixelWidth = fm.stringWidth(s);
		
		if( availablePixelSpace > 0 && s.length() > 0 && availablePixelSpace < stringPixelWidth) {
			s = s.substring(0, s.length() * availablePixelSpace / stringPixelWidth) + "...";
		}
		// End of adjust
				
		status_ta.setText(s);
	}

	public final void setSelField(String s){
		if(s == null){
			s = "";
		}
//		selField.setText(s);
	}
	
	public final void setStatusAlert(StatusAlert alert){
		this.alert = alert;
		if(alert != null){
			statusAlertButton.setIcon(alert.getIcon());
			if(alert.getIcon() == null) {
				statusAlertButton.setFont(statusAlertButton.getFont().deriveFont(textAttrMap));
				statusAlertButton.setText(alert.getDisplayMessage());
			}
			statusAlertButton.setToolTipText(alert.getToolTip());
			statusAlertButton.setVisible(true);
		}else{
			statusAlertButton.setIcon(null);
			statusAlertButton.setText(null);
			statusAlertButton.setToolTipText(null);
			statusAlertButton.setVisible(false);
		}
		this.revalidate();
	}
	
	public void displayProgress(boolean b) {
		mainCancel.setVisible(b);
		progressBar.setVisible(b);
//		progressBar.setEnabled(b);
//		progressBar.setIndeterminate(b);
	}

	public String getStatus() {
		return status_ta.getText();
	}

	public void showError(String title, String message, List<GenericAction> actions, Level level) {
		final String tempMessage = message;
		if(level.equals(Level.SEVERE)){
			messageIcon.setIcon(errorIcon);
		}
		else if(level.equals(Level.WARNING))	{
			messageIcon.setIcon(warningIcon);
		}
		else if(level.equals(Level.INFO)){
			messageIcon.setIcon(infoIcon);
		}
		messageIcon.setVisible(true);
		messageIcon.setEnabled(true);
		setStatus(tempMessage);
		final Timer timer= new Timer();
		timer.schedule(new TimerTask() {
            public void run() {
				messageIcon.setVisible(false);				
				setStatus(null);
				timer.cancel();
            }
        }, 5000, 5000);
	}

	@Override
	public void heardThreadEvent(CThreadEvent cte) {
		synchronized(progressBar) {
			boolean workerInProgress = CThreadHolder.getInstance().getCThreadWorkerCount() > 0;
			displayProgress(workerInProgress);
		}
	}
}

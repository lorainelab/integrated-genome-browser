package com.affymetrix.igb.view;

import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import com.jidesoft.status.MemoryStatusBarItem;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.DisplaysError;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.CancelAllAction;
import com.affymetrix.igb.action.ThreadHandlerAction;
import java.awt.Color;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public final class StatusBar extends JPanel implements DisplaysError {
	private static final long serialVersionUID = 1l;
	
//	private static final ImageIcon closeIcon = CommonUtils.getInstance().getIcon("images/stop.png");
	private static final ImageIcon alertIcon = CommonUtils.getInstance().getIcon("images/Warning.png");
	private static final ImageIcon stopIcon = CommonUtils.getInstance().getIcon("images/Stop16.gif");
	private static final ImageIcon warningIcon = new ImageIcon("common/resources/images/warning.png");
	private static final ImageIcon infoIcon = new ImageIcon("common/resources/images/info.gif");
	private final JLabel status_ta;
	private final MemoryStatusBarItem memory_item;
	private final JRPButton mainCancel,stopAction;
	private final JButton updateAvailable;
	private final JProgressBar progressBar;
	public static Timer timer;
		
	public StatusBar() {
		String tt_status = "Shows Selected Item, or other Message";
		final String updateMessage = "You might not be on latest revision.";
		final String update = "Update";
		
		status_ta = new JLabel("");
		progressBar = new JProgressBar();
		memory_item = new MemoryStatusBarItem();
		memory_item.setShowMaxMemory(true);
		updateAvailable = new JButton(alertIcon);
		updateAvailable.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		updateAvailable.setVisible(CommonUtils.getInstance().getUpdateAvailable());
		mainCancel = new JRPButton("StatusBar_mainCancel", CancelAllAction.getAction());
		stopAction = new JRPButton("StatusBar_stopAction", stopIcon);
		stopAction.setVisible(false);
		stopAction.setEnabled(false);
		stopAction.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setHideActionText(true);
//		ThreadHandler.getThreadHandler().addPopupHandler(mainCancel);
		progressBar.addMouseListener(
			new MouseAdapter() {
			    public void mouseClicked(MouseEvent e) {
			    	ThreadHandlerAction.getAction().actionPerformed(null);
			    }
			}
		);
		
		status_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressBar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	
		status_ta.setToolTipText(tt_status);
		progressBar.setMaximumSize(new Dimension(150, 5));
		displayProgress(false);
//		progressBar.setVisible(false);
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(false);

		updateAvailable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, updateMessage, update, JOptionPane.INFORMATION_MESSAGE);
				updateAvailable.setVisible(false);
			}
		});
				
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(mainCancel)
				.addComponent(progressBar)
				.addComponent(stopAction)
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(memory_item, 1, 200, 200)
				.addComponent(updateAvailable));

		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(mainCancel)
				.addComponent(progressBar)
				.addComponent(stopAction)
				.addComponent(status_ta)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(memory_item)
				.addComponent(updateAvailable));
		
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

		status_ta.setText(s);
	}

	public void displayProgress(boolean b) {
//		progressBar.setVisible(b);
		progressBar.setEnabled(b);
		progressBar.setIndeterminate(b);
	}

	public String getStatus() {
		return status_ta.getText();
	}

	public void showError(JFrame frame, String title, String message, List<GenericAction> actions, Level level) {
		final String tempMessage = message;
		timer= new Timer();
		if(level.equals(Level.SEVERE)){
			status_ta.setForeground(Color.red);
			stopAction.setIcon(stopIcon);
		}
		else if(level.equals(Level.WARNING))	{
			status_ta.setForeground(Color.orange);
			stopAction.setIcon(warningIcon);
		}
		else if(level.equals(Level.INFO)){
			status_ta.setForeground(new Color(30,255,30));
			stopAction.setIcon(infoIcon);
		}
		stopAction.setVisible(true);
		stopAction.setEnabled(true);
		setStatus(tempMessage);
		timer.schedule(new TimerTask() {
            public void run() {
				stopAction.setVisible(false);
				status_ta.setForeground(Color.black);
				setStatus(null);
				timer.cancel();
            }
        }, 5000, 5000);
	}
}

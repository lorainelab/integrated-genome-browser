package com.affymetrix.igb.view;

import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import com.jidesoft.status.MemoryStatusBarItem;
import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.thread.CThreadEvent;
import com.affymetrix.genometryImpl.thread.CThreadHolder;
import com.affymetrix.genometryImpl.thread.CThreadListener;
import com.affymetrix.genometryImpl.thread.CThreadWorker;
import com.affymetrix.genometryImpl.util.DisplaysError;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.CancelAllAction;
import com.affymetrix.igb.action.ThreadHandlerAction;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

public final class StatusBar extends JPanel implements DisplaysError, CThreadListener {
	private static final long serialVersionUID = 1l;
	
//	private static final ImageIcon closeIcon = CommonUtils.getInstance().getIcon("16x16/actions/stop.png");
	private static final ImageIcon alertIcon = CommonUtils.getInstance().getIcon("16x16/actions/warning.png");
	private static final ImageIcon errorIcon = CommonUtils.getInstance().getIcon("16x16/actions/stop_hex.gif");
	private static final ImageIcon warningIcon = CommonUtils.getInstance().getIcon("16x16/actions/warning.png");
	private static final ImageIcon infoIcon = CommonUtils.getInstance().getIcon("16x16/actions/info.gif");
	private static final Color warningColor = new Color(255,127,36);
	private static final Color infoColor = new Color(0,100,0);
	private final JLabel status_ta, messageIcon;
	private final MemoryStatusBarItem memory_item;
	private final JRPButton mainCancel;
	private final JButton updateAvailable;
	private final JPanel progressPanel;
//	private final JPanel selectionPanel;
//	private final JLabel selLabel;
//	private final JTextField selField;
	
		
	public StatusBar() {
		String tt_status = "Shows Selected Item, or other Message";
		final String updateMessage = "You might not be on latest revision.";
		final String update = "Update";
//		selLabel = new JLabel(" Selection Info: ");
//		selField = new JTextField(20);
//		selField.setEditable(false);
//		selectionPanel = new JPanel();
//		selectionPanel.add(selLabel);
//		selectionPanel.add(selField);
		status_ta = new JLabel("");
		progressPanel = new JPanel();
		memory_item = new MemoryStatusBarItem();
		memory_item.setShowMaxMemory(true);
		updateAvailable = new JButton(alertIcon);
		updateAvailable.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		updateAvailable.setVisible(CommonUtils.getInstance().getUpdateAvailable());
		mainCancel = new JRPButton("StatusBar_mainCancel", CancelAllAction.getAction());
		messageIcon = new JLabel();
		messageIcon.setVisible(false);
		messageIcon.setEnabled(false);
		messageIcon.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
		mainCancel.setHideActionText(true);
//		ThreadHandler.getThreadHandler().addPopupHandler(mainCancel);
		CThreadHolder.getInstance().addListener(this);
		progressPanel.addMouseListener(
			new MouseAdapter() {
			    public void mouseClicked(MouseEvent e) {
			    	ThreadHandlerAction.getAction().actionPerformed(null);
			    }
			}
		);
		
		status_ta.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
//		selField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//		selLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//		selectionPanel.setMinimumSize(new Dimension(420, 1));
//		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

		status_ta.setToolTipText(tt_status);
		progressPanel.setMaximumSize(new Dimension(150, 5));
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
				.addComponent(progressPanel)
				.addComponent(messageIcon)
				.addComponent(status_ta)
				.addGap(1, 250, Short.MAX_VALUE)
//				.addComponent(selectionPanel)
				.addGap(1, 1, Short.MAX_VALUE)
				.addComponent(memory_item, 1, 200, 200)
				.addComponent(updateAvailable));

		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(mainCancel)
				.addComponent(progressPanel)
				.addComponent(messageIcon)
				.addComponent(status_ta)
//				.addComponent(selectionPanel)
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

	public final void setSelField(String s){
		if(s == null){
			s = "";
		}
//		selField.setText(s);
	}
	
	public void displayProgress(boolean b) {
		mainCancel.setVisible(b);
		progressPanel.setVisible(b);
//		progressBar.setEnabled(b);
//		progressBar.setIndeterminate(b);
	}

	public String getStatus() {
		return status_ta.getText();
	}

	public void showError(String title, String message, List<GenericAction> actions, Level level) {
		final String tempMessage = message;
		if(level.equals(Level.SEVERE)){
			status_ta.setForeground(Color.red);
			messageIcon.setIcon(errorIcon);
		}
		else if(level.equals(Level.WARNING))	{
			status_ta.setForeground(warningColor);
			messageIcon.setIcon(warningIcon);
		}
		else if(level.equals(Level.INFO)){
			status_ta.setForeground(infoColor);
			messageIcon.setIcon(infoIcon);
		}
		messageIcon.setVisible(true);
		messageIcon.setEnabled(true);
		setStatus(tempMessage);
		final Timer timer= new Timer();
		timer.schedule(new TimerTask() {
            public void run() {
				messageIcon.setVisible(false);
				status_ta.setForeground(Color.black);
				setStatus(null);
				timer.cancel();
            }
        }, 5000, 5000);
	}

	private static final int MAX_PROGRESS_BARS = 5;
	private void rebuildProgress() {
		synchronized(progressPanel) {
			Set<CThreadWorker<?, ?>> workers = new CopyOnWriteArraySet<CThreadWorker<?, ?>>(CThreadHolder.getInstance().getAllCThreadWorkers());
			int workerCount = workers.size();
			boolean workerInProgress = workerCount > 0;
			displayProgress(workerInProgress);
			if (workerInProgress) {
				int barCount = Math.min(workerCount, MAX_PROGRESS_BARS);
				progressPanel.removeAll();
				GridLayout progressLayout = new GridLayout(1, barCount);
				progressPanel.setLayout(progressLayout);
				int width = progressPanel.getWidth() / barCount;
				int count = 0;
				for (CThreadWorker<?, ?> worker : workers) {
					count++;
					if (count > barCount) {
						break;
					}
					else if (count == MAX_PROGRESS_BARS && workerCount > MAX_PROGRESS_BARS) {
						JLabel moreLabel = getMoreLabel(width);
						progressPanel.add(moreLabel);
						moreLabel.repaint();
					}
					else {
						JProgressBar progressBar = getProgressBar(worker, width);
						progressPanel.add(progressBar);
						progressBar.repaint();
					}
				}
				progressPanel.repaint();
			}
		}
	}

	private JLabel getMoreLabel(int width) {
		JLabel moreLabel = new JLabel(". . .", SwingConstants.CENTER);
		moreLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		moreLabel.setMaximumSize(new Dimension(width, 25));
		return moreLabel;
	}

	private JProgressBar getProgressBar(CThreadWorker<?, ?> worker, int width) {
		final JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		progressBar.setMaximumSize(new Dimension(width, 25));
		worker.addPropertyChangeListener(
		     new PropertyChangeListener() {
		         public void propertyChange(PropertyChangeEvent evt) {
		             if ("progress".equals(evt.getPropertyName())) {
		         		if (progressPanel.isShowing()) {
		        			progressBar.setValue((Integer)evt.getNewValue());
		                	progressBar.repaint();
		        		}
		             }
		         }
		     }
		);
		return progressBar;
	}

	@Override
	public void heardThreadEvent(CThreadEvent cte) {
		rebuildProgress();
	}
}

package com.affymetrix.igb.view;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
//import com.sun.awt.AWTUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author hiralv
 */
public class PopupInfo extends JWindow {
	
	private static final Color backgroundColor = new Color(253, 254, 196);
	private final JLabel title;
	private final JTextPane tooltip;
	private final JButton button;
	private final boolean isPinned;
	private boolean preferredLocationSet;
	private Point lastPoint;
	
	private AbstractAction copyAction = new AbstractAction("c",CommonUtils.getInstance().getIcon("/toolbarButtonGraphics/general/Copy16.gif")){
		@Override
		public void actionPerformed(ActionEvent ae) {
			GeneralUtils.copyToClipboard(tooltip.getText());
		}
	};
	
	private AbstractAction unStickAction = new AbstractAction("x",CommonUtils.getInstance().getIcon("16x16/actions/red_close.png")){
		@Override
		public void actionPerformed(ActionEvent ae) {
			setButtonAction(stickAction);
			setVisible(false);
			preferredLocationSet = false;
//			AWTUtilities.setWindowOpacity(PopupInfo.this, 0.5f);
		}
	};
	
	private AbstractAction closeAction = new AbstractAction("x",CommonUtils.getInstance().getIcon("16x16/actions/red_close.png")){
		@Override
		public void actionPerformed(ActionEvent ae) {
			setVisible(false);
			dispose();
		}
	};
	
	private AbstractAction stickAction = new AbstractAction("*",CommonUtils.getInstance().getIcon("16x16/actions/red_pin.png")) {
		@Override
		public void actionPerformed(ActionEvent ae) {
			PopupInfo newWindow = new PopupInfo(getOwner(), true);
			newWindow.title.setText(title.getText());
			newWindow.tooltip.setText(tooltip.getText());
			newWindow.setButtonAction(newWindow.closeAction);
		
			newWindow.pack();
			newWindow.setLocation(getLocationOnScreen());
			newWindow.setVisible(true);
//			AWTUtilities.setWindowOpacity(newWindow, 1.0f);
			
//			timer.stop();
			setVisible(false);
		}
	};
	
	public PopupInfo(Window owner){
		this(owner, false);
	}
	
	private PopupInfo(Window owner, boolean isPinned){
		super(owner);
		
		title = new JLabel();
		tooltip = new JTextPane();
		button = new JButton();
		this.isPinned = isPinned;
		setButtonAction(stickAction);
		init();
	}
	
	public void setToolTip(Point point, String[][] properties){
		if(isVisible() && !preferredLocationSet){
			setVisible(false);
//			AWTUtilities.setWindowOpacity(PopupInfo.this, 0.5f);
		}
//		timer.stop();
		
		// If the main window is not in focus then return else the tooltip window 
		// would grab the focus.
		if(!getOwner().isActive() /*|| !getOwner().isFocused()*/){
			return;
		}
			
		if(properties != null && properties.length > 1){
			//title.setText(getFormattedTitle(properties));
			tooltip.setText(convertPropsToString(properties));
			pack();
			if(!preferredLocationSet){
				setLocation(determineBestLocation(point));
				setVisible(true);
//				timer.setInitialDelay(1000);
//				timer.start();
			}
		}else{
			//title.setText(null);
			tooltip.setText(null);
		}
	}
	
	private static String getFormattedTitle(String[][] properties){
		StringBuilder props = new StringBuilder();
		props.append("<html>");
		props.append("<div align='center'> <b> ").append(getSortString(properties[0][1])).append(" </b> </div>");
		props.append("</html>");
		return props.toString();
	}
	
	/**
	 * Converts given properties into string.
	 */
	private static String convertPropsToString(String[][] properties) {
		StringBuilder props = new StringBuilder();
		props.append("<html>");
		for (int i = 0; i < properties.length; i++) {
			props.append("<b>");
			props.append(properties[i][0]);
			props.append(" : </b>");
			props.append(getSortString(properties[i][1]));
			props.append("<br>");
		}
		props.append("</html>");

		return props.toString();
	}
	
	private static String getSortString(Object str){
		if(str == null){
			return "";
		}
		
		String string = str.toString();
		int strlen = string.length();
		if (strlen < 30) {
			return string;
		}
		
		return " ..." + string.substring(strlen - 25, strlen);
	}
	
	private Point determineBestLocation(Point currentPoint){
		Point bestLocation = new Point(currentPoint.x, currentPoint.y);
	
		if (lastPoint != null) {
			if (currentPoint.x > lastPoint.x) {
				bestLocation.x -= getSize().getWidth();
				bestLocation.x += 10;
			} else {
				bestLocation.x += 5;
			}

			if (currentPoint.y > lastPoint.y) {
				bestLocation.y -= getSize().getHeight();
				bestLocation.y += 10;
			} else {
				bestLocation.y += 5;
			}
		} else {
			bestLocation.x += 5;
			bestLocation.y += 5;
		}
		
		lastPoint = currentPoint;
		
		return bestLocation;
	}
	
	private void setButtonAction(AbstractAction action){
		button.setAction(action);
		if(button.getIcon() != null){
			button.setText(null);
		}
	}
	
	private void init(){
		
		//setAlwaysOnTop(true);
		setBackground(backgroundColor);
		setForeground(backgroundColor);
//		AWTUtilities.setWindowOpacity(PopupInfo.this, 0.5f);
		
		title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		
		tooltip.setBackground(backgroundColor);
		tooltip.setContentType("text/html");
		tooltip.setEditable(false);
		tooltip.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		
		button.setMargin(new Insets(0,0,0,0));
		button.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		
//		JButton copyButton = new JButton(copyAction);
//		if(copyButton.getIcon() != null){
//			copyButton.setText(null);
//		}
//		copyButton.setMargin(new Insets(0,0,0,0));
//		copyButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		
		//Box button_box = new Box(BoxLayout.X_AXIS);
		JPanel button_box = new JPanel();
		BoxLayout button_box_layout = new BoxLayout(button_box, BoxLayout.X_AXIS);
		button_box.setLayout(button_box_layout);
		button_box.setBackground(backgroundColor);
		button_box.setForeground(backgroundColor);
		button_box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		button_box.add(title);
		Component glue = Box.createHorizontalGlue();
		glue.setBackground(backgroundColor);
		glue.setForeground(backgroundColor);
		button_box.add(glue);
//		button_box.add(copyButton);
		button_box.add(button);
		button_box.addMouseListener(move);
		button_box.addMouseMotionListener(move);
			
		Box component_box = new Box(BoxLayout.Y_AXIS);
		component_box.setBackground(backgroundColor);
		component_box.setForeground(backgroundColor);
		component_box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		component_box.add(button_box);
		component_box.add(tooltip);
		
		addMouseListener(resize);
		addMouseMotionListener(resize);
		
		setLayout(new BorderLayout(0,0));
		add(component_box);
		
//		if(!isPinned){
//			Toolkit.getDefaultToolkit().addAWTEventListener(opacityController, AWTEvent.MOUSE_EVENT_MASK);
//		}
	}
	
//	Timer timer = new Timer(100, new ActionListener(){
//
//		public void actionPerformed(ActionEvent e) {
//			float opacity = AWTUtilities.getWindowOpacity(PopupInfo.this);
//			if(opacity + 0.05f >= 1.0f){
//				timer.stop();
//				return;
//			}
//			AWTUtilities.setWindowOpacity(PopupInfo.this, opacity + 0.05f);
//		}
//	});
	
	AWTEventListener opacityController = new AWTEventListener(){
		public void eventDispatched(AWTEvent event) {
			if(event.getSource() instanceof Component && 
					SwingUtilities.isDescendingFrom((Component)event.getSource(), PopupInfo.this)){
				setTranslucency((MouseEvent)event);
			}
		}
		
		private void setTranslucency(MouseEvent e){
			Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), PopupInfo.this);
			if (!PopupInfo.this.isPinned && !PopupInfo.this.preferredLocationSet) {
				if (PopupInfo.this.contains(p)) {
//					AWTUtilities.setWindowOpacity(PopupInfo.this, 1.0f);
				} else {
//					AWTUtilities.setWindowOpacity(PopupInfo.this, 0.5f);
				}
			}
		}
	};
	
	MouseAdapter move = new MouseAdapter(){
		int x_offset, y_offset;
		
		@Override
		public void mousePressed(MouseEvent evt) {
			x_offset = (int) (evt.getLocationOnScreen().getX() - PopupInfo.this.getLocationOnScreen().getX());
			y_offset = (int) (evt.getLocationOnScreen().getY() - PopupInfo.this.getLocationOnScreen().getY());
//			AWTUtilities.setWindowOpacity(PopupInfo.this, 1.0f);
		}

		@Override
		public void mouseReleased(MouseEvent evt) {
			x_offset = 0;
			y_offset = 0;
			if(!PopupInfo.this.isPinned && !PopupInfo.this.preferredLocationSet){
				PopupInfo.this.preferredLocationSet = true;
				PopupInfo.this.setButtonAction(PopupInfo.this.unStickAction);
			}
		}
		
		@Override
		public void mouseDragged(MouseEvent evt) {
			PopupInfo.this.setLocation((int)evt.getLocationOnScreen().getX() - x_offset, (int)evt.getLocationOnScreen().getY() - y_offset);
		}
		
	};
	
	MouseAdapter resize = new MouseAdapter() {
		Point sp;
		int	compStartHeight, compStartWidth;
		int minHeight = 100;
		
		@Override
		public void mouseMoved(MouseEvent e) {
			Point p = e.getPoint();

			if (p.y > PopupInfo.this.getSize().height - 5) {
				setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
			} else if (p.x > PopupInfo.this.getSize().width - 5) {
				setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			} else {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {

			Point p = e.getPoint();
			int compWidth = PopupInfo.this.getSize().width;
			int compHeight = PopupInfo.this.getSize().height;
			
			if (getCursor().getType() == Cursor.N_RESIZE_CURSOR) {
				int nextHeight = compStartHeight + p.y - sp.y;
				if (nextHeight > minHeight) {
					setSize(compWidth, nextHeight);
					setVisible(true);
				}
			} else if (getCursor().getType() == Cursor.W_RESIZE_CURSOR) {
				int nextWidth = compStartWidth + p.x - sp.x;
				if (nextWidth > minHeight) {
					setSize(nextWidth, compHeight);
					setVisible(true);
				}
			} else {
				int x = PopupInfo.this.getX() + p.x - sp.x;
				int y = PopupInfo.this.getY() + p.y - sp.y;
				setLocation(x, y);
			}

		}

		@Override
		public void mousePressed(MouseEvent e) {
			sp = e.getPoint();
			compStartHeight = getSize().height;
			compStartWidth = getSize().width;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			sp = null;
		}
			
		@Override
		public void mouseExited(MouseEvent e) {
			if (sp == null) {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	
	};
}

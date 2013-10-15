package com.affymetrix.igb.view;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author hiralv
 */
public class PopupInfo extends JWindow {
	private static final HashMap<TextAttribute, Object> textAttrMap = new HashMap<TextAttribute, Object>();
	static {
		textAttrMap.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		textAttrMap.put(TextAttribute.FOREGROUND, Color.BLUE);
	}	
	private static final Color backgroundColor = new Color(253, 254, 196);
	private static final int minHeight = 100;
	private static final int maxWidth  = 300;
	private final JLabel message;
	private final JTextPane tooltip;
	private final JButton camera, lock, moreLess;
	private final boolean isPinned;
	private boolean preferredLocationSet;
	private int preferredWidth;
	private Point lastPoint;
	private String[][] properties;
	
	private AbstractAction copyAction = new AbstractAction("c",CommonUtils.getInstance().getIcon("/toolbarButtonGraphics/general/Copy16.gif")){
		@Override
		public void actionPerformed(ActionEvent ae) {
			GeneralUtils.copyToClipboard(convertPropsToString(properties, false));
			flashMessage("copied");
		}
	};
		
	private AbstractAction closeAction = new AbstractAction("x",CommonUtils.getInstance().getIcon("16x16/actions/cross.png")){
		@Override
		public void actionPerformed(ActionEvent ae) {
			setVisible(false);
			dispose();
		}
	};
	
	private AbstractAction snapShotAction = new AbstractAction("o",CommonUtils.getInstance().getIcon("16x16/actions/stock_pin.png")) {
		@Override
		public void actionPerformed(ActionEvent ae) {
			PopupInfo newWindow = new PopupInfo(getOwner(), true);
			newWindow.properties = properties;
			newWindow.tooltip.setText(tooltip.getText());
			if(moreLess.getAction() == moreAction){
				newWindow.moreLess.setAction(newWindow.moreAction);
			} else {
				newWindow.moreLess.setAction(newWindow.lessAction);
			}
			newWindow.tooltip.setCaretPosition(0);
			newWindow.setCameraAction(newWindow.closeAction);
		
			newWindow.pack();
			newWindow.setSize(getSize());
			newWindow.setLocation(getLocationOnScreen().x + 10, getLocationOnScreen().y + 10);
			newWindow.setVisible(true);
			Opacity.INSTANCE.set(newWindow, 1.0f);
			
			if(Opacity.INSTANCE.isSupported()){
				timer.stop();
			}
//			setVisible(false);
		}
	};

	private AbstractAction lockAction = new AbstractAction("l",CommonUtils.getInstance().getIcon("16x16/actions/un_lock.png")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			preferredLocationSet = true;
			setLockAction(unlockAction);
		}
	};
	
	private AbstractAction unlockAction = new AbstractAction("u",CommonUtils.getInstance().getIcon("16x16/actions/lock.png")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			preferredLocationSet = false;
			setLockAction(lockAction);
		}
	};
	
	private AbstractAction moreAction = new AbstractAction("More \u00BB") {
		@Override
		public void actionPerformed(final ActionEvent ae) {
			ThreadUtils.runOnEventQueue(new Runnable() {
				public void run() {
					setVisible(false);
					Dimension prevSize = getSize();
					moreLess.setAction(lessAction);
					pack();
					setWidth();
					int change = prevSize.height - getSize().height;
					setLocation(getLocation().x, getLocation().y + change);
					setVisible(true);
				}
			});
		}
	};
	
	private AbstractAction lessAction = new AbstractAction("Less \u00AB"){
		@Override
		public void actionPerformed(final ActionEvent ae) {
			ThreadUtils.runOnEventQueue(new Runnable() {
				public void run() {
					setVisible(false);
					Dimension prevSize = getSize();
					moreLess.setAction(moreAction);
					setSize(getSize().width, minHeight);
					setWidth();
					int change = prevSize.height - getSize().height;
					setLocation(getLocation().x, getLocation().y + change);
					setVisible(true);
				}
			});
		}
	};
	
	public PopupInfo(Window owner){
		this(owner, false);
	}
	
	private PopupInfo(Window owner, boolean isPinned){
		super(owner);
		
		message  = new JLabel();
		tooltip  = new JTextPane();
		camera   = new JButton();
		lock	 = new JButton();
		moreLess = new JButton();
	
		this.isPinned  = isPinned;
		preferredWidth = -1;
		setCameraAction(snapShotAction);
		if(!isPinned) {
			setLockAction(lockAction);
		}
		init();
	}
	
	public void setToolTip(Point point, String[][] properties){
		if(isVisible() && properties == null && !preferredLocationSet){
			setVisible(false);
			Opacity.INSTANCE.set(PopupInfo.this, 0.5f);
		}

		if(Opacity.INSTANCE.isSupported()){
			timer.stop();
		}
		
		// If the main window is not in focus then return else the tooltip window 
		// would grab the focus.
		if(!getOwner().isActive() /*|| !getOwner().isFocused()*/){
			return;
		}
		
		if(properties != null && properties.length > 1){
			this.properties	= properties;
			//title.setText(getFormattedTitle(properties));
			tooltip.setText(convertPropsToString(properties, false));
			boolean wasVisible = isVisible();
			setVisible(false);
			pack();
			if(moreLess.getAction() == moreAction){
				setSize(getSize().width, minHeight);
			}
			setWidth();
			tooltip.setCaretPosition(0);
			setVisible(wasVisible);
			
			if(!preferredLocationSet){
				setLocation(determineBestLocation(point));
				setVisible(true);
				if(Opacity.INSTANCE.isSupported()){
					timer.setInitialDelay(1000);
					timer.start();
				}
			}
		} else if (isVisible() && preferredLocationSet){
			// Do Nothing 
		} else {
			this.properties	= properties;
			//title.setText(null);
			tooltip.setText(null);
		}
	}
	
	private void setWidth() {
		if (getSize().width > maxWidth) {
			setSize(maxWidth, getSize().height);
		}
		if (preferredWidth > 0) {
			setSize(preferredWidth, getSize().height);
		}
	}
	
	private void flashMessage(String str) {
		message.setText(str);
		flashTimer.start();
	}
	
	private static String getFormattedTitle(String[][] properties){
		StringBuilder props = new StringBuilder();
		props.append("<html>");
		props.append("<div align='center'> <b> ").append(getSortString(properties[0][1], 10)).append(" </b> </div>");
		props.append("</html>");
		return props.toString();
	}
	
	/**
	 * Converts given properties into string.
	 */
	private static String convertPropsToString(String[][] properties, boolean sorten) {
		StringBuilder props = new StringBuilder();
		for (int i = 0; i < properties.length; i++) {
			props.append(properties[i][0]).append(" : ");
			props.append(sorten ? getSortString(properties[i][1], 30) : properties[i][1]);
			if(i != properties.length - 1) {
				props.append("\n");
			}
		}
		return props.toString();
	}
	
	private static String getSortString(String string, int length){
		if(string == null){
			return "";
		}
		
		int strlen = string.length();
		if (strlen < length) {
			return string;
		}
		
		return " ..." + string.substring(strlen - length, strlen);
	}
	
	private Point determineBestLocation(Point currentPoint){
		Point bestLocation = new Point(currentPoint.x, currentPoint.y);
	
		if (lastPoint != null) {
			if (currentPoint.x > lastPoint.x) {
				bestLocation.x -= getSize().getWidth();
				bestLocation.x += 10;
			} else {
				bestLocation.x += 10;
			}

			if (currentPoint.y > lastPoint.y) {
				bestLocation.y -= getSize().getHeight();
				bestLocation.y += 10;
			} else {
				bestLocation.y += 10;
			}
		} else {
			bestLocation.x += 10;
			bestLocation.y += 10;
		}
		
		bestLocation.x = bestLocation.x < 25 ? 25 : bestLocation.x;
		bestLocation.y = bestLocation.y < 25 ? 25 : bestLocation.y;
		
		lastPoint = currentPoint;
		
		return bestLocation;
	}
	
	private void setCameraAction(AbstractAction action){
		camera.setAction(action);
		if(camera.getIcon() != null){
			camera.setText(null);
		}
	}
	
	private void setLockAction(AbstractAction action){
		lock.setAction(action);
		if(lock.getIcon() != null){
			lock.setText(null);
		}
	}
	
	private void init(){
		
		//setAlwaysOnTop(true);
		setFocusableWindowState(false);
		setBackground(backgroundColor);
		setForeground(backgroundColor);
		Opacity.INSTANCE.set(PopupInfo.this, 0.5f);
		
		message.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		message.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		message.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		
		tooltip.setBackground(backgroundColor);
		tooltip.setEditable(false);
		tooltip.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		
		JPanel noWrapPanel = new JPanel( new BorderLayout() );
		noWrapPanel.setBackground(backgroundColor);
		noWrapPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		noWrapPanel.add(tooltip);
		
		JScrollPane scrollPane = new JScrollPane(noWrapPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(backgroundColor);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		
		lock.setMargin(new Insets(0,0,0,0));
		lock.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		
		camera.setMargin(new Insets(0,0,0,0));
		camera.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		
		JButton copyButton = new JButton(copyAction);
		if(copyButton.getIcon() != null){
			copyButton.setText(null);
		}
		copyButton.setMargin(new Insets(0,0,0,0));
		copyButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		
		//Box button_box = new Box(BoxLayout.X_AXIS);
		JPanel button_box = new JPanel();
		BoxLayout button_box_layout = new BoxLayout(button_box, BoxLayout.X_AXIS);
		button_box.setLayout(button_box_layout);
		button_box.setBackground(backgroundColor);
		button_box.setForeground(backgroundColor);
		button_box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		Component glueLeft = Box.createHorizontalGlue();
		glueLeft.setBackground(backgroundColor);
		glueLeft.setForeground(backgroundColor);
		Component glueRight = Box.createHorizontalGlue();
		glueRight.setBackground(backgroundColor);
		glueRight.setForeground(backgroundColor);
		Component strutRight = Box.createHorizontalStrut(5);
		strutRight.setBackground(backgroundColor);
		strutRight.setForeground(backgroundColor);
		
		button_box.add(camera);
		button_box.add(strutRight);
		button_box.add(copyButton);
		button_box.add(glueLeft);
		button_box.add(message);
		button_box.add(glueRight);
		button_box.add(lock);
		button_box.addMouseListener(move);
		button_box.addMouseMotionListener(move);
		
		JPanel bottom_box = new JPanel();
		BoxLayout bottom_box_layout = new BoxLayout(bottom_box, BoxLayout.X_AXIS);
		bottom_box.setLayout(bottom_box_layout);
		bottom_box.setBackground(backgroundColor);
		bottom_box.setForeground(backgroundColor);
		bottom_box.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	
		moreLess.setFont(moreLess.getFont().deriveFont(textAttrMap));
		moreLess.setAction(moreAction);
		moreLess.setMargin(new Insets(0,0,0,0));
		moreLess.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		bottom_box.add(moreLess);
		
		Component glue = Box.createHorizontalGlue();
		glue.setBackground(backgroundColor);
		glue.setForeground(backgroundColor);
		bottom_box.add(glue);
		
		Box component_box = new Box(BoxLayout.Y_AXIS);
		component_box.setBackground(backgroundColor);
		component_box.setForeground(backgroundColor);
		component_box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		component_box.add(button_box);
		component_box.add(scrollPane);
		component_box.add(bottom_box);
		
		addMouseListener(resize);
		addMouseMotionListener(resize);
		
		setLayout(new BorderLayout(0,0));
		add(component_box);
		
		if(!isPinned && Opacity.INSTANCE.isSupported()){
			Toolkit.getDefaultToolkit().addAWTEventListener(opacityController, AWTEvent.MOUSE_EVENT_MASK);
		}
	}
	
	Timer flashTimer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			flashTimer.stop();
			message.setText(null);
		}
	});
	
	Timer timer = new Timer(100, new ActionListener(){

		public void actionPerformed(ActionEvent e) {
			float opacity = Opacity.INSTANCE.get(PopupInfo.this);
			if(opacity + 0.05f >= 1.0f){
				timer.stop();
				return;
			}
			Opacity.INSTANCE.set(PopupInfo.this, opacity + 0.05f);
		}
	});
	
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
					Opacity.INSTANCE.set(PopupInfo.this, 1.0f);
				} else {
					Opacity.INSTANCE.set(PopupInfo.this, 0.5f);
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
			Opacity.INSTANCE.set(PopupInfo.this, 1.0f);
		}

		@Override
		public void mouseReleased(MouseEvent evt) {
			x_offset = 0;
			y_offset = 0;
		}
		
		@Override
		public void mouseDragged(MouseEvent evt) {
			PopupInfo.this.setLocation((int)evt.getLocationOnScreen().getX() - x_offset, (int)evt.getLocationOnScreen().getY() - y_offset);
		}
		
	};
	
	MouseAdapter resize = new MouseAdapter() {
		Point sp;
		int	compStartHeight, compStartWidth;
		
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
					preferredWidth = nextWidth;
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
	
	private enum Opacity {
		INSTANCE;
		
		private final boolean isSupported;
		private Method setOpacityMethod, getOpacityMethod;
		
		@SuppressWarnings("unchecked")
		private Opacity() {
			boolean noException = true;
			try {
				Class clazz = Class.forName("com.sun.awt.AWTUtilities");
				setOpacityMethod = clazz.getDeclaredMethod("setWindowOpacity", Window.class, float.class);
				getOpacityMethod = clazz.getDeclaredMethod("getWindowOpacity", Window.class);
				
				// Make dummy call to methods
				JWindow window = new JWindow();
				float opacity = _get(window);
				_set(window, opacity);
			} catch (Exception ex) {
				Logger.getLogger(PopupInfo.class.getName()).log(Level.SEVERE, null, ex);
				noException = false;
			}
			
			isSupported = noException;
		}
		
		public boolean isSupported() {
			return isSupported;
		}
		
		// Catch excetions here
		public void set(Window window, float opacity) {
			if(!isSupported()) {
				return;
			}
			
			try{ 
				_set(window, opacity); 
			} catch (Exception ex){ }
		}
		
		// Catch excetions here
		public float get(Window window) {
			if(!isSupported()) {
				return 1.0f;
			}
			
			try{ 
				return _get(window); 
			} catch (Exception ex){ }
			
			return 1.0f;
		}
		
		private void _set(Window window, float opacity) throws Exception {
			setOpacityMethod.invoke(null, window, opacity);
		}
		
		private float _get(Window window) throws Exception {
			return ((Float) getOpacityMethod.invoke(null, window));
		}
	}
}

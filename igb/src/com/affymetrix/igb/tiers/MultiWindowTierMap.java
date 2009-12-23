package com.affymetrix.igb.tiers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.glyph.SmartRubberBand;
import com.affymetrix.igb.event.VirtualRubberBandEvent;
import java.awt.geom.AffineTransform;

/**
 *  Splits a tiered map across multiple windows on multiple screens.
 */
//   maybe have one parent master map and others use it as root?
//
//   Any methods that only affect scene should not need to be propagated to child windows,
//      since scene is shared by parent and all children
//
public final class MultiWindowTierMap extends AffyTieredMap implements MouseListener, NeoRubberBandListener {
	/*  params for running on 4x2 XGA (1024x768) screens */

	public static int tile_width = 1024;
	public static int tile_height = 768;
	public static int tile_columns = 4;
	public static int tile_rows = 2;

	/*  params for testing on single screen, 2x2 400x300 windows
	public static int tile_width = 400;
	public static int tile_height = 300;
	public static int tile_columns = 2;
	public static int tile_rows = 2;
	 */
	public static int xbump = 0;
	public static int ybump = 0;
	private int total_width = tile_width * tile_columns;
	private int total_height = tile_height * tile_rows;
	private NeoMap[][] child_maps = new NeoMap[tile_columns][tile_rows];
	private MultiMapMouseHandler child_mouse_handler;
	private List<MouseListener> mlisteners = new CopyOnWriteArrayList<MouseListener>();
	private List<NeoRubberBandListener> rlisteners = new CopyOnWriteArrayList<NeoRubberBandListener>();

	public MultiWindowTierMap(boolean hscroll, boolean vscroll) {
		super(hscroll, vscroll, NeoConstants.HORIZONTAL);
		this.setSize(total_width, total_height);
		this.getNeoCanvas().setSize(total_width, total_height);
		child_mouse_handler = new MultiMapMouseHandler(this);
		initMultiWindows();
	}

	public void updateWidget() {
		updateWidget(false);
	}

	public void updateWidget(boolean full_update) {
		//    super.updateWidget(full_update);
		// set views of child maps based on this map's view
		ViewI rootview = this.getView();
		if (rootview instanceof View) {
			((View) rootview).calcCoordBox();  // just to make sure...
		}
		// transform scale is pixels/coord, transform offset is in pixels
		AffineTransform trans =  rootview.getTransform();
		double xscale = trans.getScaleX();
		double yscale = trans.getScaleY();
		double xoffset = trans.getTranslateX();
		double yoffset = trans.getTranslateY();

		// update all the child maps
		for (int x = 0; x < tile_columns; x++) {
			for (int y = 0; y < tile_rows; y++) {
				NeoMap cmap = child_maps[x][y];

				if (cmap == null) {
					// maps can be null if the initial parameters don't match capabilities
					// of the graphics hardware.
					continue;
				}

				ViewI cview = cmap.getView();
				AffineTransform ctrans =  cview.getTransform();
				ctrans.setTransform(
						xscale,0,0,yscale,xoffset - (x * tile_width),yoffset - (y * tile_height));
				cview.setFullView(rootview);
				cmap.updateWidget();
			}
		}
	}

	private void initMultiWindows() {
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice default_device = genv.getDefaultScreenDevice();
		Rectangle maxbounds = genv.getMaximumWindowBounds();
		Point center = genv.getCenterPoint();
		GraphicsDevice[] devices = genv.getScreenDevices();
		System.out.println("*********  Graphics Configuration *********");
		System.out.println("Graphics Environment: " + genv);
		System.out.println("Screen Devices: " + devices.length);
		System.out.println("Default Screen device: " + default_device);
		System.out.println("max bounds: " + maxbounds);
		System.out.println("center point: " + center);

		int screen_count = 0;
		for (int i = 0; i < devices.length; i++) {
			int type = devices[i].getType();
			if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {
				screen_count++;
			}
		}

		Map<Rectangle, GraphicsConfiguration> bounds2config = new LinkedHashMap<Rectangle, GraphicsConfiguration>();
		Map<GraphicsConfiguration, Integer> config2devnum = new HashMap<GraphicsConfiguration, Integer>();
		for (int i = 0; i < devices.length; i++) {
			GraphicsDevice dev = devices[i];
			String id = dev.getIDstring();
			int type = dev.getType();
			if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {
				GraphicsConfiguration gconfig = dev.getDefaultConfiguration();
				Rectangle config_bounds = gconfig.getBounds();
				bounds2config.put(config_bounds, gconfig);
				config2devnum.put(gconfig, new Integer(i));
				boolean fullscreen = dev.isFullScreenSupported();
				boolean change = dev.isDisplayChangeSupported();
				int avail_accelmem = dev.getAvailableAcceleratedMemory() / 1000000;

				System.out.print("Graphics Device " + i + ", id =  " + id + " : ");
				if (type == GraphicsDevice.TYPE_RASTER_SCREEN) {
					System.out.print("RASTER\n");
				} else if (type == GraphicsDevice.TYPE_IMAGE_BUFFER) {
					System.out.print("BUFFER\n");
				} else if (type == GraphicsDevice.TYPE_PRINTER) {
					System.out.print("PRINTER\n");
				}

				System.out.println("   default config: " + gconfig);
				System.out.println("   bounds: " + config_bounds);
				System.out.println("   available VRAM: " + avail_accelmem + " MB");
				System.out.println("   full screen support: " + fullscreen);
				System.out.println("   display change support: " + change);
			}
		}
		System.out.println("#######  Allocating multiple windows for MultiWindowTierMap:");
		for (int x = 0; x < tile_columns; x++) {
			for (int y = 0; y < tile_rows; y++) {
				Rectangle win_bounds = new Rectangle((x * tile_width) + xbump, (y * tile_height) + ybump,
						tile_width, tile_height);
				Iterator iterbounds = bounds2config.keySet().iterator();
				while (iterbounds.hasNext()) {
					Rectangle screen_bounds = (Rectangle) iterbounds.next();
					if (win_bounds.intersects(screen_bounds)) {
						// found the right screen for this window

						GraphicsConfiguration gconfig = bounds2config.get(screen_bounds);
						Container win = new JWindow(gconfig);
						win.setLocation(win_bounds.x, win_bounds.y);
						win.setSize(win_bounds.width, win_bounds.height);
						System.out.println("   MultiWindow " + x + ", " + y +
								": bounds = " + win_bounds);
						System.out.println("         Graphics device #: " + config2devnum.get(gconfig) +
								", id = " + gconfig.getDevice().getIDstring());
						NeoMap newmap = new NeoMap(false, false);
						newmap.setRoot(this);
						Container cpane;

						JWindow jwin = (JWindow) win;
						cpane = jwin.getContentPane();

						cpane.setLayout(new BorderLayout());
						cpane.add("Center", newmap);


						newmap.getNeoCanvas().setDoubleBuffered(false);
						newmap.addMouseListener(child_mouse_handler);

						SmartRubberBand srb = new SmartRubberBand(newmap);
						newmap.setRubberBand(srb);
						srb.setColor(new Color(100, 100, 255));

						newmap.addRubberBandListener(this);
						child_maps[x][y] = newmap;
						win.setVisible(true);
						break;
					}
				}
			}
		}
		System.out.println("#######  Done allocating multiple windows");
		System.out.println("*******************************************");
	}

	public void transformMouseEvent(MouseEvent evt) {
		// if NeoMouseEvent on one of the child maps, coord position for event should be correct
		//   (since each child map has a proper view), though pixels position will be position in
		//   child canvas and therefore most likely wrong.
		// To fix pixels should be able to just do a reverse transform with view of parent (this) map?
		//    System.out.println("MultiWindowTierMap.transformMouseEvent() called");
		if (evt instanceof NeoMouseEvent) {
			NeoMouseEvent cevt = (NeoMouseEvent) evt;

			// make new event with this (parent) map as source
			// Current approach  is a little messy, since the pixel position is valid for the child window but not for
			//    the virtual parent window -- but need to keep it in child pixels because popups may use it
			//    for drawing/placement of popup window
			NeoMouseEvent pevt =
					new NeoMouseEvent(cevt, this, cevt.getCoordX(), cevt.getCoordY());
			// post new event to any listeners on this map

			// First tried just calling processMouseEvent(), but apparently that doesn't actually end up
			//    calling the mouse listeners if this component is not actually being rendered itself
			// Therefore setting up extra list to keep track of listeners and notify them here
			for (MouseListener listener : mlisteners) {
				if (listener == null) {
					return;
				}
				int id = pevt.getID();
				switch (id) {
					case MouseEvent.MOUSE_PRESSED:
						listener.mousePressed(pevt);
						break;
					case MouseEvent.MOUSE_RELEASED:
						listener.mouseReleased(pevt);
						break;
					case MouseEvent.MOUSE_CLICKED:
						listener.mouseClicked(pevt);
						break;
					case MouseEvent.MOUSE_EXITED:
						listener.mouseExited(pevt);
						break;
					case MouseEvent.MOUSE_ENTERED:
						listener.mouseEntered(pevt);
						break;
				}
			}
		}
	}

	@Override
	public void rubberBandChanged(NeoRubberBandEvent evt) {
		transformRubberBandEvent(evt);
	}

	/**
	 *   Handling of NeoRubberBandEvent propogation from child maps up to listeners on this parent map
	 *      is different from how MouseEvents are propogated, since rubber band listener is unlikely
	 *      to draw anything (drawing is handled internally by RubberBands), and since rubber band event
	 *      has no coord position, listener will often transform pixel to coords -- so pixels need
	 *      to be relative to virtual parent window
	 */
	private void transformRubberBandEvent(NeoRubberBandEvent evt) {
		Object orig_source = (Component) evt.getSource();
		if (orig_source instanceof NeoMap) {
			NeoMap orig_map = (NeoMap) orig_source;
			// to set pixel x and y for full map canvas, need to create a new mouse event that
			//     calculates full map pixel x and y
			// calculate pixel point for new RubberBandEvent
			Point orig_pixpoint = evt.getPoint();
			// transform pixels from child map to coords
			Point2D.Double cpoint = new Point2D.Double(0, 0);
			orig_map.getView().transformToCoords(orig_pixpoint, cpoint);
			// transform coords to pixels on full/parent map
			Point new_pixpoint = new Point();
			this.getView().transformToPixels(cpoint, new_pixpoint);
			int newx = new_pixpoint.x;
			int newy = new_pixpoint.y;

			// do same to calculate pixel box for new RubberBandEvent
			Rectangle orig_pixbox = evt.getPixelBox();
			Rectangle2D.Double cbox = new Rectangle2D.Double();
			orig_map.getView().transformToCoords(orig_pixbox, cbox);
			Rectangle new_pixbox = new Rectangle();
			this.getView().transformToPixels(cbox, new_pixbox);

			NeoRubberBandEvent new_evt =
					new VirtualRubberBandEvent(this.getNeoCanvas(), evt.getID(), evt.getWhen(), evt.getModifiers(),
					newx, newy, evt.getClickCount(), evt.isPopupTrigger(), new_pixbox);
			for (NeoRubberBandListener listener : rlisteners) {
				if (listener != null) {
					listener.rubberBandChanged(new_evt);
				}
			}

		}
	}

	@Override
	public void addMouseListener(MouseListener listener) {
		super.addMouseListener(listener);
		mlisteners.add(listener);
	}

	@Override
	public void removeMouseListener(MouseListener listener) {
		super.removeMouseListener(listener);
		mlisteners.remove(listener);
	}

	@Override
	public void addRubberBandListener(NeoRubberBandListener listener) {
		super.addRubberBandListener(listener);
		rlisteners.add(listener);
	}

	@Override
	public void removeRubberBandListener(NeoRubberBandListener listener) {
		super.removeRubberBandListener(listener);
		rlisteners.remove(listener);
	}
}

final class MultiMapMouseHandler implements MouseListener {

	MultiWindowTierMap main_map;

	public MultiMapMouseHandler(MultiWindowTierMap map) {
		main_map = map;
	}

	public void mouseEntered(MouseEvent evt) {
		main_map.transformMouseEvent(evt);
	}

	public void mouseExited(MouseEvent evt) {
		main_map.transformMouseEvent(evt);
	}

	public void mouseClicked(MouseEvent evt) {
		main_map.transformMouseEvent(evt);
	}

	public void mousePressed(MouseEvent evt) {
		main_map.transformMouseEvent(evt);
	}

	public void mouseReleased(MouseEvent evt) {
		main_map.transformMouseEvent(evt);
	}

}

package com.affymetrix.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

import javax.swing.ImageIcon;

import org.osgi.framework.BundleContext;

/**
 * utilities used by both the main, starting class, and the
 * bundles. Singleton pattern.
 *
 */
public class CommonUtils {
	private static final CommonUtils instance = new CommonUtils();
	private String app_dir = null;
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("common");

	private static final String APP_NAME            = BUNDLE.getString("appName");
	private static final String APP_NAME_SHORT      = BUNDLE.getString("appNameShort");
	private static final String APP_VERSION         = BUNDLE.getString("appVersion");
	private static final String BUILD_CUSTOMIZATION = BUNDLE.getString("buildCustomization");
	private static final String GOOGLE_ANALYTICS_ID = BUNDLE.getString("googleAnalyticsId");
	
	private CommonUtils() {
		super();
	}

	public final static CommonUtils getInstance() {
		return instance;
	}

	public String getAppName() {
		return APP_NAME;
	}

	public String getAppNameShort() {
		return APP_NAME_SHORT;
	}

	/**
	 * get the current version of IGB
	 * @return the IGB version
	 */
	public String getAppVersion() {
		return APP_VERSION;
	}
	
	/*
	 * get the google analytics id
	 * @return google analytics id 
	 */
	public String getGoogleAnalyticsId() {
		return GOOGLE_ANALYTICS_ID;
	}
	
	/**
	 * Get a large(133x133) application icon 
	 * @return 
	 */
	public ImageIcon getApplicationIcon(){
		return CommonUtils.getInstance().getIcon("images/igb.gif");
	}
	
	/**
	 * Get a small(64x64) application icon
	 * @return 
	 */
	public ImageIcon getApplicationSmallIcon(){
		return CommonUtils.getInstance().getIcon("images/igb_small.gif");
	}
		
	/**
	 * Returns the value of the argument indicated by label.
	 * If arguments are
	 *   "-flag_2 -foo bar", then get_arg("foo", args)
	 * returns "bar", get_arg("flag_2") returns a non-null string,
	 * and get_arg("flag_5") returns null.
	 */
	public String getArg(String label, String[] args) {
		String to_return = null;
		boolean got_it = false;
		if (label != null && args != null) {
			for (String item : args) {
				if (got_it) {
					to_return = item;
					break;
				}
				if (item.equals(label)) {
					got_it = true;
				}
			}
		}
		if (got_it && to_return == null) {
			to_return = "true";
		}
		return to_return;
	}

	public String[] getArgs(BundleContext bundleContext) {
		if(bundleContext.getProperty("args") == null)
			return null;
		return bundleContext.getProperty("args").split(", ");
	}

	public boolean isHelp(BundleContext bundleContext) {
        String[] args = getArgs(bundleContext);
 		return (getArg("-h", args) != null || getArg("-help", args) != null); // display all command options
	}

	public boolean isExit(BundleContext bundleContext) {
        String[] args = getArgs(bundleContext);
		return (getArg("-h", args) != null ||
				getArg("-help", args) != null ||
				getArg("-exit", args) != null ||
				getArg("-convert", args) != null ||
				getArg("-cbc", args) != null
		); // exit program
	}

	/**
	 * Returns the location of the application data directory.
	 * The String will always end with "/".
	 *
	 * @return the application directory
	 */
	public String getAppDataDirectory() {
		if (app_dir == null) {
			String home = System.getProperty("user.home");
			String app_data = home + "/Application Data";
			File app_data_dir = new File(app_data);
			if (app_data_dir.exists() && app_data_dir.isDirectory()) {
				app_dir = app_data + "/IGB/";
			} else {
				app_dir = home + "/.igb/";
			}
		}
		if (!app_dir.endsWith("/")) {
			app_dir = app_dir + "/";
		}
		return app_dir;
	}

	 public ImageIcon getIcon(String resource_name) {
	     return getIcon(CommonUtils.class, resource_name);
	 }
	  
	 /**
	   *  Loads an ImageIcon from the specified system resource.
	   *  The system resource should be in the classpath, for example,
	   *  it could be in the jlfgr-1_0.jar file.  If the resource is
	   *  absent or can't be found, this routine will not throw an exception,
	   *  but will return null.
	   *  For example: "toolbarButtonGraphics/general/About16.gif".
	   *  @return An ImageIcon or null if the one specified could not be found.
	   */
	 public ImageIcon getIcon(Class<?> clazz, String resource_name) {
		ImageIcon icon = null;
	    try {
	      // Note: MenuUtil.class.getResource(resource_name) does not work;
	      // ClassLoader.getSystemResource(resource_name) works locally, but not with WebStart;
	      //
	      // Both of these work locally and with WebStart:
	      //  MenuUtil.class.getClassLoader().getResource(resource_name)
	      //  Thread.currentThread().getContextClassLoader().getResource(resource_name)
	      java.net.URL url = clazz.getClassLoader().getResource(resource_name);
	      if (url != null) {
	        icon = new ImageIcon(url);
	      }
	    } catch (Exception e) {
	    	e.printStackTrace(System.out);
	      // It isn't a big deal if we can't find the icon, just return null
	    }
	    if (icon == null || icon.getImageLoadStatus() == MediaTracker.ABORTED ||
	        icon.getIconHeight() <= 0 || icon.getIconWidth() <= 0) {
	      icon = null;
	    }
	    
	    return icon; 
	  }
	  
	public ImageIcon getAlternateIcon(String resource_name) {
		try {
			java.net.URL resource_url = CommonUtils.class.getClassLoader().getResource(resource_name);
			if (resource_url != null) {
				BufferedImage resource_img = ImageIO.read(resource_url);
				Graphics2D resource_graphics = resource_img.createGraphics();
				
				int width = resource_img.getWidth();
				int height = resource_img.getHeight();
				int half_width = width/2;
				int half_height = height/2;
				
				resource_graphics.setColor(Color.BLACK);
				resource_graphics.setStroke(new BasicStroke(1.5f));
				resource_graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				resource_graphics.drawOval(half_width - 2, half_height - 2, half_width, half_height);
				resource_graphics.drawLine(half_width, half_height, 2 * half_width - 4, 2 * half_height - 4);
//				resource_graphics.drawLine(0, resource_img.getHeight(), resource_img.getWidth(), 0);
				return new ImageIcon(resource_img);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			// It isn't a big deal if we can't find the icon, just return null
		}
		return null;
	}
}

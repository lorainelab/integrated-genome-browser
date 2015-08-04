package com.lorainelab.protannot;

import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.slf4j.LoggerFactory;

/**
 * Class to handle Integration so it will behave more mac-like on OS X.
 * This is achieved using reflection so that the apple-specific classes will
 * not interfere on other platforms.
 *
 * @author sgblanch
 * @version $Id: MacIntegration.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
public class MacIntegration {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MacIntegration.class);

    private Class<?> applicationClass = null;
    private Object application = null;

    /**
     * Private constructor to enforce singleton pattern
     */
    public MacIntegration(ProtAnnotAction protAnnotAction) {
        try {
            applicationClass = Class.forName("com.apple.eawt.Application");
            Method getApplication = applicationClass.getDeclaredMethod("getApplication");
            application = getApplication.invoke(null);

            Method setEnabledPreferencesMenu = applicationClass.getDeclaredMethod("setEnabledPreferencesMenu", Boolean.TYPE);
            setEnabledPreferencesMenu.invoke(application, true);

            Method addApplicationListener = applicationClass.getDeclaredMethod(
                    "addApplicationListener",
                    Class.forName("com.apple.eawt.ApplicationListener"));

            Class<?> applicationAdapterClass = Class.forName("com.apple.eawt.ApplicationAdapter");
            Object proxy = ApplicationListenerProxy.newInstance(applicationAdapterClass.newInstance(), protAnnotAction);
            addApplicationListener.invoke(application, proxy);

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * Initialize the singleton copy of MacIntegration. This should only be
     * called once by the application, but it protects itself against multiple
     * invocations. Do not call this function on anything platform other
     * than Macintosh: Undefined things will happen.
     *
     * @return a singleton instance of MacIntegration
     */
//    public static synchronized MacIntegration getInstance() {
//        if (instance == null) {
//            instance = new MacIntegration();
//        }
//        return instance;
//    }

    /**
     * Wrapper around Apple's com.apple.eawt.setDockIconImage.
     *
     * @param image the Image to use as the Dock icon.
     */
    public void setDockIconImage(Image image) {
        try {
            Method setDockIconImage = applicationClass.getDeclaredMethod("setDockIconImage", Image.class);
            setDockIconImage.invoke(application, image);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}

class ApplicationListenerProxy implements InvocationHandler {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ApplicationListenerProxy.class);

    private final Object o;
    
    private final ProtAnnotAction protAnnotAction;

    public static Object newInstance(Object o, ProtAnnotAction pa) {
        return Proxy.newProxyInstance(
                o.getClass().getClassLoader(),
                o.getClass().getInterfaces(),
                new ApplicationListenerProxy(o, pa));
    }

    private ApplicationListenerProxy(Object o, ProtAnnotAction protAnnotAction) {
        this.o = o;
        this.protAnnotAction = protAnnotAction;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            switch (method.getName()) {
                case "handleAbout":
                    protAnnotAction.actionPerformed(null);
                    Method setHandled = Class.forName("com.apple.eawt.ApplicationEvent").getDeclaredMethod("setHandled", Boolean.TYPE);
                    setHandled.invoke(args[0], true);
                    break;
                case "handleQuit":
                    protAnnotAction.actionPerformed(null);
                    break;
                case "handlePreferences":
                    protAnnotAction.actionPerformed(null);
                    break;
                default:
                    result = method.invoke(o, args);
                    break;
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return result;
    }
}

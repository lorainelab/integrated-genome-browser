package com.affymetrix.genometry.servlets.das2manager;

import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class HibernateAppListener implements ServletContextListener	{

	/* Application Startup Event */
	public void	contextInitialized(ServletContextEvent ce) {
	  
	    // If web.xml indicates that the DAS2 server should load annotation info from
	    // a DB, then create a Hibernate Session Factory.
	    if (ce.getServletContext().getInitParameter("genometry_dbmode") != null && 
	        ce.getServletContext().getInitParameter("genometry_dbmode").equalsIgnoreCase("true")) {

	      try  {
	        Class.forName("com.affymetrix.genometry.servlets.das2manager.HibernateUtil").newInstance();
	        Logger.getLogger(this.getClass().getName()).info("Hibernate session factory created.");
	      }
	      catch (Exception e)  {
	        Logger.getLogger(this.getClass().getName()).severe("FAILED HibernateAppListener.contextInitialize()");
	      }
	    } else {
	        Logger.getLogger(this.getClass().getName()).info("Hibernate session factory NOT created because annotations will be loaded directly from file system.");
	    }
	}

	/* Application Shutdown	Event */
	public void	contextDestroyed(ServletContextEvent ce) {}
}
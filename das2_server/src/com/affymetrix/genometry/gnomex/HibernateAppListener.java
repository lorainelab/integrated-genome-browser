package com.affymetrix.genometry.gnomex;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.affymetrix.genometry.genopub.Constants;

public class HibernateAppListener implements ServletContextListener	{

	/* Application Startup Event */
	public void	contextInitialized(ServletContextEvent ce) {
	  
	    // If web.xml indicates that the DAS2 server should load annotation info from
	    // a DB, then create a Hibernate Session Factory.
	    if (ce.getServletContext().getInitParameter(Constants.GENOMETRY_MODE) != null && 
	        ce.getServletContext().getInitParameter(Constants.GENOMETRY_MODE).equalsIgnoreCase(Constants.GENOMETRY_MODE_GNOMEX)) {

	      try  {
	        Class.forName("com.affymetrix.genometry.gnomex.HibernateUtil").newInstance();
	        Logger.getLogger(this.getClass().getName()).info("GNomEx Hibernate session factory created.");
	      }
	      catch (Exception e)  {
	        Logger.getLogger(this.getClass().getName()).severe("FAILED GNomEx HibernateAppListener.contextInitialize()");
	      }
	    } else {
	      if (ce.getServletContext().getInitParameter(Constants.GENOMETRY_MODE).equalsIgnoreCase(Constants.GENOMETRY_MODE_CLASSIC)) {
          Logger.getLogger(this.getClass().getName()).info("Hibernate GNomEx session factory NOT created because data tracks will be loaded directly from file system.");
	      } else if (ce.getServletContext().getInitParameter(Constants.GENOMETRY_MODE).equalsIgnoreCase(Constants.GENOMETRY_MODE_GENOPUB)) {
          Logger.getLogger(this.getClass().getName()).info("Hibernate GNomEx session factory NOT created because data tracks will be loaded from GenoPub database");
	      }
	    }
	}

	/* Application Shutdown	Event */
	public void	contextDestroyed(ServletContextEvent ce) {}
}
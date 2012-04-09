package com.affymetrix.genometry.gnomex;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;



public class HibernateUtil { 

    private static final SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            sessionFactory = new Configuration().configure("hibernate.gnomex.cfg.xml").buildSessionFactory();
            System.out.println("Hibernate GNomEx session factory created.");
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.out.println("HibernateUtil GNomEx SessionFactory created failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}

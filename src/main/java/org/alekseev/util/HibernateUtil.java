package org.alekseev.util;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
public final class HibernateUtil {
    private static volatile SessionFactory sessionFactory;
    private HibernateUtil() {
    }
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                    sessionFactory = new Configuration()
                            .configure()
                            .buildSessionFactory();
                    Runtime.getRuntime().addShutdownHook(
                            new Thread(HibernateUtil::shutdown)
                    );
                }
            }
        }
        return sessionFactory;
    }
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
    public static void setSessionFactoryForTests(SessionFactory testSessionFactory) {
        shutdown();
        sessionFactory = testSessionFactory;
    }
    public static void resetForTests() {
        shutdown();
        sessionFactory = null;
    }
}
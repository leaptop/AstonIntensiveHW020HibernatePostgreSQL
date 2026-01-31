package org.alekseev;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class App {
    public static void main(String[] args) {

        SessionFactory sessionFactory = new Configuration()
                .configure() // по умолчанию ищет hibernate.cfg.xml в resources
                .buildSessionFactory();

        try (Session session = sessionFactory.openSession()) {
            Object result = session.createNativeQuery("select 1").getSingleResult();
            System.out.println("DB connection OK, select 1 = " + result);
        } finally {
            sessionFactory.close();
        }
    }
}

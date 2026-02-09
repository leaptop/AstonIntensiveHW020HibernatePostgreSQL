package org.alekseev.util;
import org.alekseev.entity.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.util.Properties;
public final class HibernateTestUtil {
    private HibernateTestUtil() {
    }
    public static SessionFactory buildSessionFactory(
            String jdbcUrl,
            String username,
            String password
    ) {
        Properties props = new Properties();
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.connection.url", jdbcUrl);
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");
        Configuration cfg = new Configuration();
        cfg.setProperties(props);
        cfg.addAnnotatedClass(User.class);
        return cfg.buildSessionFactory();
    }
}
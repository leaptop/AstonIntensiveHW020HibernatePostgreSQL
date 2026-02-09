package org.alekseev;
import org.alekseev.dao.UserDao;
import org.alekseev.dao.impl.UserDaoHibernate;
import org.alekseev.ui.UserConsoleUi;
import org.alekseev.util.HibernateUtil;
public class App {
    public static void main(String[] args) {
        UserDao userDao = new UserDaoHibernate();
        UserConsoleUi ui = new UserConsoleUi(userDao);
        ui.run();
        HibernateUtil.shutdown();
    }
}

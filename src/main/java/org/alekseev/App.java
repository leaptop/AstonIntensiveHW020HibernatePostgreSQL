package org.alekseev;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.impl.UserDaoHibernate;
import org.alekseev.ui.UserConsoleUi;
import org.alekseev.util.HibernateUtil;

/**
 * App — точка входа (main) консольного приложения.
 *
 * Без Spring мы "вручную" создаём зависимости:
 * - DAO (UserDaoHibernate)
 * - UI (UserConsoleUi)
 *
 * Это называется manual wiring (ручная сборка объектов).
 * В Spring это делал бы DI-контейнер (Dependency Injection),
 * но по заданию Spring запрещён, поэтому делаем руками.
 */
public class App {

    public static void main(String[] args) {

        // Создаём DAO — объект доступа к БД
        UserDao userDao = new UserDaoHibernate();

        // Создаём UI и передаём ему DAO (чтобы UI мог вызывать CRUD)
        UserConsoleUi ui = new UserConsoleUi(userDao);

        // Запускаем главный цикл меню
        ui.run();

        // Закрываем SessionFactory (освобождаем ресурсы Hibernate/пулы соединений)
        HibernateUtil.shutdown();
    }
}

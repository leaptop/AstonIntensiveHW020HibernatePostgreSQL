package org.alekseev;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.impl.UserDaoHibernate;
import org.alekseev.entity.User;
import org.alekseev.util.HibernateUtil;

public class App {
    public static void main(String[] args) {

        // Создаём DAO-объект. Без Spring это делается вручную.
        UserDao userDao = new UserDaoHibernate();

        // CREATE
        Long id = userDao.create(new User("Stepan", "stepan@example.com", 38));
        System.out.println("Created user id = " + id);

        // READ by id
        System.out.println("Find by id = " + userDao.findById(id));

        // UPDATE
        userDao.updateById(id, "Stepan Updated", "stepan_updated@example.com", 39);
        System.out.println("After update = " + userDao.findById(id));

        // READ all
        System.out.println("All users = " + userDao.findAll());

        // DELETE
        userDao.deleteById(id);
        System.out.println("After delete = " + userDao.findById(id));

        // Закрываем фабрику (освобождение ресурсов)
        HibernateUtil.shutdown();
    }
}



/*
 // try-with-resources: гарантирует, что session.close() вызовется автоматически в конце блока
        try (Session session = HibernateUtil
                .getSessionFactory()//Получаем фабрику (создастся при первом вызове)
                .openSession()) {//Открываем Session — это “контекст” общения с БД

            Что такое Session (и что значит “контекст общения с БД”)
Session в Hibernate — это объект, который:
даёт тебе API для операций с БД (persist, get, createQuery, remove…)
хранит persistence context (первичный кэш / карта “объект → строка в БД”)
отслеживает изменения объектов (dirty checking) и в нужный момент пишет их в БД
Термин “контекст общения с БД” = набор правил и состояния, в рамках которых Hibernate работает с объектами:
какие сущности уже загружены (в памяти)
какие из них изменились
какие SQL нужно выполнить при commit
обеспечивает, что внутри одной Session один и тот же объект из БД представлен одной Java-ссылкой (identity map)
Очень грубо: Session = “рабочая папка/рабочий стол” Hibernate на время операции.


Object result = session
        .createNativeQuery("select 1")//выполняем “сырой” SQL (native query), чтобы проверить соединение
        .getSingleResult();//Берём единственный результат (здесь это число 1)

// Пишем в консоль, чтобы убедиться, что база доступна и запросы выполняются
            System.out.println("DB connection OK, select 1 = " + result);
        }

                // Закрываем SessionFactory явно (в маленьком консольном приложении это нормально и наглядно)
                // Даже если не вызвать, shutdown hook всё равно попробует закрыть при завершении JVM,
                // но явное закрытие помогает “как книга” увидеть жизненный цикл ресурсов.
                HibernateUtil.shutdown();
 */
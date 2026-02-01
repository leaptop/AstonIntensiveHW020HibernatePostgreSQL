package org.alekseev;

import org.alekseev.util.HibernateUtil; // Наш util для получения SessionFactory (создаётся один раз)
import org.hibernate.Session; // Session — “сеанс” работы с БД (через него делаем запросы/CRUD)

public class App {
    public static void main(String[] args) {

        // try-with-resources: гарантирует, что session.close() вызовется автоматически в конце блока
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // HibernateUtil.getSessionFactory(): получаем фабрику (создастся при первом вызове)
            // openSession(): открываем Session — это “контекст” общения с БД

            // createNativeQuery("select 1"): выполняем “сырой” SQL (native query), чтобы проверить соединение
            // getSingleResult(): берём единственный результат (здесь это число 1)
            Object result = session.createNativeQuery("select 1").getSingleResult();

            // Пишем в консоль, чтобы убедиться, что база доступна и запросы выполняются
            System.out.println("DB connection OK, select 1 = " + result);
        }

        // Закрываем SessionFactory явно (в маленьком консольном приложении это нормально и наглядно)
        // Даже если не вызвать, shutdown hook всё равно попробует закрыть при завершении JVM,
        // но явное закрытие помогает “как книга” увидеть жизненный цикл ресурсов.
        HibernateUtil.shutdown();
    }
}

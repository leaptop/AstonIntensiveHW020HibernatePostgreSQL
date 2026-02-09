package org.alekseev.util;

import org.alekseev.entity.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

/**
 * HibernateTestUtil — тестовая утилита для сборки SessionFactory,
 * которая подключается к PostgreSQL из Testcontainers.
 *
 * Здесь происходит именно "настройка клиента Hibernate", а не работа с данными.
 * То есть этот класс не вставляет строки в БД и не делает commit транзакций.
 * Он только собирает SessionFactory, через которую DAO позже будет открывать Session.
 *
 * Почему это в src/test:
 * - Этот код нужен только тестам, потому что в проде мы читаем hibernate.cfg.xml.
 * - В тестах мы не хотим зависеть от localhost:5432 и вручную поднятого Postgres.
 *   Мы хотим зависеть только от контейнера, который поднимается внутри теста.
 *
 * Как это связано с "в памяти vs в БД":
 * - Здесь мы работаем только с объектами конфигурации в памяти (Properties/Configuration).
 * - Реальная работа с БД начнётся только когда DAO откроет Session,
 *   начнёт Transaction и сделает commit (commit обычно вызывает flush и отправку SQL).
 */
public final class HibernateTestUtil {

    private HibernateTestUtil() {
        // Запрещаем создание экземпляров.
        // Эффект: класс используется как набор статических методов.
    }

    /**
     * Собрать SessionFactory для тестов, подключившись к БД контейнера.
     *
     * Что делаем:
     * - Формируем набор свойств Hibernate вручную (без чтения hibernate.cfg.xml).
     * - Указываем JDBC URL/логин/пароль, которые дал Testcontainers.
     * - Регистрируем entity-класс User, чтобы Hibernate знал, какие таблицы/маппинг использовать.
     *
     * Зачем:
     * - В интеграционных тестах мы должны работать с Postgres из контейнера, а не с локальной БД.
     * - Контейнер может выдать другой порт и другие креды, поэтому конфиг должен строиться динамически.
     *
     * Эффект:
     * - Возвращается SessionFactory, которая будет открывать соединения именно к контейнеру.
     * - Если потом мы подложим эту фабрику в HibernateUtil, то UserDaoHibernate автоматически начнёт
     *   работать через тестовую БД, потому что он берёт фабрику через HibernateUtil.getSessionFactory().
     */
    public static SessionFactory buildSessionFactory(
            String jdbcUrl,
            String username,
            String password
    ) {
        Properties props = new Properties();

        // --- Настройки подключения к БД контейнера ---
        // Что делаем: указываем драйвер PostgreSQL.
        // Зачем: Hibernate должен знать, какой JDBC-драйвер использовать для соединения.
        // Эффект: при открытии Session Hibernate сможет создать JDBC connection к контейнеру.
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");

        // Что делаем: задаём URL, который выдал контейнер (он включает хост/порт/базу).
        // Зачем: это точный адрес контейнерного Postgres, не localhost из hibernate.cfg.xml.
        // Эффект: все соединения будут идти в контейнер, а не в твою “ручную” БД.
        props.put("hibernate.connection.url", jdbcUrl);

        // Что делаем: задаём логин/пароль, который выдал контейнер.
        // Зачем: контейнер может создавать пользователя и пароль, и тест должен использовать их.
        // Эффект: соединение к контейнеру будет авторизовано корректно.
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);

        // --- Настройки диалекта ---
        // Что делаем: указываем диалект PostgreSQL.
        // Зачем: Hibernate генерирует SQL с учётом особенностей конкретной СУБД.
        // Эффект: DDL/SQL будут корректны именно для Postgres.
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // --- Управление схемой БД в тестах ---
        // Что делаем: включаем create-drop.
        // Зачем: в тестовой БД мы хотим автоматически создать таблицы в начале
        // и удалить их в конце жизни SessionFactory.
        // Эффект: тесты не зависят от того, создавал ли кто-то таблицы руками.
        // Важно: это влияет на схему БД, но это всё равно делается только в контейнере теста.
        props.put("hibernate.hbm2ddl.auto", "create-drop");

        // --- Логи SQL (опционально) ---
        // Что делаем: включаем show_sql/format_sql.
        // Зачем: когда тесты падают, очень полезно видеть, какие SQL запросы реально выполнялись.
        // Эффект: SQL будет печататься в консоль тестов.
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.format_sql", "true");

        // Что делаем: собираем Configuration и регистрируем entity User.
        // Зачем: без addAnnotatedClass Hibernate не будет знать, какие таблицы/маппинги есть в приложении.
        // Эффект: Hibernate сможет создать таблицу users по аннотациям @Entity из класса User.
        Configuration cfg = new Configuration();
        cfg.setProperties(props);
        cfg.addAnnotatedClass(User.class);

        // Что делаем: строим SessionFactory.
        // Зачем: это “тяжёлый” объект, который хранит всю собранную конфигурацию и умеет открывать Session.
        // Эффект: дальше DAO сможет открывать Session и делать транзакции/commit уже против контейнерной БД.
        return cfg.buildSessionFactory();
    }
}
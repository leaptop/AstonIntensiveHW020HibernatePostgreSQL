package org.alekseev.dao.impl;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.exception.DaoException;
import org.alekseev.entity.User;
import org.alekseev.util.HibernateTestUtil;
import org.alekseev.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для UserDaoHibernate (DAO-слой + Hibernate + реальный Postgres).
 * <p>
 * Главная идея этого класса:
 * 1) Мы НЕ мокаем DAO и НЕ мокаем Hibernate.
 * Мы проверяем настоящую работу: DAO открывает Session, начинает транзакцию,
 * делает INSERT/SELECT/UPDATE/DELETE и фиксирует результат commit-ом.
 * <p>
 * 2) PostgreSQL поднимается автоматически в Docker через Testcontainers.
 * Это НЕ “встроенная БД в памяти”, а реальный Postgres, просто временный.
 * Поэтому мы ловим те же ошибки, что и на проде: уникальные ограничения,
 * поведение транзакций, генерацию id, работу типов и т.д.
 * <p>
 * 3) Почему здесь так много аннотаций:
 * - JUnit 5 управляет жизненным циклом тестового класса и методов (когда создать объект,
 * когда вызвать @BeforeAll/@BeforeEach/@Test/@AfterAll).
 * - Testcontainers подключается к JUnit 5 как расширение (extension) и делает так,
 * чтобы контейнер стартовал ДО тестов и останавливался ПОСЛЕ.
 * <p>
 * Что делает @Testcontainers:
 * - Это “переключатель”, который говорит JUnit 5:
 * “подключи расширение Testcontainers и обработай поля @Container”.
 * - Без него поле @Container не будет автоматически стартовать/останавливаться.
 * <p>
 * Что делает @Container:
 * - Это метка для поля-контейнера.
 * - Testcontainers увидит это поле, поднимет контейнер перед тестами
 * и выдаст нам параметры подключения: JDBC URL, username, password.
 * <p>
 * Почему контейнер static:
 * - static означает “одно поле на весь класс”, то есть контейнер стартует 1 раз на класс,
 * а не заново на каждый тестовый метод.
 * - Это сильно ускоряет тесты.
 * - Минус: данные могут “протекать” между тестами, поэтому мы чистим таблицы в @BeforeEach.
 * <p>
 * Где тут “в памяти vs в БД”:
 * - Когда мы делаем new User(...) — это объект только в памяти JVM.
 * - Когда DAO вызывает session.persist(...) — Hibernate помечает объект как managed внутри Session
 * (то есть начинает отслеживать его состояние в памяти).
 * - Когда вызывается commit транзакции — Hibernate делает flush (отправляет SQL в БД),
 * и изменения становятся реальными строками в PostgreSQL контейнера.
 * <p>
 * Почему это именно интеграционный тест, а не юнит:
 * - Юнит-тест изолирует класс от окружения (БД, сеть, файлы).
 * - Здесь окружение намеренно настоящее (контейнер + Hibernate),
 * потому что цель — проверить “склейку” слоёв и реальное поведение SQL/транзакций.
 */
@Testcontainers//Примерно здесь запускается Docker и Postgres (вообще jvm решает когда и как запускать Docker).
@TestInstance(TestInstance.Lifecycle.PER_CLASS)//Создаём один объект тестового класса на все тесты. Если указать
//PER_METHOD, то будет создаваться новый объект на каждый тестовый метод, помеченный аннотацией @Test.
class UserDaoHibernateIT {

    /**
     * Контейнер PostgreSQL для тестов.
     * <p>
     * Что делаем:
     * - Описываем “какой Postgres запустить” (образ, имя БД, логин/пароль).
     * <p>
     * Зачем:
     * - Testcontainers должен поднять реальный Postgres в Docker.
     * - Мы получим честное поведение PostgreSQL, а не упрощённую имитацию.
     * <p>
     * Эффект:
     * - Перед тестами появится контейнер с Postgres.
     * - Мы сможем подключаться к нему по JDBC и выполнять запросы через Hibernate.
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("user_service_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    /**
     * SessionFactory для тестов.
     * <p>
     * Что делаем:
     * - Храним “фабрику сессий”, собранную на параметрах контейнера.
     * <p>
     * Зачем:
     * - DAO внутри себя берёт SessionFactory через HibernateUtil.
     * - Мы должны подложить такую фабрику, которая подключается к контейнеру,
     * а не к localhost из hibernate.cfg.xml.
     * <p>
     * Эффект:
     * - Все вызовы DAO будут ходить именно в контейнерную БД.
     */
    private SessionFactory sessionFactory;

    /**
     * Реальный DAO, который мы тестируем.
     * <p>
     * Тут важно:
     * - Это НЕ мок.
     * - Он будет открывать настоящие Hibernate Session и делать настоящие транзакции.
     */
    private UserDao userDao;

    @BeforeAll
    void beforeAll() {
        /*
         * Что делаем:
         * 1) Берём JDBC URL/логин/пароль у контейнера.
         * 2) Собираем SessionFactory через HibernateTestUtil (в памяти JVM).
         * 3) Подкладываем её в HibernateUtil через тестовый хук,
         *    чтобы UserDaoHibernate начал использовать именно её.
         *
         * Зачем:
         * - UserDaoHibernate внутри кода вызывает HibernateUtil.getSessionFactory().
         * - Если мы не подменим фабрику, DAO полезет в продовый конфиг (localhost:5432),
         *   и тест превратится в “зависит от моей локальной БД”, что плохо.
         *
         * Эффект:
         * - DAO автоматически работает с контейнером.
         * - Мы получаем повторяемые тесты на любой машине.
         */
        sessionFactory = HibernateTestUtil.buildSessionFactory(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        HibernateUtil.setSessionFactoryForTests(sessionFactory);

        userDao = new UserDaoHibernate();
    }

    @AfterAll
    void afterAll() {
        /*
         * Что делаем:
         * - Аккуратно закрываем SessionFactory.
         *
         * Зачем:
         * - SessionFactory держит ресурсы: соединения, внутренние сервисы Hibernate.
         * - Если не закрывать, тестовый процесс может “держать” ресурсы дольше нужного.
         *
         * Эффект:
         * - Тесты завершаются “чисто”.
         * - Контейнеру проще корректно остановиться.
         */
        HibernateUtil.resetForTests();
    }

    @BeforeEach
    void beforeEach() {
        /*
         * Изоляция тестов — ключевой момент.
         *
         * Что делаем:
         * - Перед каждым тестом удаляем все строки из таблицы users через HQL:
         *   "delete from User".
         *
         * Зачем:
         * - Контейнер у нас один на класс (static), то есть данные могли остаться
         *   после предыдущего теста.
         * - Мы хотим, чтобы каждый тест стартовал с “пустой БД”.
         *
         * Эффект:
         * - Тесты не зависят от порядка запуска.
         * - Один тест не ломает другой “остатками данных”.
         *
         * Важно про “в памяти vs в БД”:
         * - delete from User — это команда, которая реально удаляет строки в БД,
         *   но только после commit транзакции.
         */
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.createMutationQuery("delete from User").executeUpdate();
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    @Test
    void create_persistsUser_andCanBeReadBack() {
        /*
         * Что тестируем:
         * - create(...) реально вставляет строку в Postgres (контейнер),
         *   и возвращённый id позволяет потом прочитать того же пользователя.
         *
         * Почему это важно:
         * - Это проверка всей цепочки: Hibernate mapping → INSERT → генерация id → SELECT.
         */
        User user = new User();
        user.setName("Stepan");
        user.setEmail("stepan@example.com");
        user.setAge(38);

        Long id = userDao.create(user);

        assertNotNull(id, "DAO должен вернуть id созданной записи (id генерируется БД).");
        assertNotNull(user.getId(),
                "После commit Hibernate должен записать сгенерированный id обратно в объект в памяти.");

        Optional<User> loaded = userDao.findById(id);

        assertTrue(loaded.isPresent(), "После insert запись должна существовать в БД.");
        assertEquals("Stepan", loaded.get().getName());
        assertEquals("stepan@example.com", loaded.get().getEmail());
        assertEquals(38, loaded.get().getAge());

        assertNotNull(loaded.get().getCreatedAt(),
                "createdAt должен быть проставлен при INSERT (Hibernate @CreationTimestamp).");
    }

    @Test
    void findById_whenMissing_returnsEmptyOptional() {
        /*
         * Что тестируем:
         * - findById(...) на несуществующем id возвращает Optional.empty().
         *
         * Зачем:
         * - Это контракт DAO: отсутствие записи — это нормальный результат,
         *   а не исключение.
         */
        Optional<User> loaded = userDao.findById(999_999L);
        assertTrue(loaded.isEmpty(), "Если записи нет, Optional должен быть пустым.");
    }

    @Test
    void findAll_returnsAllUsers() {
        /*
         * Что тестируем:
         * - findAll() возвращает все строки из таблицы users как список объектов.
         *
         * Зачем:
         * - Проверяем HQL 'from User' → правильный SQL → правильный mapping в List<User>.
         */
        User u1 = new User();
        u1.setName("A");
        u1.setEmail("a@example.com");
        u1.setAge(10);
        userDao.create(u1);

        User u2 = new User();
        u2.setName("B");
        u2.setEmail("b@example.com");
        u2.setAge(20);
        userDao.create(u2);

        List<User> all = userDao.findAll();

        assertEquals(2, all.size(), "Должно вернуться ровно 2 пользователя.");
        assertTrue(all.stream().anyMatch(u -> "a@example.com".equals(u.getEmail())));
        assertTrue(all.stream().anyMatch(u -> "b@example.com".equals(u.getEmail())));
    }

    @Test
    void updateById_updatesExistingUser() {
        /*
         * Что тестируем:
         * - updateById(...) меняет поля существующей записи.
         *
         * Важный смысл именно для Hibernate:
         * - Внутри DAO объект загружается через session.get(...) и становится managed.
         * - Затем мы меняем поля через сеттеры в памяти.
         * - Hibernate делает dirty checking (то есть сам видит изменения)
         *   и отправляет UPDATE в БД при flush/commit.
         */
        User u = new User();
        u.setName("Old");
        u.setEmail("old@example.com");
        u.setAge(1);

        Long id = userDao.create(u);

        userDao.updateById(id, "New", "new@example.com", 99);

        User reloaded = userDao.findById(id).orElseThrow();
        assertEquals("New", reloaded.getName());
        assertEquals("new@example.com", reloaded.getEmail());
        assertEquals(99, reloaded.getAge());
    }

    @Test
    void deleteById_removesRow() {
        /*
         * Что тестируем:
         * - deleteById(...) реально удаляет строку из таблицы.
         *
         * Важно:
         * - Внутри DAO session.remove(...) только “помечает на удаление” внутри Session.
         * - Реальный DELETE уйдёт в БД при flush/commit транзакции.
         */
        User u = new User();
        u.setName("ToDelete");
        u.setEmail("delete@example.com");
        u.setAge(5);

        Long id = userDao.create(u);

        userDao.deleteById(id);

        Optional<User> loaded = userDao.findById(id);
        assertTrue(loaded.isEmpty(), "После delete запись должна исчезнуть из БД.");
    }

    @Test
    void create_whenEmailDuplicated_throwsDaoException() {
        /*
         * Что тестируем:
         * - Уникальность email на уровне БД действительно работает,
         *   и DAO заворачивает DB-ошибку в DaoException.
         *
         * Почему это важно:
         * - В реальной жизни уникальные ограничения — это “последняя линия обороны”.
         * - Даже если сервис валидирует вход, гонки (race conditions) всё равно возможны,
         *   поэтому constraint в БД обязателен.
         */
        User u1 = new User();
        u1.setName("User1");
        u1.setEmail("dup@example.com");
        u1.setAge(1);
        userDao.create(u1);

        User u2 = new User();
        u2.setName("User2");
        u2.setEmail("dup@example.com"); // дубликат
        u2.setAge(2);

        DaoException ex = assertThrows(
                DaoException.class,
                () -> userDao.create(u2),
                "При дубликате email DAO должен выбросить DaoException."
        );

        assertTrue(
                ex.getMessage().toLowerCase().contains("constraint"),
                "Сообщение должно намекать на constraint violation, чтобы было понятно, что случилось."
        );
    }

    @Test
    void updateById_whenUserMissing_throwsDaoException() {
        /*
         * Что тестируем:
         * - updateById(...) на несуществующем id выбрасывает DaoException.
         *
         * Зачем:
         * - Это поведение у тебя выбрано осознанно:
         *   “для учебного CRUD лучше явно сообщать, что не найден”.
         */
        assertThrows(
                DaoException.class,
                () -> userDao.updateById(12345L, "X", "x@example.com", 1),
                "Если пользователя нет, DAO должен явно сообщить об этом через DaoException."
        );
    }

    @Test
    void deleteById_whenUserMissing_throwsDaoException() {
        /*
         * Что тестируем:
         * - deleteById(...) на несуществующем id выбрасывает DaoException.
         *
         * Зачем:
         * - Чтобы не было “тихого удаления” (no-op), когда пользователь думает,
         *   что что-то удалилось, хотя удалять было нечего.
         */
        assertThrows(
                DaoException.class,
                () -> userDao.deleteById(12345L),
                "Если пользователя нет, DAO должен явно сообщить об этом через DaoException."
        );
    }
}
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
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDaoHibernateIT {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("user_service_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");
    private SessionFactory sessionFactory;
    private UserDao userDao;
    @BeforeAll
    void beforeAll() {
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
        HibernateUtil.resetForTests();
    }
    @BeforeEach
    void beforeEach() {
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
        Optional<User> loaded = userDao.findById(999_999L);
        assertTrue(loaded.isEmpty(), "Если записи нет, Optional должен быть пустым.");
    }
    @Test
    void findAll_returnsAllUsers() {
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
        User u1 = new User();
        u1.setName("User1");
        u1.setEmail("dup@example.com");
        u1.setAge(1);
        userDao.create(u1);
        User u2 = new User();
        u2.setName("User2");
        u2.setEmail("dup@example.com");
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
        assertThrows(
                DaoException.class,
                () -> userDao.updateById(12345L, "X", "x@example.com", 1),
                "Если пользователя нет, DAO должен явно сообщить об этом через DaoException."
        );
    }
    @Test
    void deleteById_whenUserMissing_throwsDaoException() {
        assertThrows(
                DaoException.class,
                () -> userDao.deleteById(12345L),
                "Если пользователя нет, DAO должен явно сообщить об этом через DaoException."
        );
    }
}
package org.alekseev.dao.impl;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.exception.DaoException;
import org.alekseev.entity.User;
import org.alekseev.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * UserDaoHibernate — реализация DAO через Hibernate.
 *
 * Здесь происходит всё "не Java Core":
 * - открытие Session
 * - запуск транзакции
 * - ORM-операции (persist/get/remove/query)
 * - commit/rollback
 * - обработка Hibernate/SQL исключений
 */
public class UserDaoHibernate implements UserDao {

    /**
     * Logger (SLF4J) — это стандартный интерфейс логирования.
     * Он не "пишет в файл сам", он вызывает конкретную реализацию (у тебя logback).
     *
     * Почему логи важны:
     * - видеть, что реально происходит (ошибки, id, действия)
     * - отлаживать транзакции и запросы
     */
    private static final Logger log = LoggerFactory.getLogger(UserDaoHibernate.class);

    @Override
    public Long create(User user) {
        // Session — "рабочий контекст" Hibernate:
        // внутри Session Hibernate хранит кэш загруженных объектов и отслеживает изменения.
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Transaction — граница атомарности.
            // Всё внутри commit либо применится, либо при ошибке откатится rollback.
            Transaction tx = session.beginTransaction();

            try {
                // persist переводит объект в состояние "persistent" (управляемый Hibernate).
                // Hibernate поймёт, что объект нужно вставить в таблицу.
                session.persist(user);

                // commit:
                // - фиксирует транзакцию в БД
                // - обычно вызывает flush (выгрузку изменений в SQL) перед commit
                tx.commit();

                // После commit у объекта user уже должен быть id (особенно при IDENTITY).
                log.info("User created with id={}", user.getId());
                return user.getId();

            } catch (ConstraintViolationException e) {
                // ConstraintViolationException — типичный случай: уникальность email нарушена и т.п.
                // Это уже "SQL/DB уровень", но Hibernate оборачивает в понятное исключение.
                safeRollback(tx);

                // В message пишем понятнее, чем “SQL state 23505”.
                throw new DaoException("Cannot create user: constraint violation (maybe email already exists).", e);

            } catch (RuntimeException e) {
                // RuntimeException покрывает HibernateException тоже (он наследник RuntimeException).
                safeRollback(tx);
                throw new DaoException("Cannot create user due to unexpected DB error.", e);
            }
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        // Для чтения транзакция технически не всегда обязательна,
        // но хорошая практика — всё равно использовать транзакцию:
        // - единый стиль
        // - корректная работа с уровнями изоляции и возможными lazy-данными
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // session.get(...) делает SELECT по первичному ключу.
                // Если записи нет — вернёт null (поэтому мы заворачиваем в Optional).
                User user = session.get(User.class, id);

                tx.commit();
                return Optional.ofNullable(user);

            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot find user by id=" + id, e);
            }
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // HQL/JPQL (Hibernate Query Language) — это запрос не к таблице, а к сущности.
                // "from User" значит: "выбери все объекты User".
                // Hibernate сам преобразует это в SQL: select ... from users
                List<User> users = session
                        .createQuery("from User", User.class) // типизированный запрос (Hibernate 6)
                        .getResultList();

                tx.commit();
                return users;

            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot load users list.", e);
            }
        }
    }

    @Override
    public void updateById(Long id, String newName, String newEmail, Integer newAge) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // Загружаем пользователя в рамках этой Session.
                // Теперь этот объект становится "persistent" (управляемым).
                User user = session.get(User.class, id);

                if (user == null) {
                    // Решение на твоё усмотрение: либо молча ничего не делать, либо бросить исключение.
                    // Для учебного CRUD обычно лучше явно сообщать, что не найден.
                    throw new DaoException("Cannot update: user not found, id=" + id);
                }

                // Мы меняем поля обычными сеттерами.
                // Важный момент Hibernate: dirty checking.
                //
                // Dirty checking = Hibernate запоминает "снимок" объекта при загрузке,
                // и перед commit сравнивает: что изменилось.
                // Если изменилось — сам сделает UPDATE без явного session.update().
                user.setName(newName);
                user.setEmail(newEmail);
                user.setAge(newAge);

                // На этом этапе ты не видишь SQL, но перед commit Hibernate сделает flush:
                // он отправит UPDATE в БД.
                tx.commit();

                log.info("User updated, id={}", id);

            } catch (ConstraintViolationException e) {
                // Например, ты обновил email на такой, который уже существует у другого пользователя
                safeRollback(tx);
                throw new DaoException("Cannot update user: constraint violation (maybe email already exists).", e);

            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot update user id=" + id, e);
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                User user = session.get(User.class, id);

                if (user == null) {
                    // Можно сделать no-op, но для учебного CRUD обычно лучше явно сообщать:
                    throw new DaoException("Cannot delete: user not found, id=" + id);
                }

                // remove помечает сущность на удаление.
                // Реальный SQL DELETE обычно будет выполнен при flush/commit.
                session.remove(user);

                tx.commit();

                log.info("User deleted, id={}", id);

            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot delete user id=" + id, e);
            }
        }
    }

    /**
     * Вспомогательный метод: аккуратно откатываем транзакцию, если она ещё активна.
     * Это важно, потому что:
     * - если tx уже закоммичен/откачен, повторный rollback может дать новую ошибку
     * - при исключении мы хотим гарантированно попытаться вернуть БД в чистое состояние
     */
    private void safeRollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException rollbackError) {
            // rollback тоже может упасть — логируем, но не перебиваем основную причину
            log.error("Rollback failed", rollbackError);
        }
    }
}

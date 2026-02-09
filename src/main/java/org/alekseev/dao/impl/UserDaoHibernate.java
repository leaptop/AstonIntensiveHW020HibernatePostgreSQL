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
public class UserDaoHibernate implements UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDaoHibernate.class);
    @Override
    public Long create(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(user);
                tx.commit();
                log.info("User created with id={}", user.getId());
                return user.getId();
            } catch (ConstraintViolationException e) {
                safeRollback(tx);
                throw new DaoException("Cannot create user: constraint violation (maybe email already exists).", e);
            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot create user due to unexpected DB error.", e);
            }
        }
    }
    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
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
                List<User> users = session
                        .createQuery("from User", User.class)
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
                User user = session.get(User.class, id);
                if (user == null) {
                    throw new DaoException("Cannot update: user not found, id=" + id);
                }
                user.setName(newName);
                user.setEmail(newEmail);
                user.setAge(newAge);
                tx.commit();
                log.info("User updated, id={}", id);
            } catch (ConstraintViolationException e) {
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
                    throw new DaoException("Cannot delete: user not found, id=" + id);
                }
                session.remove(user);
                tx.commit();
                log.info("User deleted, id={}", id);
            } catch (RuntimeException e) {
                safeRollback(tx);
                throw new DaoException("Cannot delete user id=" + id, e);
            }
        }
    }
    private void safeRollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException rollbackError) {
            log.error("Rollback failed", rollbackError);
        }
    }
}

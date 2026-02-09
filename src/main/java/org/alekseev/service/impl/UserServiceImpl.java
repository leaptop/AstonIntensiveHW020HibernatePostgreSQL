package org.alekseev.service.impl;
import org.alekseev.dao.UserDao;
import org.alekseev.dao.exception.DaoException;
import org.alekseev.entity.User;
import org.alekseev.service.UserService;
import org.alekseev.service.exception.NotFoundException;
import org.alekseev.service.exception.ServiceException;
import org.alekseev.service.exception.ValidationException;
import java.util.List;
import java.util.Optional;
public class UserServiceImpl implements UserService {
    private final UserDao userDao;
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }
    @Override
    public Long create(String name, String email, Integer age) {
        validateName(name);
        validateEmail(email);
        validateAge(age);
        try {
            User user = new User(name.trim(), email.trim(), age);
            return userDao.create(user);
        } catch (DaoException e) {
            throw new ServiceException("Cannot create user due to data access error.", e);
        }
    }
    @Override
    public Optional<User> findById(Long id) {
        validateId(id);
        try {
            return userDao.findById(id);
        } catch (DaoException e) {
            throw new ServiceException("Cannot find user due to data access error.", e);
        }
    }
    @Override
    public List<User> findAll() {
        try {
            return userDao.findAll();
        } catch (DaoException e) {
            throw new ServiceException("Cannot load users due to data access error.", e);
        }
    }
    @Override
    public void updateById(Long id, String newName, String newEmail, Integer newAge) {
        validateId(id);
        validateName(newName);
        validateEmail(newEmail);
        validateAge(newAge);
        try {
            Optional<User> existing = userDao.findById(id);
            if (existing.isEmpty()) {
                throw new NotFoundException("User not found, id=" + id);
            }
            userDao.updateById(id, newName.trim(), newEmail.trim(), newAge);
        } catch (NotFoundException e) {
            throw e;
        } catch (DaoException e) {
            throw new ServiceException("Cannot update user due to data access error.", e);
        }
    }
    @Override
    public void deleteById(Long id) {
        validateId(id);
        try {
            Optional<User> existing = userDao.findById(id);
            if (existing.isEmpty()) {
                throw new NotFoundException("User not found, id=" + id);
            }
            userDao.deleteById(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (DaoException e) {
            throw new ServiceException("Cannot delete user due to data access error.", e);
        }
    }
    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("id must be a positive number.");
        }
    }
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name must not be blank.");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("name length must be <= 100.");
        }
    }
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email must not be blank.");
        }
        if (email.trim().length() > 150) {
            throw new ValidationException("email length must be <= 150.");
        }
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        int dot = trimmed.lastIndexOf('.');
        if (at <= 0 || dot <= at + 1 || dot == trimmed.length() - 1) {
            throw new ValidationException("email looks invalid (expected something like name@example.com).");
        }
    }
    private void validateAge(Integer age) {
        if (age == null || age < 0 || age > 150) {
            throw new ValidationException("age must be between 0 and 150.");
        }
    }
}
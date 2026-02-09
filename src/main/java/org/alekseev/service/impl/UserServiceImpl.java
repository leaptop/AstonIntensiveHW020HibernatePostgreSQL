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

/**
 * Мы реализуем сервис как тонкий слой над DAO с проверками и понятными ошибками.
 * Это нужно, чтобы в юнит-тестах мы проверяли логику сервиса без настоящей БД, используя Mockito.
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    /**
     * Мы передаём DAO через конструктор, а не создаём его внутри сервиса.
     * Это даёт эффект: в тестах мы можем подставить мок DAO и изолировать сервис от БД.
     */
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Long create(String name, String email, Integer age) {
        validateName(name);
        validateEmail(email);
        validateAge(age);

        try {
            /* Мы создаём объект User в памяти, то есть пока никакой записи в БД не появляется.
               Запись в БД произойдёт только внутри DAO, когда DAO выполнит транзакцию и commit. */
            User user = new User(name.trim(), email.trim(), age);

            /* Мы вызываем DAO, чтобы он сохранил пользователя и вернул id, выданный базой данных.
               Эффект снаружи: UI получает идентификатор созданной записи и может показать его пользователю. */
            return userDao.create(user);

        } catch (DaoException e) {
            /* Мы ловим DaoException, чтобы внешний код не зависел от DAO и Hibernate.
               Эффект снаружи: UI/тесты видят единый тип ServiceException и могут обработать его одинаково. */
            throw new ServiceException("Cannot create user due to data access error.", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        validateId(id);

        try {
            /* Мы делегируем чтение DAO, потому что сервис не должен открывать Session/Transaction.
               Эффект снаружи: сервис остаётся лёгким, а вся работа с БД остаётся в DAO. */
            return userDao.findById(id);

        } catch (DaoException e) {
            /* Мы заворачиваем техническую ошибку чтения в ServiceException.
               Эффект снаружи: UI получает понятный тип ошибки и не обязан знать детали ORM/SQL. */
            throw new ServiceException("Cannot find user due to data access error.", e);
        }
    }

    @Override
    public List<User> findAll() {
        try {
            /* Мы делегируем чтение списка пользователей DAO, не добавляя бизнес-правил.
               Эффект снаружи: UI получает список и может вывести его, а тесты проверяют результат без контейнера. */
            return userDao.findAll();

        } catch (DaoException e) {
            /* Мы переводим DaoException в ServiceException, чтобы не протекали детали нижнего слоя.
               Эффект снаружи: обработка ошибок в UI становится единообразной. */
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
            /* Мы проверяем существование пользователя перед обновлением, чтобы дать понятный результат.
               Эффект снаружи: если пользователя нет, UI получит NotFoundException вместо "тихого" поведения. */
            Optional<User> existing = userDao.findById(id);
            if (existing.isEmpty()) {
                throw new NotFoundException("User not found, id=" + id);
            }

            /* Мы делегируем обновление DAO, потому что именно DAO знает, как выполнить транзакцию и commit.
               Эффект снаружи: изменения будут зафиксированы в БД только после commit внутри DAO, а не в сервисе. */
            userDao.updateById(id, newName.trim(), newEmail.trim(), newAge);

        } catch (NotFoundException e) {
            /* Мы пробрасываем NotFoundException дальше без обёртки, потому что это ожидаемая бизнес-ситуация.
               Эффект снаружи: UI и тесты могут отдельно обработать "не найдено" как обычный сценарий. */
            throw e;

        } catch (DaoException e) {
            /* Мы оборачиваем техническую ошибку DAO, чтобы UI не зависел от конкретной ORM/драйвера.
               Эффект снаружи: единый тип ServiceException для всех технических проблем. */
            throw new ServiceException("Cannot update user due to data access error.", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        validateId(id);

        try {
            /* Мы проверяем существование пользователя перед удалением, чтобы исключить no-op.
                */
            Optional<User> existing = userDao.findById(id);
            if (existing.isEmpty()) {
                throw new NotFoundException("User not found, id=" + id);
            }

            /* Мы делегируем удаление DAO, потому что именно DAO выполнит транзакцию и commit.
               Эффект снаружи: запись исчезнет из БД только после commit внутри DAO, а не просто после вызова метода. */
            userDao.deleteById(id);

        } catch (NotFoundException e) {
            /* Мы пробрасываем NotFoundException дальше, потому что это не "поломка", а ожидаемый результат.
               Эффект снаружи: UI может вывести "не найдено", а тесты могут это явно проверять. */
            throw e;

        } catch (DaoException e) {
            /* Мы переводим DaoException в ServiceException, чтобы сервис оставался единой точкой ошибок для UI.
               Эффект снаружи: UI не знает, какая БД и какой ORM используется внутри. */
            throw new ServiceException("Cannot delete user due to data access error.", e);
        }
    }

    private void validateId(Long id) {
        /* Мы проверяем id до обращения к DAO, чтобы не делать лишний запрос в БД.
           Эффект снаружи: пользователь сразу получает понятную ошибку ввода, а не технический сбой где-то глубже. */
        if (id == null || id <= 0) {
            throw new ValidationException("id must be a positive number.");
        }
    }

    private void validateName(String name) {
        /* Мы проверяем имя в памяти, чтобы гарантировать минимальные правила качества данных.
           Эффект снаружи: меньше мусора в БД и меньше неожиданных ошибок на уровне NOT NULL/длины поля. */
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name must not be blank.");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("name length must be <= 100.");
        }
    }

    private void validateEmail(String email) {
        /* Мы проверяем email до обращения к БД, чтобы ловить очевидные ошибки ввода сразу.
           Эффект снаружи: UI получает понятное сообщение, а БД не тратит ресурсы на заведомо неверные данные. */
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("email must not be blank.");
        }
        if (email.trim().length() > 150) {
            throw new ValidationException("email length must be <= 150.");
        }

        /* Мы делаем простую проверку формата, потому что полная RFC-валидация сложная и не нужна для учебного проекта.
           Эффект снаружи: сервис отсекает явный мусор, но не пытается быть идеальным валидатором email. */
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        int dot = trimmed.lastIndexOf('.');
        if (at <= 0 || dot <= at + 1 || dot == trimmed.length() - 1) {
            throw new ValidationException("email looks invalid (expected something like name@example.com).");
        }
    }

    private void validateAge(Integer age) {
        /* Мы проверяем возраст в памяти, чтобы не допускать бессмысленные значения.
           Эффект снаружи: UI может сообщить пользователю о неверном вводе без обращения к базе данных. */
        if (age == null || age < 0 || age > 150) {
            throw new ValidationException("age must be between 0 and 150.");
        }
    }
}
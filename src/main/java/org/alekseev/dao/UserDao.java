package org.alekseev.dao;

import org.alekseev.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * UserDao — контракт (интерфейс) доступа к данным User.
 *
 * Почему интерфейс полезен:
 * - можно заменить реализацию (например, JDBC вместо Hibernate)
 * - легче тестировать (можно подменить мок-реализацией)
 *
 * CRUD:
 * C (Create)  -> create(...)
 * R (Read)    -> findById(...), findAll()
 * U (Update)  -> updateById(...)
 * D (Delete)  -> deleteById(...)
 */
public interface UserDao {

    /**
     * Создаёт пользователя в БД.
     * Возвращаем id созданной записи.
     */
    Long create(User user);

    /**
     * Ищем пользователя по id.
     * Optional: потому что пользователь может не существовать.
     */
    Optional<User> findById(Long id);

    /**
     * Получить всех пользователей.
     */
    List<User> findAll();

    /**
     * Обновить поля пользователя по id.
     *
     * Мы НЕ принимаем User целиком по двум причинам:
     * 1) User может быть "detached" (оторван от Session), и новичку легко запутаться.
     * 2) Этот метод демонстрирует важную вещь Hibernate: dirty checking (авто-обновление).
     */
    void updateById(Long id, String newName, String newEmail, Integer newAge);

    /**
     * Удалить пользователя по id.
     * Если пользователя нет — можно сделать no-op или бросить исключение (ниже покажу).
     */
    void deleteById(Long id);
}

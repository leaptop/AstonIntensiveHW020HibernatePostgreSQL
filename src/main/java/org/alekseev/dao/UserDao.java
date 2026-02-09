package org.alekseev.dao;
import org.alekseev.entity.User;
import java.util.List;
import java.util.Optional;
public interface UserDao {
    Long create(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
    void updateById(Long id, String newName, String newEmail, Integer newAge);
    void deleteById(Long id);
}

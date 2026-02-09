package org.alekseev.service;
import org.alekseev.entity.User;
import java.util.List;
import java.util.Optional;
public interface UserService {
    Long create(String name, String email, Integer age);
    Optional<User> findById(Long id);
    List<User> findAll();
    void updateById(Long id, String newName, String newEmail, Integer newAge);
    void deleteById(Long id);
}
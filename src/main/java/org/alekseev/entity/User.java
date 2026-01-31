package org.alekseev.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor

@Entity // Говорим Hibernate: этот класс = сущность, которая будет храниться в таблице
@Table(
        name = "users", // Имя таблицы в БД
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email") // email должен быть уникальным
        }
)
public class User {

    @Id // первичный ключ
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY для PostgreSQL: БД сама генерирует id (аналог SERIAL/IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    // nullable=false -> NOT NULL, length=100 -> VARCHAR(100)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "age", nullable = false)
    private Integer age;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    // updatable=false: Hibernate не будет менять это поле при update
    private OffsetDateTime createdAt;

    // Удобный конструктор для создания "нового пользователя" (id и createdAt будут выставлены автоматически)
    public User(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }
}

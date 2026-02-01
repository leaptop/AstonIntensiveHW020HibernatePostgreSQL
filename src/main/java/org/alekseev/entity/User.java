package org.alekseev.entity;

import jakarta.persistence.*; // JPA-аннотации (jakarta.persistence) для описания маппинга "класс -> таблица"
import lombok.Getter; // Lombok: генерирует get-методы во время компиляции
import lombok.NoArgsConstructor; // Lombok: генерирует пустой конструктор (без аргументов)
import lombok.Setter; // Lombok: генерирует set-методы
import lombok.ToString; // Lombok: генерирует toString()
import org.hibernate.annotations.CreationTimestamp; // Hibernate-аннотация: автоматически проставляет timestamp при INSERT

import java.time.OffsetDateTime; // Тип даты-времени с часовым сдвигом (UTC+02 и т.п.), удобно для created_at

@Getter // Lombok: создаст геттеры для всех полей (getId(), getName()...)
@Setter // Lombok: создаст сеттеры для всех полей (setName(...), setAge(...))
@ToString // Lombok: создаст toString(), чтобы удобно печатать объект в лог/консоль
@NoArgsConstructor // Lombok: создаст public User() (Hibernate часто требует конструктор без аргументов)
@Entity // Говорим Hibernate: этот класс является сущностью (будет храниться в БД)
@Table( // Настройки таблицы, соответствующей этой сущности
        name = "users", // Имя таблицы в БД. Если не указать, будет имя класса (User) или по стратегии naming
        uniqueConstraints = { // EXPLAIN FROM HERE
                @UniqueConstraint(
                        name = "uk_users_email", // Имя constraint (удобно видеть в ошибках БД/логах)
                        columnNames = "email" // Колонка, которая должна быть уникальной
                )
        }
)//TO HERE
public class User { // Java-класс, который маппится на таблицу users

    @Id // Это поле — первичный ключ (PRIMARY KEY)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY означает:
    // - id генерируется на стороне БД
    // - для PostgreSQL это обычно эквивалентно SERIAL/IDENTITY колонке
    private Long id; // Тип Long -> в БД будет BIGINT (обычно)

    @Column(
            name = "name", // Имя колонки в таблице
            nullable = false, // nullable=false -> NOT NULL в БД (значение обязательно)
            length = 100 // VARCHAR(100). Для String полезно задавать длину
    )
    private String name; // Имя пользователя

    @Column(
            name = "email", // Колонка email
            nullable = false, // NOT NULL
            length = 150 // VARCHAR(150)
    )
    private String email; // Email пользователя (уникальность обеспечивается unique constraint на таблице)

    @Column(
            name = "age", // Колонка age
            nullable = false // NOT NULL
    )
    private Integer age; // Возраст (Integer, а не int: теоретически может быть null, но мы запретили null в БД)

    @CreationTimestamp
    // Hibernate при INSERT автоматически поставит текущий timestamp
    // Это удобно: ты не думаешь про created_at в коде DAO, он заполнится сам
    @Column(
            name = "created_at", // Имя колонки created_at
            nullable = false, // NOT NULL (вставка без значения запрещена)
            updatable = false // Hibernate не будет обновлять это поле при UPDATE (created_at фиксируется навсегда)
    )
    private OffsetDateTime createdAt; // В PostgreSQL обычно хранится как TIMESTAMP WITH TIME ZONE (timestamptz)

    // Конструктор для создания "нового пользователя" из кода приложения:
    // - id мы не передаём, потому что БД сама его сгенерирует
    // - createdAt мы тоже не передаём, потому что его поставит Hibernate (@CreationTimestamp)
    public User(String name, String email, Integer age) {
        this.name = name; // присваиваем имя
        this.email = email; // присваиваем email
        this.age = age; // присваиваем возраст
    }
}

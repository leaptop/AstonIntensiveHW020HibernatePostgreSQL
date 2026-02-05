package org.alekseev.dao.exception;

/**
 * DaoException — наше "обёрточное" исключение для слоя DAO.
 *
 * Зачем оно нужно:
 * 1) Hibernate/SQL выбрасывают много разных исключений.
 * 2) В UI/Service мы не хотим знать детали конкретной библиотеки.
 * 3) Мы хотим иметь единый тип ошибки "ошибка доступа к данным".
 *
 * Это типичный паттерн: низкоуровневые исключения заворачиваются в свои,
 * чтобы остальной код не зависел от конкретной ORM / драйвера.
 */
public class DaoException extends RuntimeException {

    public DaoException(String message, Throwable cause) {//изменение из гит мобайл
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}

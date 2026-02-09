package org.alekseev.service.exception;

/**
 * Мы создаём отдельное исключение для ошибок валидации входных данных.
 * Это нужно, чтобы сервис мог сообщать UI/тестам: "данные плохие", не обращаясь к БД.
 */
public class ValidationException extends RuntimeException {

    /**
     * Мы принимаем человекочитаемое сообщение об ошибке валидации.
     * Это нужно, чтобы UI и тесты могли проверять понятный текст, а не технические коды БД.
     */
    public ValidationException(String message) {
        super(message);
    }
}
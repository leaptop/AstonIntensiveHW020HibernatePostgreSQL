package org.alekseev.service.exception;

/**
 * Мы создаём исключение "не найдено" для ситуаций, когда сущности с таким id нет.
 * Это нужно, чтобы отличать "нормальную бизнес-ситуацию" от "технической ошибки БД".
 */
public class NotFoundException extends RuntimeException {

    /**
     * Мы сохраняем понятное сообщение о том, какой объект не найден.
     * Это нужно, чтобы UI мог показать пользователю ясный результат, а тесты могли это проверить.
     */
    public NotFoundException(String message) {
        super(message);
    }
}
package org.alekseev.service.exception;

/**
 * Мы создаём "обёртку" для неожиданных технических ошибок на уровне сервиса.
 * Это нужно, чтобы UI/тесты не зависели от DaoException и деталей Hibernate/SQL.
 */
public class ServiceException extends RuntimeException {

    /**
     * Мы сохраняем исходную причину (cause), чтобы не терять реальную ошибку DAO.
     * Это даёт эффект: логировать и отлаживать проще, но внешнему коду достаточно ServiceException.
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
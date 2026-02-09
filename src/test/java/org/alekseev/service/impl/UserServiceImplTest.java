package org.alekseev.service.impl;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.exception.DaoException;
import org.alekseev.entity.User;
import org.alekseev.service.exception.NotFoundException;
import org.alekseev.service.exception.ServiceException;
import org.alekseev.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Этот тестовый класс — пример “чистого” unit-тестирования сервисного слоя без базы данных и без Hibernate.
 *
 * 1) Зачем вообще unit-тестировать сервис отдельно от DAO:
 *    - Сервис содержит правила: валидация входных данных, реакция “не найдено”, упаковка ошибок DAO в ServiceException.
 *    - Эти правила должны проверяться быстро и изолированно, чтобы тесты запускались сотни раз за минуту и не были хрупкими.
 *    - Если мы тестируем сервис через реальную БД, то любой сбой Docker/сети/состояния таблиц будет “ломать” тесты сервиса,
 *      и мы потеряем смысл unit-тестов как быстрых проверок логики.
 *
 * 2) Что означает “изолированно” в контексте этого класса:
 *    - Мы НЕ создаём SessionFactory, НЕ открываем Hibernate Session и НЕ поднимаем PostgreSQL.
 *    - Вместо реального DAO мы используем мок (mock) UserDao, то есть объект-подделку, созданную Mockito.
 *    - Мок не делает реальных SQL-операций; он либо возвращает заранее заданные значения, либо бросает заранее заданные исключения.
 *
 * 3) Как это работает под капотом (JUnit 5 + Mockito):
 *    - Аннотация @ExtendWith(MockitoExtension.class) подключает расширение JUnit 5.
 *    - Расширение запускается ПЕРЕД каждым тестом и выполняет “инициализацию Mockito”:
 *      оно сканирует поля с аннотациями @Mock и создаёт для них мок-объекты.
 *    - Благодаря этому моки создаются автоматически и получаются “свежими” для каждого тестового метода,
 *      что помогает изоляции тестов (состояние одного теста не влияет на другой).
 *
 * 4) Что такое мок и чем он отличается от настоящего объекта:
 *    - Мок — это динамический прокси/сгенерированный класс, который перехватывает вызовы методов.
 *    - Если мы не задали поведение через when(...).thenReturn(...), то Mockito вернёт “значение по умолчанию”
 *      (null/0/false/пустые коллекции, в зависимости от типа).
 *    - Мы можем не только “подкладывать ответы”, но и проверять, какие методы были вызваны и с какими аргументами,
 *      через verify(...). Это позволяет тестировать, что сервис взаимодействует с DAO корректно.
 *
 * 5) Почему эти тесты не проверяют “commit/rollback/constraint/SQL”:
 *    - Потому что это ответственность DAO + Postgres, то есть интеграционный уровень.
 *    - Проверка SQL и транзакций делается в *IT тестах* через Testcontainers, где Postgres настоящий.
 *    - Здесь мы проверяем только логику сервиса в памяти и корректность “контракта” сервиса с DAO.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;
    /*
       Здесь мы объявляем мок UserDao, то есть подделку DAO-слоя.

       Что делаем:
       - Просим Mockito создать объект, который “выглядит как UserDao”, но не имеет реальной реализации работы с БД.

       Зачем это нужно:
       - Чтобы тестировать сервис без побочных эффектов (без вставок в БД, без Docker, без Hibernate).

       Какой эффект:
       - Когда сервис вызовет userDao.create/findById/updateById/deleteById, вызов будет перехвачен Mockito.
       - Мы сможем задать ответ мока (thenReturn) или искусственно сымитировать ошибку (thenThrow),
         а также проверить, вызывался ли метод и с какими аргументами (verify).
    */

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        /*
           Что делаем:
           - Создаём новый экземпляр UserServiceImpl перед каждым тестом и передаём ему мок DAO.

           Зачем это нужно:
           - Мы хотим, чтобы каждый тест проверял сервис в “чистом” состоянии.
           - Если бы сервис был общим на все тесты и хранил бы внутреннее состояние, тесты могли бы влиять друг на друга.

           Какой эффект:
           - Каждый тест начинает с предсказуемой стартовой конфигурации: service использует userDao-мок.
           - Любое поведение DAO в тесте управляется исключительно тем, что мы настроили в конкретном тестовом методе.
        */
        service = new UserServiceImpl(userDao);
    }

    @Test
    void create_validInput_callsDaoAndReturnsId() {
        /*
           Что делаем:
           - Настраиваем мок так, чтобы он “как будто” успешно создал пользователя и вернул id=10.

           Зачем это нужно:
           - В реальной системе id генерируется базой данных при INSERT.
           - Но в unit-тесте у нас нет базы, поэтому мы имитируем результат DAO вручную.

           Какой эффект:
           - Service.create(...) получит от DAO значение 10 и вернёт его наружу.
           - Мы сможем проверить, что сервис корректно “протаскивает” результат и правильно формирует объект User для DAO.
        */
        when(userDao.create(any())).thenReturn(10L);

        Long id = service.create("  Stepan  ", "  stepan@example.com  ", 38);

        /*
           Что делаем:
           - Проверяем, что сервис вернул id, который пришёл от DAO.

           Зачем это нужно:
           - Это контракт: сервис не должен “терять” id и не должен подменять его чем-то другим.

           Какой эффект:
           - В UI можно показать пользователю “создан id=10”, и это будет соответствовать тому, что DAO вернул.
        */
        assertEquals(10L, id);

        /*
           Что делаем:
           - Используем ArgumentCaptor, чтобы “поймать” объект User, который сервис передал в DAO.create(...).

           Зачем это нужно:
           - Нам важно доказать, что сервис реально добавляет ценность: он делает trim() и передаёт нормализованные данные.
           - Без этой проверки сервис мог бы случайно отправлять в DAO строки с пробелами, и это ухудшит данные в БД.

           Какой эффект:
           - Мы проверяем, что сервис подготовил правильные данные ещё ДО базы данных.
           - Это важно, потому что unit-тесты должны ловить ошибки логики как можно раньше и как можно дешевле.
        */
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao, times(1)).create(captor.capture());

        User passedUser = captor.getValue();
        assertEquals("Stepan", passedUser.getName());
        assertEquals("stepan@example.com", passedUser.getEmail());
        assertEquals(38, passedUser.getAge());
    }

    @Test
    void create_invalidEmail_throwsValidationAndDoesNotCallDao() {
        /*
           Что делаем:
           - Вызываем сервис с email, который явно не проходит валидацию.

           Зачем это нужно:
           - Сервис существует, в том числе, чтобы “отсечь” плохие данные ещё до DAO.
           - Если данные плохие, мы должны остановиться на уровне сервиса, а не пытаться выполнять запись в БД.

           Какой эффект:
           - Сервис бросает ValidationException.
           - DAO вообще не вызывается, то есть в системе не происходит попытки записи (никаких INSERT даже “в теории”).
        */
        assertThrows(ValidationException.class,
                () -> service.create("Stepan", "not-an-email", 38));

        /*
           Что делаем:
           - Проверяем отсутствие любого взаимодействия с DAO.

           Зачем это нужно:
           - Это ключевое свойство корректного сервиса: он не производит побочные эффекты при невалидных данных.

           Какой эффект:
           - Тест доказывает, что при ошибке ввода мы не идём в БД и не тратим ресурсы.
           - В будущем, если кто-то случайно перенесёт валидацию в DAO или забудет проверку, тест сразу поймает регрессию.
        */
        verifyNoInteractions(userDao);
    }

    @Test
    void create_invalidName_throwsValidationAndDoesNotCallDao() {
        /*
           Что делаем:
           - Передаём пустое имя (после trim оно становится пустой строкой).

           Зачем это нужно:
           - Мы хотим проверить, что валидация по имени срабатывает до обращения к DAO.

           Какой эффект:
           - Сервис бросает ValidationException.
           - DAO не вызывается, то есть никакая запись в БД не инициируется даже логически.
        */
        assertThrows(ValidationException.class,
                () -> service.create("   ", "a@b.com", 20));

        verifyNoInteractions(userDao);
    }

    @Test
    void create_invalidAge_throwsValidationAndDoesNotCallDao() {
        /*
           Что делаем:
           - Передаём явно некорректный возраст.

           Зачем это нужно:
           - Валидация возраста — бизнес-правило, которое должно жить в сервисе.
           - Мы хотим, чтобы неверные значения не доходили до слоя данных.

           Какой эффект:
           - Бросается ValidationException.
           - DAO не вызывается, значит не будет попыток изменений в БД.
        */
        assertThrows(ValidationException.class,
                () -> service.create("Stepan", "a@b.com", -1));

        verifyNoInteractions(userDao);
    }

    @Test
    void create_daoThrowsDaoException_serviceThrowsServiceException() {
        /*
           Что делаем:
           - Настраиваем мок DAO так, чтобы он бросил DaoException при create(...).

           Зачем это нужно:
           - Это имитация реальной ситуации “на уровне данных”: например, БД недоступна,
             или нарушено ограничение уникальности, или произошла другая ошибка хранения.

           Какой эффект:
           - Сервис должен не “пропускать наружу” DaoException, а упаковать её в ServiceException.
           - Это делает внешний код (UI) независимым от деталей DAO и ORM, но при этом сохраняет причину (cause) для диагностики.
        */
        when(userDao.create(any())).thenThrow(new DaoException("DB error"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.create("Stepan", "stepan@example.com", 38));

        /*
           Что делаем:
           - Проверяем, что у ServiceException есть причина, и это именно DaoException.

           Зачем это нужно:
           - Если “cause” потерять, то отлаживать станет значительно сложнее: мы увидим только общий текст без корня проблемы.

           Какой эффект:
           - Сервис остаётся дружелюбным для UI (единый тип ServiceException),
             но для разработчика сохраняется полная информация об исходной ошибке.
        */
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof DaoException);
    }

    @Test
    void findById_invalidId_throwsValidationAndDoesNotCallDao() {
        /*
           Что делаем:
           - Пытаемся вызвать findById с некорректным id (0).

           Зачем это нужно:
           - id=0 или отрицательный id не имеет смысла как первичный ключ.
           - Сервис должен остановить этот сценарий раньше, чем будет сделан запрос в БД.

           Какой эффект:
           - Бросается ValidationException.
           - DAO не вызывается, то есть не тратятся ресурсы и не создаётся ложное ощущение “мы что-то искали”.
        */
        assertThrows(ValidationException.class, () -> service.findById(0L));
        verifyNoInteractions(userDao);
    }

    @Test
    void updateById_userNotFound_throwsNotFoundAndDoesNotCallDaoUpdate() {
        /*
           Что делаем:
           - Настраиваем мок DAO так, чтобы findById вернул Optional.empty(), то есть “пользователь не найден”.

           Зачем это нужно:
           - Мы проверяем бизнес-реакцию сервиса: отсутствие записи — это не техническая ошибка, а нормальный сценарий “не найдено”.

           Какой эффект:
           - Сервис должен бросить NotFoundException.
           - Важно также, что сервис НЕ должен пытаться обновлять то, чего нет,
             поэтому DAO.updateById(...) не должен вызываться.
        */
        when(userDao.findById(123L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateById(123L, "New", "new@example.com", 40));

        verify(userDao, times(1)).findById(123L);
        verify(userDao, never()).updateById(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void updateById_valid_callsDaoUpdateWithTrimmedValues() {
        /*
           Что делаем:
           - Имитируем, что пользователь существует, возвращая непустой Optional.

           Зачем это нужно:
           - По логике сервиса обновление допускается только для существующего пользователя.
           - Мы хотим перейти в “успешный путь” и проверить, что сервис вызывает DAO.updateById(...) правильно.

           Какой эффект:
           - Сервис сделает trim() на входных строках и вызовет DAO.updateById с “чистыми” значениями.
           - Мы не проверяем здесь SQL и commit, потому что это задача интеграционных тестов DAO.
        */
        when(userDao.findById(5L)).thenReturn(Optional.of(new User("x", "x@x.com", 1)));

        service.updateById(5L, "  Name  ", "  mail@example.com  ", 50);

        /*
           Что делаем:
           - Проверяем, что сервис действительно сначала проверил существование (findById),
             а потом выполнил обновление (updateById).

           Зачем это нужно:
           - Это фиксирует “протокол” работы сервиса с DAO.
           - Если кто-то позже уберёт проверку существования или поменяет порядок вызовов, тест поймает изменение поведения.

           Какой эффект:
           - Поведение сервиса становится стабильным и предсказуемым: “не найдено” всегда обрабатывается до попытки обновления.
        */
        verify(userDao, times(1)).findById(5L);
        verify(userDao, times(1)).updateById(5L, "Name", "mail@example.com", 50);
    }

    @Test
    void deleteById_userNotFound_throwsNotFoundAndDoesNotCallDaoDelete() {
        /*
           Что делаем:
           - Настраиваем мок так, чтобы пользователь не существовал.

           Зачем это нужно:
           - Мы хотим избежать поведения “no-op”.
             Термин “no-op” означает “no operation”, то есть “метод ничего не сделал и молча завершился”.

           Какой эффект:
           - Сервис бросает NotFoundException и ясно сообщает, что удалять нечего.
           - DAO.deleteById(...) не вызывается, потому что нельзя удалить то, чего нет.
        */
        when(userDao.findById(77L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteById(77L));

        verify(userDao, times(1)).findById(77L);
        verify(userDao, never()).deleteById(anyLong());
    }

    @Test
    void deleteById_valid_callsDaoDelete() {
        /*
           Что делаем:
           - Имитируем существующего пользователя, чтобы сервис перешёл в “успешную ветку” удаления.

           Зачем это нужно:
           - Мы хотим проверить, что сервис вызывает DAO.deleteById(...) только после подтверждения существования записи.

           Какой эффект:
           - Сервис вызовет findById(...) и затем deleteById(...).
           - Реальное удаление строки в БД в этом unit-тесте не происходит,
             потому что DAO — мок и не имеет настоящей реализации.
        */
        when(userDao.findById(77L)).thenReturn(Optional.of(new User("x", "x@x.com", 1)));

        service.deleteById(77L);

        verify(userDao, times(1)).findById(77L);
        verify(userDao, times(1)).deleteById(77L);
    }
}
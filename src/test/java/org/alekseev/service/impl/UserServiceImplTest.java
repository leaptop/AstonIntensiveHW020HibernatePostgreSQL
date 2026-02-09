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
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserDao userDao;
    private UserServiceImpl service;
    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userDao);
    }
    @Test
    void create_validInput_callsDaoAndReturnsId() {
        when(userDao.create(any())).thenReturn(10L);
        Long id = service.create("  Stepan  ", "  stepan@example.com  ", 38);
        assertEquals(10L, id);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao, times(1)).create(captor.capture());
        User passedUser = captor.getValue();
        assertEquals("Stepan", passedUser.getName());
        assertEquals("stepan@example.com", passedUser.getEmail());
        assertEquals(38, passedUser.getAge());
    }
    @Test
    void create_invalidEmail_throwsValidationAndDoesNotCallDao() {
        assertThrows(ValidationException.class,
                () -> service.create("Stepan", "not-an-email", 38));
        verifyNoInteractions(userDao);
    }
    @Test
    void create_invalidName_throwsValidationAndDoesNotCallDao() {
        assertThrows(ValidationException.class,
                () -> service.create("   ", "a@b.com", 20));
        verifyNoInteractions(userDao);
    }
    @Test
    void create_invalidAge_throwsValidationAndDoesNotCallDao() {
        assertThrows(ValidationException.class,
                () -> service.create("Stepan", "a@b.com", -1));
        verifyNoInteractions(userDao);
    }
    @Test
    void create_daoThrowsDaoException_serviceThrowsServiceException() {
        when(userDao.create(any())).thenThrow(new DaoException("DB error"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.create("Stepan", "stepan@example.com", 38));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof DaoException);
    }
    @Test
    void findById_invalidId_throwsValidationAndDoesNotCallDao() {
        assertThrows(ValidationException.class, () -> service.findById(0L));
        verifyNoInteractions(userDao);
    }
    @Test
    void updateById_userNotFound_throwsNotFoundAndDoesNotCallDaoUpdate() {
        when(userDao.findById(123L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.updateById(123L, "New", "new@example.com", 40));
        verify(userDao, times(1)).findById(123L);
        verify(userDao, never()).updateById(anyLong(), anyString(), anyString(), anyInt());
    }
    @Test
    void updateById_valid_callsDaoUpdateWithTrimmedValues() {
        when(userDao.findById(5L)).thenReturn(Optional.of(new User("x", "x@x.com", 1)));
        service.updateById(5L, "  Name  ", "  mail@example.com  ", 50);
        verify(userDao, times(1)).findById(5L);
        verify(userDao, times(1)).updateById(5L, "Name", "mail@example.com", 50);
    }
    @Test
    void deleteById_userNotFound_throwsNotFoundAndDoesNotCallDaoDelete() {
        when(userDao.findById(77L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteById(77L));
        verify(userDao, times(1)).findById(77L);
        verify(userDao, never()).deleteById(anyLong());
    }
    @Test
    void deleteById_valid_callsDaoDelete() {
        when(userDao.findById(77L)).thenReturn(Optional.of(new User("x", "x@x.com", 1)));
        service.deleteById(77L);
        verify(userDao, times(1)).findById(77L);
        verify(userDao, times(1)).deleteById(77L);
    }
}
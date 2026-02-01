package org.alekseev.ui;

import org.alekseev.dao.UserDao;
import org.alekseev.dao.exception.DaoException;
import org.alekseev.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class UserConsoleUi {

    private final UserDao userDao;
    private final Scanner scanner;

    public UserConsoleUi(UserDao userDao) {
        this.userDao = userDao;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Главный цикл UI:
     * - печатает меню
     * - читает команду
     * - выполняет действие
     * - повторяет, пока пользователь не выберет Exit
     */
    public void run() {
        boolean running = true;

        while (running) {
            printMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> createUser();
                case "2" -> findUserById();
                case "3" -> listAllUsers();
                case "4" -> updateUser();
                case "5" -> deleteUser();
                case "0" -> {
                    System.out.println("Bye!");
                    running = false;
                }
                default -> System.out.println("Unknown command. Please choose 0-5.");
            }

            System.out.println();         }

        // Scanner можно не закрывать (закрытие закроет System.in), но это консольное приложение — норм.
        // scanner.close();
    }

    private void printMenu() {
        System.out.println("=== USER SERVICE MENU ===");
        System.out.println("1. Create user");
        System.out.println("2. Find user by id");
        System.out.println("3. List all users");
        System.out.println("4. Update user");
        System.out.println("5. Delete user");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    /**
     * CREATE (Create user)
     */
    private void createUser() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            Integer age = readInt("Age: ");

            // Создаём объект User в памяти (это ещё НЕ запись в БД).
            // Запись в БД делает DAO (через Hibernate persist/transaction).
            User user = new User(name, email, age);

            Long id = userDao.create(user);

            System.out.println("User created. id=" + id);

        } catch (DaoException e) {
            // DaoException — наше “единое” исключение для ошибок доступа к данным.
            // Внутри может быть нарушение unique email, проблемы соединения и т.д.
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * READ (Find user by id)
     */
    private void findUserById() {
        try {
            Long id = readLong("Enter id: ");

            Optional<User> userOpt = userDao.findById(id);

            if (userOpt.isPresent()) {
                // Здесь мы печатаем объект.
                // Благодаря Lombok @ToString() обычно вывод будет читабельный.
                System.out.println("Found: " + userOpt.get());
            } else {
                System.out.println("User not found.");
            }

        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * READ (List all)
     */
    private void listAllUsers() {
        try {
            List<User> users = userDao.findAll();

            if (users.isEmpty()) {
                System.out.println("No users in database.");
                return;
            }

            // Печатаем каждого пользователя отдельной строкой.
            // В реальном проекте часто делают красивый форматированный вывод.
            users.forEach(u -> System.out.println(u));

        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * UPDATE
     */
    private void updateUser() {
        try {
            Long id = readLong("Enter id to update: ");

            // Опционально: сначала показать текущего пользователя
            Optional<User> existing = userDao.findById(id);
            if (existing.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            System.out.println("Current: " + existing.get());

            System.out.print("New name: ");
            String newName = scanner.nextLine().trim();

            System.out.print("New email: ");
            String newEmail = scanner.nextLine().trim();

            Integer newAge = readInt("New age: ");

            // Важно: мы не делаем SQL напрямую.
            // DAO сам:
            // - откроет Session
            // - начнёт Transaction
            // - загрузит User
            // - применит изменения (dirty checking)
            // - commit/rollback
            userDao.updateById(id, newName, newEmail, newAge);

            System.out.println("Updated.");

        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * DELETE
     */
    private void deleteUser() {
        try {
            Long id = readLong("Enter id to delete: ");

            // DAO удалит запись (или бросит DaoException, если id не найден — как у тебя сейчас реализовано)
            userDao.deleteById(id);

            System.out.println("Deleted.");

        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    /**
     * Ниже вспомогательные методы ввода.
     *
     * Почему так:
     * - Scanner.nextLine() безопаснее использовать везде одинаково.
     * - Если смешивать nextInt() и nextLine(), часто ловят “пустой ввод” из-за перевода строки.
     */

    private Long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (long).");
            }
        }
    }

    private Integer readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (int).");
            }
        }
    }
}

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
    private void createUser() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            Integer age = readInt("Age: ");
            User user = new User(name, email, age);
            Long id = userDao.create(user);
            System.out.println("User created. id=" + id);
        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    private void findUserById() {
        try {
            Long id = readLong("Enter id: ");
            Optional<User> userOpt = userDao.findById(id);
            if (userOpt.isPresent()) {
                System.out.println("Found: " + userOpt.get());
            } else {
                System.out.println("User not found.");
            }
        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    private void listAllUsers() {
        try {
            List<User> users = userDao.findAll();
            if (users.isEmpty()) {
                System.out.println("No users in database.");
                return;
            }
            users.forEach(u -> System.out.println(u));
        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    private void updateUser() {
        try {
            Long id = readLong("Enter id to update: ");
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
            userDao.updateById(id, newName, newEmail, newAge);
            System.out.println("Updated.");
        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
    private void deleteUser() {
        try {
            Long id = readLong("Enter id to delete: ");
            userDao.deleteById(id);
            System.out.println("Deleted.");
        } catch (DaoException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
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

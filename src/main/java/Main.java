import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        while (true) {

            System.out.print("$ ");

            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("exit") || input.equals("exit 0")) {
                break;
            }

            String[] parts = input.split(" ");

            String command = parts[0];

            // echo builtin
            if (command.equals("echo")) {

                String message = input.substring(5);

                System.out.println(message);

                continue;
            }

            // pwd builtin
            if (command.equals("pwd")) {

                System.out.println(currentDirectory);

                continue;
            }

            // cd builtin
            if (command.equals("cd")) {

                if (parts.length < 2) {
                    continue;
                }

                String dir = parts[1];

                Path newPath;

                // Handle cd ~
                if (dir.equals("~")) {

                    String home = System.getenv("HOME");

                    newPath = Paths.get(home);

                }

                // Absolute path
                else if (Paths.get(dir).isAbsolute()) {

                    newPath = Paths.get(dir);

                }

                // Relative path
                else {

                    newPath = currentDirectory.resolve(dir);
                }

                newPath = newPath.normalize();

                if (Files.exists(newPath)
                        && Files.isDirectory(newPath)) {

                    currentDirectory = newPath;

                } else {

                    System.out.println(
                            "cd: " + dir + ": No such file or directory");
                }

                continue;
            }

            // type builtin
            if (command.equals("type")) {

                if (parts.length < 2) {
                    continue;
                }

                String cmdToCheck = parts[1];

                if (cmdToCheck.equals("echo")
                        || cmdToCheck.equals("exit")
                        || cmdToCheck.equals("type")
                        || cmdToCheck.equals("pwd")
                        || cmdToCheck.equals("cd")) {

                    System.out.println(
                            cmdToCheck + " is a shell builtin");

                    continue;
                }

                String pathEnv = System.getenv("PATH");

                boolean found = false;

                if (pathEnv != null) {

                    String[] directories =
                            pathEnv.split(File.pathSeparator);

                    for (String dir : directories) {

                        Path fullPath =
                                Paths.get(dir, cmdToCheck);

                        if (Files.exists(fullPath)
                                && Files.isExecutable(fullPath)) {

                            System.out.println(
                                    cmdToCheck
                                            + " is "
                                            + fullPath.toString());

                            found = true;

                            break;
                        }
                    }
                }

                if (!found) {

                    System.out.println(
                            cmdToCheck + ": not found");
                }

                continue;
            }

            // External commands

            String pathEnv = System.getenv("PATH");

            boolean executed = false;

            if (pathEnv != null) {

                String[] directories =
                        pathEnv.split(File.pathSeparator);

                for (String dir : directories) {

                    Path fullPath =
                            Paths.get(dir, command);

                    if (Files.exists(fullPath)
                            && Files.isExecutable(fullPath)) {

                        List<String> commandWithArgs =
                                new ArrayList<>();

                        commandWithArgs.add(command);

                        commandWithArgs.addAll(
                                Arrays.asList(parts)
                                        .subList(1, parts.length));

                        ProcessBuilder pb =
                                new ProcessBuilder(commandWithArgs);

                        pb.directory(currentDirectory.toFile());

                        pb.inheritIO();

                        Process process = pb.start();

                        process.waitFor();

                        executed = true;

                        break;
                    }
                }
            }

            if (!executed) {

                System.out.println(
                        input + ": command not found");
            }
        }

        sc.close();
    }
}
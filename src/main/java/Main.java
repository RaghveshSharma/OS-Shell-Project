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

        // Current working directory
        Path currentDirectory = Paths.get("").toAbsolutePath();

        while (true) {

            System.out.print("$ ");

            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("exit 0") || input.equals("exit")) {
                break;
            }

            // Split input into command + arguments
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

                System.out.println(currentDirectory.toString());

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
                        || cmdToCheck.equals("pwd")) {

                    System.out.println(cmdToCheck + " is a shell builtin");

                    continue;
                }

                String pathEnv = System.getenv("PATH");

                boolean found = false;

                if (pathEnv != null) {

                    String[] directories = pathEnv.split(File.pathSeparator);

                    for (String dir : directories) {

                        Path fullPath = Paths.get(dir, cmdToCheck);

                        if (Files.exists(fullPath)
                                && Files.isExecutable(fullPath)) {

                            System.out.println(
                                    cmdToCheck + " is " + fullPath.toString());

                            found = true;

                            break;
                        }
                    }
                }

                if (!found) {

                    System.out.println(cmdToCheck + ": not found");
                }

                continue;
            }

            // External commands

            String pathEnv = System.getenv("PATH");

            boolean executed = false;

            if (pathEnv != null) {

                String[] directories = pathEnv.split(File.pathSeparator);

                for (String dir : directories) {

                    Path fullPath = Paths.get(dir, command);

                    if (Files.exists(fullPath)
                            && Files.isExecutable(fullPath)) {

                        List<String> commandWithArgs = new ArrayList<>();

                        // argv[0] should be command name
                        commandWithArgs.add(command);

                        // Remaining arguments
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

                System.out.println(input + ": command not found");
            }
        }

        sc.close();
    }
}
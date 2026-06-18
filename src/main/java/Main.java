import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static List<String> parseCommand(String input) {

        List<String> parts = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < input.length(); i++) {

            char ch = input.charAt(i);

            // Inside double quotes
            if (inDoubleQuotes && ch == '\\') {

                if (i + 1 < input.length()) {

                    char next = input.charAt(i + 1);

                    // Only these are escaped in this stage
                    if (next == '"' || next == '\\') {

                        current.append(next);

                        i++;

                        continue;
                    }

                    // Backslash is literal for all others
                    current.append('\\');

                    continue;
                }

                current.append('\\');

                continue;
            }

            // Outside quotes: backslash escapes next char
            if (ch == '\\'
                    && !inSingleQuotes
                    && !inDoubleQuotes) {

                if (i + 1 < input.length()) {

                    current.append(input.charAt(i + 1));

                    i++;
                }

                continue;
            }

            // Single quotes
            if (ch == '\''
                    && !inDoubleQuotes) {

                inSingleQuotes = !inSingleQuotes;

                continue;
            }

            // Double quotes
            if (ch == '"'
                    && !inSingleQuotes) {

                inDoubleQuotes = !inDoubleQuotes;

                continue;
            }

            // Space outside quotes
            if (Character.isWhitespace(ch)
                    && !inSingleQuotes
                    && !inDoubleQuotes) {

                if (current.length() > 0) {

                    parts.add(current.toString());

                    current.setLength(0);
                }

            } else {

                current.append(ch);
            }
        }

        if (current.length() > 0) {

            parts.add(current.toString());
        }

        return parts;
    }

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        Path currentDirectory =
                Paths.get("").toAbsolutePath().normalize();
        
        int nextJobNumber = 1;

        long backgroundPid = -1;

String backgroundCommand = "";

boolean hasBackgroundJob = false;

        while (true) {

            System.out.print("$ ");

            String input = sc.nextLine();

            if (input.trim().isEmpty()) {

                continue;
            }

            String originalInput = input;

            List<String> parts = parseCommand(input);

boolean runInBackground = false;

if (!parts.isEmpty()
        && parts.get(parts.size() - 1).equals("&")) {

    runInBackground = true;

    parts.remove(parts.size() - 1);
}

if (parts.isEmpty()) {

    continue;
}

String command = parts.get(0);

            // exit builtin

            if (command.equals("exit")
                    || input.trim().equals("exit 0")) {

                break;
            }

            // echo builtin

            if (command.equals("echo")) {

                for (int i = 1; i < parts.size(); i++) {

                    if (i > 1) {

                        System.out.print(" ");
                    }

                    System.out.print(parts.get(i));
                }

                System.out.println();

                continue;
            }

            // pwd builtin

            if (command.equals("pwd")) {

                System.out.println(currentDirectory);

                continue;
            }

            // cd builtin

            if (command.equals("cd")) {

                if (parts.size() < 2) {

                    continue;
                }

                String dir = parts.get(1);

                Path newPath;

                // cd ~

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
                            "cd: "
                                    + dir
                                    + ": No such file or directory");
                }

                continue;
            }

            // jobs builtin

           if (command.equals("jobs")) {

    if (hasBackgroundJob) {

        System.out.printf(
                "[1]+  %-24s%s%n",
                "Running",
                backgroundCommand);
    }

    continue;
}

            // type builtin

            if (command.equals("type")) {

                if (parts.size() < 2) {

                    continue;
                }

                String cmdToCheck = parts.get(1);

                if (cmdToCheck.equals("echo")
                        || cmdToCheck.equals("exit")
                        || cmdToCheck.equals("type")
                        || cmdToCheck.equals("pwd")
                        || cmdToCheck.equals("cd")
                        || cmdToCheck.equals("jobs")) {

                    System.out.println(
                            cmdToCheck
                                    + " is a shell builtin");

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
                                            + fullPath);

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

                        for (int i = 1; i < parts.size(); i++) {

                            commandWithArgs.add(parts.get(i));
                        }

                        ProcessBuilder pb =
        new ProcessBuilder(commandWithArgs);

pb.directory(currentDirectory.toFile());



    pb.inheritIO();


Process process = pb.start();

if (runInBackground) {

    backgroundPid = process.pid();

    backgroundCommand = originalInput;

    hasBackgroundJob = true;

    System.out.println(
            "[" + nextJobNumber + "] "
                    + backgroundPid);

    nextJobNumber++;

}
else {

    process.waitFor();
}

executed = true;

break;
                    }
                }
            }

            if (!executed) {

                System.out.println(
                        command + ": command not found");
            }
        }

        sc.close();
    }
}
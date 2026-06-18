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

        while (true) {
            System.out.print("$ ");
            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("exit 0") || input.equals("exit")) {
                break;
            }

            // Split the input by spaces to separate the command from its arguments
            String[] parts = input.split(" ");
            String command = parts[0]; // Fixed: extract the first element of the array

            // Handle the echo builtin
            if (command.equals("echo")) {
                String message = input.substring(5);
                System.out.println(message);
                continue;
            }

            // Handle the type builtin
            if (command.equals("type")) {
                if (parts.length < 2) {
                    continue;
                }
                String cmdToCheck = parts[1];
                
                if (cmdToCheck.equals("echo") || cmdToCheck.equals("exit") || cmdToCheck.equals("type")) {
                    System.out.println(cmdToCheck + " is a shell builtin");
                    continue;
                }

                String pathEnv = System.getenv("PATH");
                boolean found = false;

                if (pathEnv != null) {
                    String[] directories = pathEnv.split(File.pathSeparator);
                    for (String dir : directories) {
                        Path fullPath = Paths.get(dir, cmdToCheck);
                        if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {
                            System.out.println(cmdToCheck + " is " + fullPath.toString());
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

            // Handle external commands by searching the PATH
            String pathEnv = System.getenv("PATH");
            boolean executed = false;

            if (pathEnv != null) {
                String[] directories = pathEnv.split(File.pathSeparator);
                for (String dir : directories) {
                    Path fullPath = Paths.get(dir, command);
                    
                    if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {
                        // Prepare the execution arguments list
                        List<String> commandWithArgs = new ArrayList<>();
                        commandWithArgs.add(fullPath.toString()); // Path to executable
                        commandWithArgs.addAll(Arrays.asList(parts).subList(1, parts.length)); // Remaining arguments

                        // Start the process and inherit its I/O streams so output displays automatically
                        ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
                        pb.inheritIO();
                        Process process = pb.start();
                        process.waitFor(); // Wait for the external process to finish running
                        
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
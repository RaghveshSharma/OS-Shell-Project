import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = sc.nextLine().trim();

            if (input.equals("exit 0") || input.equals("exit")) {
                break;
            }

            if (input.startsWith("echo ")) {
                String message = input.substring(5);
                System.out.println(message);
                continue;
            }

            if (input.startsWith("type ")) {
                String cmdToCheck = input.substring(5).trim();
                
                // 1. Check builtins first
                if (cmdToCheck.equals("echo") || cmdToCheck.equals("exit") || cmdToCheck.equals("type")) {
                    System.out.println(cmdToCheck + " is a shell builtin");
                    continue;
                }

                // 2. Search executable files in PATH environment variable
                String pathEnv = System.getenv("PATH");
                boolean found = false;

                if (pathEnv != null) {
                    // Split paths using the OS-agnostic separator (e.g., ':' on Linux/macOS)
                    String[] directories = pathEnv.split(File.pathSeparator);
                    
                    for (String dir : directories) {
                        Path fullPath = Paths.get(dir, cmdToCheck);
                        
                        // Check if file exists and has execution permissions
                        if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {
                            System.out.println(cmdToCheck + " is " + fullPath.toString());
                            found = true;
                            break;
                        }
                    }
                }

                // 3. Fallback if not found anywhere
                if (!found) {
                    System.out.println(cmdToCheck + ": not found");
                }
                continue;
            }

            System.out.println(input + ": command not found");
        }
        sc.close();
    }
}
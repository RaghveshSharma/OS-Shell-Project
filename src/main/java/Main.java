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

            // Handle the type builtin
            if (input.startsWith("type ")) {
                String cmdToCheck = input.substring(5).trim();
                
                // Check if the command is a recognized builtin
                if (cmdToCheck.equals("echo") || cmdToCheck.equals("exit") || cmdToCheck.equals("type")) {
                    System.out.println(cmdToCheck + " is a shell builtin");
                } else {
                    System.out.println(cmdToCheck + ": not found");
                }
                continue;
            }

            System.out.println(input + ": command not found");
        }
        sc.close();
    }
}
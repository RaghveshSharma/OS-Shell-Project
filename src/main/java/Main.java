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

            // Handle the echo builtin
            if (input.startsWith("echo ")) {
                String message = input.substring(5); // Extract everything after "echo "
                System.out.println(message);
                continue;
            }

            System.out.println(input + ": command not found");
        }
        sc.close();
    }
}
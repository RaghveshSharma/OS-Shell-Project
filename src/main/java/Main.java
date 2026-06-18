import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = sc.nextLine().trim();

            // Check if the user wants to terminate the shell
            if (input.equals("exit 0") || input.equals("exit")) {
                break;
            }

            System.out.println(input + ": command not found");
        }
        sc.close();
    }
}
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    static class Job {

        int jobNumber;

        long pid;

        String command;

        Process process;

        Job(int jobNumber, long pid, String command, Process process) {

            this.jobNumber = jobNumber;

            this.pid = pid;

            this.command = command;

            this.process = process;
        }
    }

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
            if (ch == '\\' && !inSingleQuotes && !inDoubleQuotes) {

                if (i + 1 < input.length()) {

                    current.append(input.charAt(i + 1));

                    i++;
                }

                continue;
            }

            // Single quotes
            if (ch == '\'' && !inDoubleQuotes) {

                inSingleQuotes = !inSingleQuotes;

                continue;
            }

            // Double quotes
            if (ch == '"' && !inSingleQuotes) {

                inDoubleQuotes = !inDoubleQuotes;

                continue;
            }

            // Space outside quotes
            if (Character.isWhitespace(ch) && !inSingleQuotes && !inDoubleQuotes) {

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

    public static void reapJobs(Map<Integer, Job> jobs) {

        List<Integer> removeJobs = new ArrayList<>();

        int maxJob = -1;

        int secondMaxJob = -1;

        for (Integer id : jobs.keySet()) {

            if (id > maxJob) {

                secondMaxJob = maxJob;

                maxJob = id;

            } else if (id > secondMaxJob) {

                secondMaxJob = id;
            }
        }

        for (Map.Entry<Integer, Job> entry : jobs.entrySet()) {

            Job job = entry.getValue();

            if (!job.process.isAlive()) {

                String marker = " ";

                if (job.jobNumber == maxJob) {

                    marker = "+";

                } else if (job.jobNumber == secondMaxJob) {

                    marker = "-";
                }

                String doneCommand = job.command;

                if (doneCommand.endsWith("&")) {

                    doneCommand = doneCommand.substring(0, doneCommand.length() - 1).trim();
                }

                System.out.printf("[%d]%s  %-24s%s%n", job.jobNumber, marker, "Done", doneCommand);

                removeJobs.add(job.jobNumber);
            }
        }

        for (Integer id : removeJobs) {

            jobs.remove(id);
        }
    }

    public static int getNextJobNumber(Map<Integer, Job> jobs) {

        int jobNumber = 1;

        while (jobs.containsKey(jobNumber)) {

            jobNumber++;
        }

        return jobNumber;
    }

    public static boolean isBuiltin(String cmd) {

        return cmd.equals("echo")
                || cmd.equals("type")
                || cmd.equals("pwd")
                || cmd.equals("cd")
                || cmd.equals("jobs")
                || cmd.equals("exit");
    }

    public static String executeBuiltin(
            List<String> parts,
            Path currentDirectory) {

        String command = parts.get(0);

        StringBuilder output =
                new StringBuilder();

        if (command.equals("echo")) {

            for (int i = 1; i < parts.size(); i++) {

                if (i > 1) {

                    output.append(" ");
                }

                output.append(parts.get(i));
            }

            output.append("\n");
        }

        else if (command.equals("pwd")) {

            output.append(currentDirectory)
                    .append("\n");
        }

        else if (command.equals("type")) {

            if (parts.size() >= 2) {

                String arg = parts.get(1);

                if (isBuiltin(arg)) {

                    output.append(arg)
                            .append(" is a shell builtin\n");

                } else {

                    output.append(arg)
                            .append(": not found\n");
                }
            }
        }

        return output.toString();
    }

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        Map<Integer, Job> jobs = new LinkedHashMap<>();

        while (true) {

            reapJobs(jobs);

            System.out.print("$ ");

            String input = sc.nextLine();

            if (input.trim().isEmpty()) {

                continue;
            }

            String originalInput = input;

            List<String> parts = parseCommand(input);

            boolean runInBackground = false;

            if (!parts.isEmpty() && parts.get(parts.size() - 1).equals("&")) {

                runInBackground = true;

                parts.remove(parts.size() - 1);
            }

            if (parts.isEmpty()) {

                continue;
            }

            String command = parts.get(0);

            // exit builtin

            if (command.equals("exit") || input.trim().equals("exit 0")) {

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

                if (Files.exists(newPath) && Files.isDirectory(newPath)) {

                    currentDirectory = newPath;

                } else {

                    System.out.println("cd: " + dir + ": No such file or directory");
                }

                continue;
            }

            // jobs builtin

            if (command.equals("jobs")) {

                int maxJob = -1;

                int secondMaxJob = -1;

                for (Integer id : jobs.keySet()) {

                    if (id > maxJob) {

                        secondMaxJob = maxJob;

                        maxJob = id;

                    } else if (id > secondMaxJob) {

                        secondMaxJob = id;
                    }
                }

                for (Map.Entry<Integer, Job> entry : jobs.entrySet()) {

                    Job job = entry.getValue();

                    String marker = " ";

                    if (job.jobNumber == maxJob) {

                        marker = "+";

                    } else if (job.jobNumber == secondMaxJob) {

                        marker = "-";
                    }

                    if (job.process.isAlive()) {

                        System.out.printf("[%d]%s  %-24s%s%n", job.jobNumber, marker, "Running", job.command);

                    } else {

                        String doneCommand = job.command;

                        if (doneCommand.endsWith("&")) {

                            doneCommand = doneCommand.substring(0, doneCommand.length() - 1).trim();
                        }

                        System.out.printf("[%d]%s  %-24s%s%n", job.jobNumber, marker, "Done", doneCommand);
                    }
                }

                List<Integer> removeJobs = new ArrayList<>();

                for (Map.Entry<Integer, Job> entry : jobs.entrySet()) {

                    Job job = entry.getValue();

                    if (!job.process.isAlive()) {

                        removeJobs.add(job.jobNumber);
                    }
                }

                for (Integer id : removeJobs) {

                    jobs.remove(id);
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

                            System.out.println(cmdToCheck + " is " + fullPath);

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

            // Dual command pipeline

            if (input.contains("|")) {

                String[] commands =
                        input.split("\\|", 2);

                List<String> leftCommand =
                        parseCommand(
                                commands[0].trim());

                List<String> rightCommand =
                        parseCommand(
                                commands[1].trim());

                boolean leftBuiltin =
                        isBuiltin(
                                leftCommand.get(0));

                boolean rightBuiltin =
                        isBuiltin(
                                rightCommand.get(0));

                byte[] leftOutput;

                // LEFT SIDE

                if (leftBuiltin) {

                    String out =
                            executeBuiltin(
                                    leftCommand,
                                    currentDirectory);

                    leftOutput =
                            out.getBytes();

                }

                else {

                    ProcessBuilder pb =
                            new ProcessBuilder(
                                    leftCommand);

                    pb.directory(
                            currentDirectory.toFile());

                    Process p =
                            pb.start();

                    leftOutput =
                            p.getInputStream()
                             .readAllBytes();

                    p.waitFor();
                }

                // RIGHT SIDE

                if (rightBuiltin) {

                    String out =
                            executeBuiltin(
                                    rightCommand,
                                    currentDirectory);

                    System.out.print(out);

                }

                else {

                    ProcessBuilder pb =
                            new ProcessBuilder(
                                    rightCommand);

                    pb.directory(
                            currentDirectory.toFile());

                    Process p =
                            pb.start();

                    p.getOutputStream()
                            .write(leftOutput);

                    p.getOutputStream()
                            .close();

                    p.getInputStream()
                            .transferTo(System.out);

                    p.waitFor();
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

                    if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {

                        List<String> commandWithArgs = new ArrayList<>();

                        commandWithArgs.add(command);

                        for (int i = 1; i < parts.size(); i++) {

                            commandWithArgs.add(parts.get(i));
                        }

                        ProcessBuilder pb = new ProcessBuilder(commandWithArgs);

                        pb.directory(currentDirectory.toFile());

                        pb.inheritIO();

                        Process process = pb.start();

                        if (runInBackground) {

                            int jobNumber = getNextJobNumber(jobs);

                            Job job = new Job(jobNumber, process.pid(), originalInput, process);

                            jobs.put(jobNumber, job);

                            System.out.println("[" + jobNumber + "] " + process.pid());

                        } else {

                            process.waitFor();
                        }

                        executed = true;

                        break;
                    }
                }
            }

            if (!executed) {

                System.out.println(command + ": command not found");
            }
        }

        sc.close();
    }
}

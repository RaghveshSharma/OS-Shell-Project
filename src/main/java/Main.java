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
    static Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

    static Map<Integer, Job> jobs = new LinkedHashMap<>();

    public static boolean isBuiltin(String command) {
        return command.equals("echo") || command.equals("exit") || command.equals("type")
                || command.equals("pwd") || command.equals("cd") || command.equals("jobs");
    }

    public static void executeBuiltinForPipeline(List<String> parts, String originalInput, java.io.PrintStream out) {
        String command = parts.get(0);
        if (command.equals("exit") || originalInput.trim().equals("exit 0")) {
            System.exit(0);
        }
        if (command.equals("echo")) {
            for (int i = 1; i < parts.size(); i++) {
                if (i > 1) {
                    out.print(" ");
                }
                out.print(parts.get(i));
            }
            out.println();
            return;
        }
        if (command.equals("pwd")) {
            out.println(currentDirectory);
            return;
        }
        if (command.equals("cd")) {
            if (parts.size() < 2) {
                return;
            }
            String dir = parts.get(1);
            Path newPath;
            if (dir.equals("~")) {
                newPath = Paths.get(System.getenv("HOME"));
            } else if (Paths.get(dir).isAbsolute()) {
                newPath = Paths.get(dir);
            } else {
                newPath = currentDirectory.resolve(dir);
            }
            newPath = newPath.normalize();
            if (Files.exists(newPath) && Files.isDirectory(newPath)) {
                currentDirectory = newPath;
            } else {
                out.println("cd: " + dir + ": No such file or directory");
            }
            return;
        }
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
                    out.printf("[%d]%s  %-24s%s%n", job.jobNumber, marker, "Running", job.command);
                } else {
                    String doneCommand = job.command;
                    if (doneCommand.endsWith("&")) {
                        doneCommand = doneCommand.substring(0, doneCommand.length() - 1).trim();
                    }
                    out.printf("[%d]%s  %-24s%s%n", job.jobNumber, marker, "Done", doneCommand);
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
            return;
        }
        if (command.equals("type")) {
            if (parts.size() < 2) {
                return;
            }
            String cmdToCheck = parts.get(1);
            if (isBuiltin(cmdToCheck)) {
                out.println(cmdToCheck + " is a shell builtin");
                return;
            }
            String pathEnv = System.getenv("PATH");
            boolean found = false;
            if (pathEnv != null) {
                String[] directories = pathEnv.split(File.pathSeparator);
                for (String dir : directories) {
                    Path fullPath = Paths.get(dir, cmdToCheck);
                    if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {
                        out.println(cmdToCheck + " is " + fullPath);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                out.println(cmdToCheck + ": not found");
            }
            return;
        }
    }

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
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

                String[] commands = input.split("\\|", 2);

                List<String> leftCommand = parseCommand(commands[0].trim());

                List<String> rightCommand = parseCommand(commands[1].trim());

                boolean leftBuiltin = isBuiltin(leftCommand.get(0));
                boolean rightBuiltin = isBuiltin(rightCommand.get(0));

                Process p1 = null;
                Process p2 = null;
                java.io.ByteArrayOutputStream leftBaos = null;

                if (leftBuiltin) {
                    leftBaos = new java.io.ByteArrayOutputStream();
                    executeBuiltinForPipeline(leftCommand, commands[0].trim(), new java.io.PrintStream(leftBaos));
                } else {
                    ProcessBuilder pb1 = new ProcessBuilder(leftCommand);
                    pb1.directory(currentDirectory.toFile());
                    pb1.redirectError(ProcessBuilder.Redirect.INHERIT);
                    p1 = pb1.start();
                }

                if (!rightBuiltin) {
                    ProcessBuilder pb2 = new ProcessBuilder(rightCommand);
                    pb2.directory(currentDirectory.toFile());
                    pb2.redirectError(ProcessBuilder.Redirect.INHERIT);
                    p2 = pb2.start();
                }

                Thread pipeThread = null;

                if (leftBuiltin && !rightBuiltin) {
                    p2.getOutputStream().write(leftBaos.toByteArray());
                    p2.getOutputStream().close();
                } else if (!leftBuiltin && !rightBuiltin) {
                    Process finalP1 = p1;
                    Process finalP2 = p2;
                    pipeThread = new Thread(() -> {
                        try {
                            java.io.InputStream in = finalP1.getInputStream();
                            java.io.OutputStream out = finalP2.getOutputStream();
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                                out.flush();
                            }
                        } catch (Exception e) {}
                        finally {
                            try { finalP2.getOutputStream().close(); } catch (Exception e) {}
                        }
                    });
                    pipeThread.start();
                } else if (!leftBuiltin && rightBuiltin) {
                    Process finalP1 = p1;
                    Thread discardThread = new Thread(() -> {
                        try {
                            java.io.InputStream in = finalP1.getInputStream();
                            byte[] buffer = new byte[1024];
                            while (in.read(buffer) != -1) {}
                        } catch (Exception e) {}
                    });
                    discardThread.start();
                }

                Thread outputThread = null;

                if (rightBuiltin) {
                    executeBuiltinForPipeline(rightCommand, commands[1].trim(), System.out);
                } else {
                    Process finalP2 = p2;
                    outputThread = new Thread(() -> {
                        try {
                            java.io.InputStream in = finalP2.getInputStream();
                            java.io.OutputStream out = System.out;
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                                out.flush();
                            }
                        } catch (Exception e) {}
                    });
                    outputThread.start();

                    p2.waitFor();
                }

                if (pipeThread != null) pipeThread.join();
                if (outputThread != null) outputThread.join();

                if (!leftBuiltin) {
                    if (p1.isAlive()) {
                        p1.destroy();
                    }
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

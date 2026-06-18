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
        return cmd.equals("echo") || cmd.equals("type") || cmd.equals("pwd") || cmd.equals("cd") || cmd.equals("jobs") || cmd.equals("exit");
    }

    public static String executeBuiltin(List<String> parts, Path currentDirectory) {
        String command = parts.get(0);
        StringBuilder output = new StringBuilder();
        if (command.equals("echo")) {
            for (int i = 1; i < parts.size(); i++) {
                if (i > 1) {
                    output.append(" ");
                }
                output.append(parts.get(i));
            }
            output.append("\n");
        } else if (command.equals("pwd")) {
            output.append(currentDirectory).append("\n");
        } else if (command.equals("type")) {
            if (parts.size() >= 2) {
                String arg = parts.get(1);
                if (isBuiltin(arg)) {
                    output.append(arg).append(" is a shell builtin\n");
                } else {
                    output.append(arg).append(": not found\n");
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

            String outputFile = null;

            String errorFile = null;

            if (input.contains("2>")) {

                String[] temp = input.split("2>", 2);

                input = temp[0].trim();

                errorFile = temp[1].trim();
            }

            else if (input.contains("1>")) {

                String[] temp = input.split("1>", 2);

                input = temp[0].trim();

                outputFile = temp[1].trim();
            }

            else if (input.contains(">")) {

                String[] temp = input.split(">", 2);

                input = temp[0].trim();

                outputFile = temp[1].trim();
            }

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

            // Multi-stage pipeline

            if (input.contains("|")) {

                String[] commands =
                        input.split("\\|");

                List<Process> processes =
                        new ArrayList<>();

                List<Thread> threads =
                        new ArrayList<>();

                Process previous = null;

                for (int i = 0; i < commands.length; i++) {

                    List<String> cmd =
                            parseCommand(
                                    commands[i].trim());

                    Process current;
                    if (isBuiltin(cmd.get(0))) {
                        String out = executeBuiltin(cmd, currentDirectory);
                        current = new Process() {
                            java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(out.getBytes());
                            public java.io.OutputStream getOutputStream() { return java.io.OutputStream.nullOutputStream(); }
                            public java.io.InputStream getInputStream() { return in; }
                            public java.io.InputStream getErrorStream() { return java.io.InputStream.nullInputStream(); }
                            public int waitFor() { return 0; }
                            public int exitValue() { return 0; }
                            public void destroy() {}
                            public boolean isAlive() { return false; }
                        };
                    } else {
                        ProcessBuilder pb =
                                new ProcessBuilder(cmd);

                        pb.directory(
                                currentDirectory.toFile());

                        current =
                                pb.start();
                    }

                    processes.add(current);

                    // connect previous stdout
                    // to current stdin

                    if (previous != null) {

                        Process left = previous;

                        Process right = current;

                        Thread t =
                                new Thread(() -> {

                                    try {

                                        byte[] buffer =
                                                new byte[1024];

                                        int read;

                                        while ((read =
                                                left.getInputStream()
                                                        .read(buffer))
                                                != -1) {

                                            right.getOutputStream()
                                                    .write(
                                                            buffer,
                                                            0,
                                                            read);

                                            right.getOutputStream()
                                                    .flush();
                                        }

                                    }

                                    catch (Exception e) {

                                    }

                                    finally {

                                        try {

                                            right.getOutputStream()
                                                    .close();

                                        }

                                        catch (Exception e) {

                                        }
                                    }

                                });

                        t.start();

                        threads.add(t);
                    }

                    previous = current;
                }

                // last process output

                Process last =
                        processes.get(
                                processes.size() - 1);

                Thread outputThread =
                        new Thread(() -> {

                            try {

                                byte[] buffer =
                                        new byte[1024];

                                int read;

                                while ((read =
                                        last.getInputStream()
                                                .read(buffer))
                                        != -1) {

                                    System.out.write(
                                            buffer,
                                            0,
                                            read);

                                    System.out.flush();
                                }

                            }

                            catch (Exception e) {

                            }

                        });

                outputThread.start();

                // wait for last

                last.waitFor();

                // destroy remaining alive processes

                for (Process p : processes) {

                    if (p.isAlive()) {

                        p.destroyForcibly();
                    }
                }

                outputThread.join();

                continue;
            }

            // exit builtin

            if (command.equals("exit") || input.trim().equals("exit 0")) {

                break;
            }

            // echo builtin

            if (command.equals("echo")) {

                StringBuilder out =
                        new StringBuilder();

                for (int i = 1; i < parts.size(); i++) {

                    if (i > 1) {

                        out.append(" ");
                    }

                    out.append(parts.get(i));
                }

                out.append("\n");

                if (outputFile != null) {

                    Files.writeString(

                            Paths.get(outputFile),

                            out.toString()

                    );

                }

                else {

                    System.out.print(out);
                }

                continue;
            }

            // pwd builtin

            if (command.equals("pwd")) {

                String out =
                        currentDirectory
                                .toString()
                                + "\n";

                if (outputFile != null) {

                    Files.writeString(

                            Paths.get(outputFile),

                            out

                    );

                }

                else {

                    System.out.print(out);
                }

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

                String result = "";

                if (cmdToCheck.equals("echo")
                        || cmdToCheck.equals("exit")
                        || cmdToCheck.equals("type")
                        || cmdToCheck.equals("pwd")
                        || cmdToCheck.equals("cd")
                        || cmdToCheck.equals("jobs")) {

                    result =
                            cmdToCheck
                                    + " is a shell builtin\n";

                } else {

                    String pathEnv = System.getenv("PATH");

                    boolean found = false;

                    if (pathEnv != null) {

                        String[] directories = pathEnv.split(File.pathSeparator);

                        for (String dir : directories) {

                            Path fullPath = Paths.get(dir, cmdToCheck);

                            if (Files.exists(fullPath) && Files.isExecutable(fullPath)) {

                                result =
                                        cmdToCheck
                                                + " is "
                                                + fullPath
                                                + "\n";

                                found = true;

                                break;
                            }
                        }
                    }

                    if (!found) {

                        result =
                                cmdToCheck
                                        + ": not found\n";
                    }

                }

                if (outputFile != null) {

                    Files.writeString(

                            Paths.get(outputFile),

                            result

                    );

                }

                else {

                    System.out.print(result);
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

                        ProcessBuilder pb =
                                new ProcessBuilder(commandWithArgs);

                        pb.directory(currentDirectory.toFile());


                        // stdout

                        if (outputFile != null) {

                            pb.redirectOutput(

                                    new File(outputFile)

                            );

                        }

                        else {

                            pb.redirectOutput(
                                    ProcessBuilder.Redirect.INHERIT);
                        }


                        // stderr

                        if (errorFile != null) {

                            pb.redirectError(

                                    new File(errorFile)

                            );

                        }

                        else {

                            pb.redirectError(

                                    ProcessBuilder.Redirect.INHERIT

                            );
                        }

                        Process process =
                                pb.start();

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

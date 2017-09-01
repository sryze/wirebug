/*
 * This file is part of Wirebug.
 *
 * Wirebug is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wirebug is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wirebug.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.sryze.wirebug;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import timber.log.Timber;

public class Shell {

    private static Shell globalShell;

    private final Runtime runtime;
    private int logPriority = Log.DEBUG;
    private boolean isLoggingEnabled = false;

    public Shell(Runtime runtime) {
        this.runtime = runtime;
    }

    private static String joinArgs(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                builder.append(" ");
            }
            if (args[i].contains(" ")) {
                builder.append(String.format("\"%s\"", args[i]));
            } else {
                builder.append(args[i]);
            }
        }
        return builder.toString();
    }

    private static String getProcessOutput(Process process) throws IOException {
        StringBuilder outputBuilder = new StringBuilder();
        String line;

        BufferedReader stdoutReader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = stdoutReader.readLine()) != null) {
            outputBuilder.append(line).append("\n");
        }

        BufferedReader stderrReader =
                new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            outputBuilder.append(line);
        }

        return outputBuilder.toString();
    }

    public static Shell getShell() {
        if (globalShell == null) {
            globalShell = new Shell(Runtime.getRuntime());
        }
        return globalShell;
    }

    public void setLogPriority(int priority) {
        logPriority = priority;
    }

    public void setLoggingEnabled(boolean isEnabled) {
        isLoggingEnabled = isEnabled;
    }

    private void logCommand(String command, String output, int exitStatus) {
        if (isLoggingEnabled) {
            Timber.log(logPriority, "$?=%d, %s: %s", exitStatus, command, output);
        }
    }

    public String exec(String command) throws ShellException {
        try {
            Process process = runtime.exec(command);
            String output = getProcessOutput(process);
            process.waitFor();
            logCommand(command, output, process.exitValue());
            return output;
        } catch (IOException | InterruptedException e) {
            throw new ShellException(e);
        }
    }

    public String exec(String[] args) throws ShellException {
        try {
            Process process = runtime.exec(args);
            String output = getProcessOutput(process);
            process.waitFor();
            logCommand(joinArgs(args), output, process.exitValue());
            return output;
        } catch (IOException | InterruptedException e) {
            throw new ShellException(e);
        }
    }

    public boolean canExecAsRoot() {
        try {
            return exec("su -c id").startsWith("uid=0(root)");
        } catch (ShellException e) {
            return false;
        }
    }

    public String execAsRoot(String command) throws ShellException {
        return exec(new String[]{"su", "-c", command});
    }
}

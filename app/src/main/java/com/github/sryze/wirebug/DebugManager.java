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

import timber.log.Timber;

public class DebugManager {
    private static final String ADB_TCP_PORT_PROPERTY = "service.adb.tcp.port";
    private static final int ADB_TCP_PORT_DEFAULT = 5555;

    public static boolean isTcpDebuggingEnabled() {
        return getAdbTcpPort() > 0;
    }

    public static void setTcpDebuggingEnabled(boolean isEnabled) {
        if (setAdbTcpPort(isEnabled ? ADB_TCP_PORT_DEFAULT : 0)) {
            Timber.i("Debugging over TCP is enabled: %s", isEnabled ? "YES" : "NO");
            Timber.i("Restarting ADB daemon (this will kill your debugging session)");
            restartAdbDaemon();
        }
    }

    public static int getAdbTcpPort() {
        try {
            String output = Shell.getShell().exec("getprop " + ADB_TCP_PORT_PROPERTY);
            return Integer.parseInt(output.trim());
        } catch (ShellException e) {
            Timber.e("Error getting current TCP port: %s", e.getMessage());
        } catch (NumberFormatException e) {
            // OK
        }
        return 0;
    }

    public static boolean setAdbTcpPort(int port) {
        try {
            String portArg = port > 0 ? String.format("%d", port) : "\"\"";
            String command = String.format("setprop %s %s", ADB_TCP_PORT_PROPERTY, portArg);
            Shell.getShell().execAsRoot(command);
            return true;
        } catch (ShellException e) {
            Timber.e("Error setting TCP port (%s): %s", port, e.getMessage());
            return false;
        }
    }

    public static void restartAdbDaemon() {
        try {
            Shell.getShell().execAsRoot("stop adbd; start adbd");
        } catch (ShellException e) {
            Timber.e("Error restarting ADB daemon: %s", e.getMessage());
        }
    }
}

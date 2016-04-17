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

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtils {

    @SuppressLint("DefaultLocale")
    public static String getStringFromIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d",
            ipAddress & 0xFF,
            (ipAddress >> 8) & 0xFF,
            (ipAddress >> 16) & 0xFF,
            (ipAddress >> 24) & 0xFF);
    }

    public static boolean isConnectedToWifi(ConnectivityManager connectivityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected()) {
                    return true;
                }
            }
        } else {
            for (NetworkInfo networkInfo : connectivityManager.getAllNetworkInfo()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }
}

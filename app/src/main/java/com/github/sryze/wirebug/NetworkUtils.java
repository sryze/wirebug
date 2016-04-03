package com.github.sryze.wirebug;

public class NetworkUtils {

    public static String getStringFromIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d",
                ipAddress & 0xFF,
                (ipAddress >> 8) & 0xFF,
                (ipAddress >> 16) & 0xFF,
                (ipAddress >> 24) & 0xFF);
    }

}

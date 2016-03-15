Wirebug
=======

Allows you to enable or disable debugging over Wi-Fi from within the device,
without a USB cable. This can come in handy if your USB cable broke or you
lost/forgot it somewhere.

**Note:** Requires root permissions.

![Screenshot](screenshot.png)

Why does it need root?
----------------------

Wirebug needs root permissions to write to the `system.adb.tcp.port`
property and to restart the ADB daemon.

If the `su` command is not available it will only be able to reflect
the current debugging status in the notification area but not change
it (may be still be useful in case you use `adb tcpip`).

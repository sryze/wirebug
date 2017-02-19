Wirebug
=======

Allows you to enable or disable debugging over Wi-Fi from within the device,
without an USB cable. This can come in handy if your USB cable broke or you
lost/forgot it somewhere.

**Note:** Requires root permissions _(read below)_.

![Screenshot](screenshot.png)

Why does it need root?
----------------------

Wirebug needs root permissions to write to the `system.adb.tcp.port`
property and to restart the ADB daemon.
If the `su` command is not available the app will be able to reflect
the current debugging status in the notification area, but not change
it.

What about `adb tcpip`?
-----------------------

Even without root, Wi-Fi debugging can be enabled if shortly connecting to a PC with an USB cable and running `adb tcpip portnumber` (**portnumber** should be >1024) and then disconnecting. Until the next reboot you can use Wirebug to connect to host **localhost** and port **portnumber** with the same effect. Remember that this will not bring you any additional permissions, the shell has the same non-root limitations.

#!/bin/sh

# More information:
# https://github.com/0xFireball/root_avd

adb root \
    && adb remount \
    && adb push $ROOT_AVD_DIR/SuperSU/x64/su /system/xbin/su \
    && adb shell chmod 0755 /system/xbin/su \
    && adb shell setenforce 0 \
    && adb shell su --install
adb shell su --daemon&

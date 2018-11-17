#!/bin/sh

# More information:
# https://github.com/0xFireball/root_avd

cd $ANDROID_SDK_ROOT/emulator \
    && emulator -avd $AVD_NAM -writable-system -selinux disabled -qemu

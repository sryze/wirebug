#!/bin/sh

# More information:
# https://github.com/0xFireball/root_avd

cd $ANDROID_SDK_ROOT/emulator \
    && emulator -avd Pixel_2_API_28_Rooted -snapshot Rooted

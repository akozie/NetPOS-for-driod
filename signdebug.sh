#!/bin/zsh

#./gradlew assembleDebug
cd /Users/oluwatayo/AndroidStudioProjects/NetPOS\ Server/app/build/outputs/apk/debug || exit
signapk.sh app-debug.apk netpos-debug-signed.apk
adb -s B1791E1XL8080026 install -r netpos-debug-signed.apk
adb shell am start -n com.woleapp.netpos.debug/com.woleapp.netpos.ui.activities.AuthenticationActivity
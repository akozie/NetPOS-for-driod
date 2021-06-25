#!/bin/zsh

./gradlew assembleDebug
mv /Users/oluwatayo/AndroidStudioProjects/NetPOS\ Server/app/build/outputs/apk/debug/app-debug.apk /Users/oluwatayo/Development/signedd/signer/netpos-server-debug.apk
cd /Users/oluwatayo/Development/signedd/signer || exit
signapk netpos-server-debug.apk netpos-debug-signed.apk
adb -s B1791E1XL8080026 install -r netpos-debug-signed.apk
adb shell am start -n com.woleapp.netpos.debug/com.woleapp.netpos.ui.activities.AuthenticationActivity
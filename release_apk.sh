#!/bin/bash

APK_VER=$(cat app/build.gradle|grep versionName|head -n 1|awk '{print $2}'|sed -e 's/"//g')
echo "APK version: $APK_VER"

bash gradlew clean assembleCom_ihanghai_googlesearch_without_text_input_Addon_amazon_app_storeRelease

rm *.apk
find . -name "*.apk" -exec mv -vf {} . \;
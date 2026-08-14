#!/bin/bash

# Tạo debug keystore nếu chưa có
if [ ! -f "debug.keystore" ]; then
  echo "Generating debug keystore..."
  keytool -genkey -v -keystore debug.keystore \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
  echo "Debug keystore created successfully!"
else
  echo "Debug keystore already exists."
fi

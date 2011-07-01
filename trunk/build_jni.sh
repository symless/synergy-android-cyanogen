#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <NDK Base>"
    exit 1
fi

NDK_LOCATION=$1

# Build 
$1/ndk-build APP_OPTIM=release

cmake_minimum_required(VERSION 3.10.2)

project("knobdroid_usbfs" C)

# Talks to the adapter through raw Linux usbfs ioctls on the file descriptor
# from UsbDeviceConnection; no libusb needed.
add_library(knobdroid_usbfs SHARED
        usbfs_core.c
        usbfs_jni.c)

target_compile_options(knobdroid_usbfs PRIVATE -Wall -Wextra -Werror)

find_library(log-lib log)

target_link_libraries(knobdroid_usbfs ${log-lib})

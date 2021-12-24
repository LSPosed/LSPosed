LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE            := yahfa
LOCAL_C_INCLUDES        := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES         := src/HookMain.cpp src/trampoline.cpp
LOCAL_EXPORT_LDLIBS     := -llog
LOCAL_STATIC_LIBRARIES  := libcxx
include $(BUILD_STATIC_LIBRARY)

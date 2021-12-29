LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE                  := daemon
LOCAL_SRC_FILES               := logcat.cpp
LOCAL_STATIC_LIBRARIES        := cxx
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_LDLIBS                  := -llog
include $(BUILD_SHARED_LIBRARY)

$(call import-module,prefab/cxx)
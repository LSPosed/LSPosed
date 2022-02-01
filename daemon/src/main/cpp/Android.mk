LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE                  := daemon
LOCAL_C_INCLUDES              := ../core/src/main/cpp/main/include/
LOCAL_SRC_FILES               := logcat.cpp obfuscation.cpp ../../../../core/src/main/cpp/main/api/config.cpp
LOCAL_STATIC_LIBRARIES        := cxx dex_builder
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_LDLIBS                  := -llog -landroid
include $(BUILD_SHARED_LIBRARY)

include ../core/src/main/cpp/external/DexBuilder/Android.mk
$(call import-module,prefab/cxx)
LOCAL_PATH := $(call my-dir)
define walk
  $(wildcard $(1)) $(foreach e, $(wildcard $(1)/*), $(call walk, $(e)))
endef

include $(CLEAR_VARS)
LOCAL_MODULE           := lspd
LOCAL_C_INCLUDES       := $(LOCAL_PATH)/include $(LOCAL_PATH)/src
FILE_LIST              := $(filter %.cpp, $(call walk, $(LOCAL_PATH)/src))
LOCAL_SRC_FILES        := $(FILE_LIST:$(LOCAL_PATH)/%=%)
LOCAL_STATIC_LIBRARIES := cxx yahfa riru dobby dex_builder
LOCAL_CFLAGS           := -DRIRU_MODULE -DRIRU_MODULE_API_VERSION=${RIRU_MODULE_API_VERSION}
LOCAL_CFLAGS           += -DMODULE_NAME=${MODULE_NAME}
LOCAL_LDLIBS           := -llog
include $(BUILD_SHARED_LIBRARY)

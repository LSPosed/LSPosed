APP_ABI := arm64-v8a armeabi-v7a x86 x86_64
APP_PLATFORM := android-23
APP_CFLAGS := -std=gnu99
APP_CPPFLAGS := -std=c++11
APP_STL := c++_static
APP_SHORT_COMMANDS := true

ifeq ($(NDK_DEBUG),1)
$(info building DEBUG version...)
APP_CFLAGS += -O0
APP_CPPFLAGS += -O0
else
$(info building RELEASE version...)
APP_CFLAGS += -fvisibility=hidden -fvisibility-inlines-hidden -O2
APP_CPPFLAGS += -fvisibility=hidden -fvisibility-inlines-hidden -O2
endif

# do not remove this, or your module will crash apps on Android Q
SCS_FLAGS := -ffixed-x18
APP_LDFLAGS += $(SCS_FLAGS)
APP_CFLAGS += $(SCS_FLAGS)
APP_CPPFLAGS += $(SCS_FLAGS)
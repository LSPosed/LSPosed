project(external)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
SET_OPTION(Plugin.SymbolResolver OFF)

set(LIBCXX_SOURCES
        algorithm.cpp
#        any.cpp
        atomic.cpp
        barrier.cpp
#        bind.cpp
        charconv.cpp
        chrono.cpp
        condition_variable.cpp
        condition_variable_destructor.cpp
#        debug.cpp
        exception.cpp
#        filesystem/directory_iterator.cpp
#        filesystem/int128_builtins.cpp
#        filesystem/operations.cpp
        functional.cpp
        future.cpp
        hash.cpp
#        ios.cpp
#        ios.instantiations.cpp
#        iostream.cpp
#        locale.cpp
        memory.cpp
        mutex.cpp
        mutex_destructor.cpp
        new.cpp
        optional.cpp
        random.cpp
#        regex.cpp
#        ryu/d2fixed.cpp
#        ryu/d2s.cpp
#        ryu/f2s.cpp
        shared_mutex.cpp
        stdexcept.cpp
        string.cpp
#        strstream.cpp
        system_error.cpp
        thread.cpp
#        typeinfo.cpp
        utility.cpp
        valarray.cpp
        variant.cpp
        vector.cpp
        )

list(TRANSFORM LIBCXX_SOURCES PREPEND cxx/src/)

set(LIBCXX_EXPORT_FLAGS
        -DLIBCXX_BUILDING_LIBCXXABI
        -D_LIBCPP_NO_EXCEPTIONS
        -D_LIBCPP_NO_RTTI
        -D_LIBCPP_BUILDING_LIBRARY
        -D_LIBCPP_DISABLE_VISIBILITY_ANNOTATIONS
        -D_LIBCXXABI_NO_EXCEPTIONS
        -D_LIBCPP_HAS_NO_LOCALIZATION
        )
set(LIBCXX_FLAGS
        -fvisibility-global-new-delete-hidden
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        )
set(LIBCXX_EXPORT_INCLUDES cxx/include)
set(LIBCXX_INCLUDES cxx/src)

set(LIBCXXABI_SOURCES
        abort_message.cpp
        cxa_aux_runtime.cpp
        cxa_default_handlers.cpp
        cxa_exception_storage.cpp
        cxa_guard.cpp
        cxa_handlers.cpp
        cxa_noexception.cpp
        cxa_thread_atexit.cpp
        cxa_vector.cpp
        cxa_virtual.cpp
        stdlib_exception.cpp
        stdlib_new_delete.cpp
        stdlib_stdexcept.cpp
        stdlib_typeinfo.cpp
        )
list(TRANSFORM LIBCXXABI_SOURCES PREPEND cxx/src/abi/)
set(LIBCXXABI_FLAGS
        -Wno-macro-redefined
        -Wno-unknown-attributes
        -DHAS_THREAD_LOCAL)
set(LIBCXXABI_INCLUDES cxx/include/abi)

add_library(cxx STATIC ${LIBCXX_SOURCES} ${LIBCXXABI_SOURCES})
target_compile_options(cxx PUBLIC ${LIBCXX_EXPORT_FLAGS})
target_compile_options(cxx PRIVATE ${LIBCXX_FLAGS} ${LIBCXXABI_FLAGS} -ffunction-sections -fdata-sections)
target_include_directories(cxx PUBLIC ${LIBCXX_EXPORT_INCLUDES})
target_include_directories(cxx PRIVATE ${LIBCXX_INCLUDES} ${LIBCXXABI_INCLUDES})

link_libraries(cxx)

OPTION(LSPLANT_BUILD_SHARED OFF)
add_subdirectory(lsplant/lsplant/src/main/jni)
add_subdirectory(dobby)
add_subdirectory(fmt)
target_compile_definitions(fmt-header-only INTERFACE FMT_STATIC_THOUSANDS_SEPARATOR=1 FMT_USE_FLOAT=0 FMT_USE_DOUBLE=0 FMT_USE_LONG_DOUBLE=0)

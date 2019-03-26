#!/usr/bin/python

import os

STUB_FILE_NAME = "MethodHookerStubs"

TEMP_STUB_CLASS_WRAPPER = """package com.swift.sandhook.xposedcompat.hookstub;

import static com.swift.sandhook.xposedcompat.hookstub.HookStubManager.hookBridge;
import static com.swift.sandhook.xposedcompat.hookstub.HookStubManager.getMethodId;
import static com.swift.sandhook.xposedcompat.hookstub.HookStubManager.originMethods;
import static com.swift.sandhook.xposedcompat.utils.DexLog.printCallOriginError;

/**
*   this file is auto gen by genhookstubs.py
*   it is for sandhook internal hooker & backup methods
**/
public class MethodHookerStubs%d {
%s
}
"""

TEMP_STUB_HOOK_METHOD_NAME = """stub_hook_%d"""
TEMP_STUB_HOOK_BACKUP_NAME = """stub_backup_%d"""
TEMP_STUB_CALL_ORIGIN_NAME = """call_origin_%d_%d"""

TEMP_STUB_GET_METHOD_ID_NAME = """getMethodId(%d, %d)"""

JAVA_TYPE_INT = "int"
JAVA_CAST_INT = "(int)"
JAVA_TYPE_LONG = "long"

TEMP_STUB_HOOK_METHOD = """
    public static %s %s(%s) throws Throwable {
        return %s hookBridge(%s, %s %s);
    }
"""

TEMP_STUB_BACKUP_METHOD = """
    public static %s %s(%s) throws Throwable {
        try {
            printCallOriginError(originMethods[%s]);
        } catch (Throwable throwable) {}
        return 0;
    }
"""

TEMP_STUB_CALL_ORIGIN_CLASS = """
    static class %s implements CallOriginCallBack {
        @Override
        public long call(long... args) throws Throwable {
            return %s(%s);
        }
    }
"""

TEMP_STUB_INFO = """
    public static boolean hasStubBackup = %s;
    public static int[] stubSizes = {%s};
"""


STUB_SIZES_32 = [10,20,30,30,30,30,30,20,10,10,5,5,3]
STUB_SIZES_64 = [10,20,30,30,30,30,50,50]
HAS_BACKUP = False


def getMethodId(args, index):
    return TEMP_STUB_GET_METHOD_ID_NAME % (args, index)

def getMethodHookName(index):
    return TEMP_STUB_HOOK_METHOD_NAME % index

def getMethodBackupName(index):
    return TEMP_STUB_HOOK_BACKUP_NAME % index

def getCallOriginClassName(args, index):
    return TEMP_STUB_CALL_ORIGIN_NAME % (args, index)


def genArgsList(is64Bit, isDefine, length):
    args_list = ""
    for i in range(length):
        if (i != 0):
            args_list += ", "
        if isDefine:
            if (is64Bit):
                args_list += (JAVA_TYPE_LONG + " " + "a" + str(i))
            else:
                args_list += (JAVA_TYPE_INT + " " + "a" + str(i))
        else:
            args_list += ("a" + str(i))
    return args_list


def genArgsListForCallOriginMethod(is64Bit, length):
    arg_name = """args[%s]"""
    args_list = ""
    for i in range(length):
        if (i != 0):
            args_list += ", "
        if (is64Bit):
            args_list += arg_name % i
        else:
            args_list += (JAVA_CAST_INT + arg_name % i)
    return args_list


def genHookMethod(is64Bit, args, index):
    java_type = JAVA_TYPE_LONG if is64Bit else JAVA_TYPE_INT
    cast = "" if is64Bit else JAVA_CAST_INT
    args_list_pre = ", " if args > 0 else ""
    args_list = genArgsList(is64Bit, False, args)
    args_list_def = genArgsList(is64Bit, True, args)
    call_origin_obj = ("new " + getCallOriginClassName(args, index) + "()") if HAS_BACKUP else "null"
    method = TEMP_STUB_HOOK_METHOD % (java_type, getMethodHookName(index), args_list_def, cast, getMethodId(args, index), call_origin_obj, args_list_pre + args_list)
    return method


def genBackupMethod(is64Bit, args, index):
    java_type = JAVA_TYPE_LONG if is64Bit else JAVA_TYPE_INT
    args_list_def = genArgsList(is64Bit, True, args)
    method = TEMP_STUB_BACKUP_METHOD % (java_type, getMethodBackupName(index), args_list_def, getMethodId(args, index))
    return method

def genCallOriginClass(is64Bit, args, index):
    method = TEMP_STUB_CALL_ORIGIN_CLASS % (getCallOriginClassName(args, index), getMethodBackupName(index), genArgsListForCallOriginMethod(is64Bit, args))
    return method

def genStubInfo32():
    hasStub = "true" if HAS_BACKUP else "false"
    stubSizes = ""
    for args in range(len(STUB_SIZES_32)):
        if (args != 0):
            stubSizes += ", "
        stubSizes += str(STUB_SIZES_32[args])
    return TEMP_STUB_INFO % (hasStub, stubSizes)

def genStubInfo64():
    hasStub = "true" if HAS_BACKUP else "false"
    stubSizes = ""
    for args in range(len(STUB_SIZES_64)):
        if (args != 0):
            stubSizes += ", "
        stubSizes += str(STUB_SIZES_64[args])
    return TEMP_STUB_INFO % (hasStub, stubSizes)

def gen32Stub(packageDir):
    class_content = genStubInfo32()
    class_name = STUB_FILE_NAME + "32"
    for args in range(len(STUB_SIZES_32)):
        for index in range(STUB_SIZES_32[args]):
            class_content += """\n\n\t//stub of arg size %d, index %d""" % (args, index)
            class_content += genHookMethod(False, args, index)
            if HAS_BACKUP:
                class_content += "\n"
                class_content += genCallOriginClass(False, args, index)
                class_content += "\n"
                class_content += genBackupMethod(False, args, index)
                class_content += "\n"
    class_str = TEMP_STUB_CLASS_WRAPPER % (32, class_content)
    javaFile = open(os.path.join(packageDir, class_name + ".java"), "w")
    javaFile.write(class_str)
    javaFile.close()


def gen64Stub(packageDir):
    class_content = genStubInfo64()
    class_name = STUB_FILE_NAME + "64"
    for args in range(len(STUB_SIZES_64)):
        for index in range(STUB_SIZES_64[args]):
            class_content += """\n\n\t//stub of arg size %d, index %d""" % (args, index)
            class_content += genHookMethod(True, args, index)
            if HAS_BACKUP:
                class_content += "\n"
                class_content += genCallOriginClass(True, args, index)
                class_content += "\n"
                class_content += genBackupMethod(True, args, index)
                class_content += "\n"
    class_str = TEMP_STUB_CLASS_WRAPPER % (64, class_content)
    javaFile = open(os.path.join(packageDir, class_name + ".java"), "w")
    javaFile.write(class_str)
    javaFile.close()


def genStub(packageDir):
    for fileName in os.listdir(packageDir):
        if fileName.startswith(STUB_FILE_NAME):
            os.remove(os.path.join(packageDir, fileName))
    gen32Stub(packageDir)
    gen64Stub(packageDir)


if __name__ == "__main__":
    genStub(os.path.join(os.path.dirname(os.path.realpath(__file__)),
                         "src/main/java/com/swift/sandhook/xposedcompat/hookstub"))

/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.instrumentation.FramesComputationMode
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

val apiCode: Int by rootProject.extra
val verName: String by rootProject.extra
val verCode: Int by rootProject.extra

plugins {
    id("com.android.library")
}

android {
    namespace = "org.lsposed.lspd.core"

    buildFeatures {
        androidResources = false
    }

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")

        buildConfigField("int", "API_CODE", "$apiCode")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

copy {
    from("src/main/jni/template/") {
        expand("VERSION_CODE" to "$verCode", "VERSION_NAME" to verName)
    }
    into("src/main/jni/src/")
}

abstract class ExampleClassVisitorFactory : AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun createClassVisitor(
        classContext: ClassContext, nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                val newSuperName = "xposed/dummy/X${superName?.substringAfterLast('/')}SuperClass"
                println("replace super class of $name to $newSuperName")
                super.visit(
                    version,
                    access,
                    name,
                    signature,
                    newSuperName,
                    interfaces
                )
            }

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                return object : MethodVisitor(
                    Opcodes.ASM9, super.visitMethod(
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions
                    )
                ) {
                    override fun visitVarInsn(opcode: Int, `var`: Int) {
                    }

                    override fun visitInsn(opcode: Int) {
                        if (opcode != Opcodes.ACONST_NULL) {
                            super.visitInsn(opcode)
                        }
                    }

                    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                        super.visitMaxs(
                            if (maxLocals > maxStack) maxLocals else maxStack,
                            maxLocals
                        )
                    }

                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String?,
                        name: String?,
                        instDescriptor: String?,
                        isInterface: Boolean
                    ) {
                        if (opcode == Opcodes.INVOKESPECIAL) {
                            for (i in 0 .. Type.getMethodType(descriptor).argumentTypes.size) {
                                super.visitVarInsn(Opcodes.ALOAD, i)
                            }
                            val newOwner =
                                "xposed/dummy/X${owner?.substringAfterLast('/')}SuperClass"
                            println("replace method call of $owner.$name$instDescriptor to $newOwner.$name$descriptor")
                            super.visitMethodInsn(
                                opcode,
                                newOwner,
                                name,
                                descriptor,
                                isInterface
                            )
                        } else {
                            super.visitMethodInsn(
                                opcode,
                                owner,
                                name,
                                instDescriptor,
                                isInterface
                            )
                        }
                    }
                }
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className.startsWith("android.content.res.Xposed")
    }
}


androidComponents.onVariants { variant ->
    variant.instrumentation.transformClassesWith(
        ExampleClassVisitorFactory::class.java, InstrumentationScope.PROJECT
    ) {}
    variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("de.upb.cs.swt:axml:2.1.3")
    compileOnly("androidx.annotation:annotation:1.5.0")
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.services.managerService)
}

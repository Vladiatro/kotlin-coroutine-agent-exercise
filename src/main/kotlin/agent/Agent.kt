package agent

import java.lang.instrument.Instrumentation
import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.MethodVisitor
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

import jdk.internal.org.objectweb.asm.Opcodes.*
import java.util.*

class WatchingMethodVisitor(api: Int, mv: MethodVisitor) : MethodVisitor(api, mv) {
    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == INVOKESTATIC && owner == "example/CoroutineExampleKt" && name == "test"
                && desc == "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;") {
            visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
            visitLdcInsn("Test detected")
            visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }
}

class WatchingClassVisitor(api: Int, classWriter: ClassWriter) : ClassVisitor(api, classWriter) {
    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?,
                             exceptions: Array<out String>?): MethodVisitor {
        val methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
        return WatchingMethodVisitor(super.api, methodVisitor)
    }
}

class Agent {
    companion object : ClassFileTransformer {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer(Agent.Companion)
        }

        override fun transform(loader: ClassLoader?, className: String?,
                               classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain?,
                               classfileBuffer: ByteArray?): ByteArray {
            val classReader = ClassReader(classfileBuffer)
            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
            val watchingClassVisitor = WatchingClassVisitor(ASM5, classWriter)
            classReader.accept(watchingClassVisitor, 0)
            return classWriter.toByteArray()
        }
    }
}

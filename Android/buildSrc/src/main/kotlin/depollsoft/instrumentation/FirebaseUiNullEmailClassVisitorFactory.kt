package depollsoft.instrumentation

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * FirebaseUI 9.1.1 accepts a nullable email inside CredentialSaveActivity but
 * declares the preceding createIntent parameter non-null. Facebook users can
 * therefore authenticate successfully and crash before disabled credential
 * saving is consulted. Remove only that generated Kotlin parameter check.
 */
abstract class FirebaseUiNullEmailClassVisitorFactory : AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun isInstrumentable(classData: ClassData): Boolean =
        classData.className ==
            "com.firebase.ui.auth.ui.credentials.CredentialSaveActivity\$Companion"

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor,
    ): ClassVisitor =
        object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            private var removedEmailNullCheck = false

            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                val delegate =
                    super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name != "createIntent") return delegate

                return object : MethodVisitor(Opcodes.ASM9, delegate) {
                    private var loadedEmailParameterName = false

                    override fun visitLdcInsn(value: Any?) {
                        super.visitLdcInsn(value)
                        loadedEmailParameterName = value == "email"
                    }

                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String,
                        name: String,
                        descriptor: String,
                        isInterface: Boolean,
                    ) {
                        val isEmailNullCheck =
                            loadedEmailParameterName &&
                                owner == "kotlin/jvm/internal/Intrinsics" &&
                                name == "checkNotNullParameter"
                        if (isEmailNullCheck) {
                            removedEmailNullCheck = true
                            // Discard the email value and parameter-name string
                            // that would otherwise be consumed by Intrinsics.
                            super.visitInsn(Opcodes.POP2)
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                        }
                        loadedEmailParameterName = false
                    }
                }
            }

            override fun visitEnd() {
                check(removedEmailNullCheck) {
                    "FirebaseUI CredentialSaveActivity email null-check was not found; " +
                        "review the 9.1.1 compatibility patch before upgrading."
                }
                super.visitEnd()
            }
        }
}

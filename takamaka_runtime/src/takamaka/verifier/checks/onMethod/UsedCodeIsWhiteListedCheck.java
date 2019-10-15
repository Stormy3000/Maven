package takamaka.verifier.checks.onMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Optional;

import org.apache.bcel.Const;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import takamaka.verifier.VerifiedClassGen;
import takamaka.verifier.errors.IllegalAccessToNonWhiteListedFieldError;
import takamaka.verifier.errors.IllegalCallToNonWhiteListedConstructorError;
import takamaka.verifier.errors.IllegalCallToNonWhiteListedMethodError;

/**
 * A check that a method calls white-listed methods only and accesses white-listed fields only.
 */
public class UsedCodeIsWhiteListedCheck extends VerifiedClassGen.Verification.MethodVerification.Check {

	public UsedCodeIsWhiteListedCheck(VerifiedClassGen.Verification.MethodVerification verification) {
		verification.super();

		instructions().forEach(ih -> {
			Instruction ins = ih.getInstruction();
			if (ins instanceof FieldInstruction) {
				FieldInstruction fi = (FieldInstruction) ins;
				if (!hasWhiteListingModel(fi))
					issue(new IllegalAccessToNonWhiteListedFieldError(clazz, methodName, lineOf(ih), fi.getLoadClassType(cpg).getClassName(), fi.getFieldName(cpg)));
			}
			else if (ins instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) ins;
				if (!hasWhiteListingModel(invoke)) {
					Optional<? extends Executable> target = clazz.getClassResolver().resolvedExecutableFor(invoke);
					if (target.isPresent()) {
						Executable executable = target.get();
						if (executable instanceof Constructor<?>)
							issue(new IllegalCallToNonWhiteListedConstructorError(clazz, methodName, lineOf(ih), executable.getDeclaringClass().getName()));
						else
							issue(new IllegalCallToNonWhiteListedMethodError(clazz, methodName, lineOf(ih), executable.getDeclaringClass().getName(), executable.getName()));
					}
					else {
						// the call seems not resolvable
						ReferenceType receiverType = invoke.getReferenceType(cpg);
						String receiverClassName = receiverType instanceof ObjectType ? ((ObjectType) receiverType).getClassName() : "java.lang.Object";
						String methodName = invoke.getMethodName(cpg);

						if (invoke instanceof INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(methodName))
							issue(new IllegalCallToNonWhiteListedConstructorError(clazz, methodName, lineOf(ih), receiverClassName));
						else
							issue(new IllegalCallToNonWhiteListedMethodError(clazz, methodName, lineOf(ih), receiverClassName, methodName));
					}
				}
			}
		});
	}
}
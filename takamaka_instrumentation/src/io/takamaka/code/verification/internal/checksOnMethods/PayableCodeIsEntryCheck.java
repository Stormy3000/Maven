package io.takamaka.code.verification.internal.checksOnMethods;

import io.takamaka.code.verification.VerifiedClass;
import io.takamaka.code.verification.issues.PayableWithoutEntryError;

/**
 * A check that {@code @@Entry} is applied only to instance methods or constructors of contracts.
 */
public class PayableCodeIsEntryCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public PayableCodeIsEntryCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		if (annotations.isPayable(className, methodName, methodArgs, methodReturnType)
				&& !annotations.isEntry(className, methodName, methodArgs, methodReturnType).isPresent())
			issue(new PayableWithoutEntryError(inferSourceFile(), methodName));
	}
}
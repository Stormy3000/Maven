package io.takamaka.code.instrumentation.issues;

import org.apache.bcel.generic.ClassGen;

public class PayableWithoutAmountError extends Error {

	public PayableWithoutAmountError(ClassGen clazz, String methodName) {
		super(clazz, methodName, -1, "a @Payable method must have a first argument for the payed amount, of type int, long or BigInteger");
	}
}
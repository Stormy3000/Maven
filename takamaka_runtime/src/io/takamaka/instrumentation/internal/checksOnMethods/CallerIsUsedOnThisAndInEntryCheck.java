package io.takamaka.instrumentation.internal.checksOnMethods;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

import io.takamaka.instrumentation.internal.VerifiedClass;
import io.takamaka.instrumentation.issues.CallerNotOnThisError;
import io.takamaka.instrumentation.issues.CallerOutsideEntryError;
import takamaka.blockchain.types.ClassType;

/**
 * A check that {@code caller()} is only used with {@code this} as receiver
 * and inside an {@code @@Entry} method or constructor.
 */
public class CallerIsUsedOnThisAndInEntryCheck extends VerifiedClass.ClassVerification.MethodVerification.Check {

	public CallerIsUsedOnThisAndInEntryCheck(VerifiedClass.ClassVerification.MethodVerification verification) {
		verification.super();

		boolean isEntry = clazz.annotations.isEntry(className, methodName, methodArgs, methodReturnType).isPresent();

		instructions()
			.filter(this::isCallToContractCaller)
			.forEach(ih -> {
				if (!isEntry)
					issue(new CallerOutsideEntryError(clazz, methodName, lineOf(ih)));

				if (!previousIsLoad0(ih))
					issue(new CallerNotOnThisError(clazz, methodName, lineOf(ih)));
			});
	}

	private boolean previousIsLoad0(InstructionHandle ih) {
		// we skip NOPs
		for (ih = ih.getPrev(); ih != null && ih.getInstruction() instanceof NOP; ih = ih.getPrev());

		Instruction ins;
		return ih != null && (ins = ih.getInstruction()) instanceof LoadInstruction && ((LoadInstruction) ins).getIndex() == 0;
	}

	/**
	 * The Java bytecode types of the {@code caller()} method of {@link io.takamaka.lang.Contract}.
	 */
	private final static String TAKAMAKA_CALLER_SIG = "()L" + ClassType.CONTRACT.name.replace('.', '/') + ";";

	private boolean isCallToContractCaller(InstructionHandle ih) {
		Instruction ins = ih.getInstruction();
		if (ins instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) ins;
			ReferenceType receiver;
	
			return "caller".equals(invoke.getMethodName(cpg))
				&& TAKAMAKA_CALLER_SIG.equals(invoke.getSignature(cpg))
				&& (receiver = invoke.getReferenceType(cpg)) instanceof ObjectType
				&& classLoader.isContract(((ObjectType) receiver).getClassName());
		}
		else
			return false;
	}
}
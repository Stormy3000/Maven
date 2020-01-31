package io.takamaka.code.engine.internal.transactions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.IllegalTransactionRequestException;
import io.takamaka.code.engine.internal.EngineClassLoaderImpl;

public class ConstructorCallTransactionRun extends CodeCallTransactionRun<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {
	public final CodeSignature constructor;

	public ConstructorCallTransactionRun(ConstructorCallTransactionRequest request, TransactionReference current, Node node) throws TransactionException, IllegalTransactionRequestException {
		super(request, current, node);

		this.constructor = request.constructor;

		try (EngineClassLoaderImpl classLoader = new EngineClassLoaderImpl(request.classpath, this)) {
			this.classLoader = classLoader;
			this.response = computeResponse();
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}
	}

	private ConstructorCallTransactionResponse computeResponse() throws Exception {
		try {
			this.deserializedCaller = deserializer.deserialize(request.caller);
			this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
			checkIsExternallyOwned(deserializedCaller);
			// we sell all gas first: what remains will be paid back at the end;
			// if the caller has not enough to pay for the whole gas, the transaction won't be executed
			this.balanceUpdateInCaseOfFailure = checkMinimalGas(request, deserializedCaller);
			chargeForCPU(node.getGasCostModel().cpuBaseTransactionCost());
			chargeForStorage(sizeCalculator.sizeOf(request));
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			throw wrapAsTransactionException(t, "cannot complete the transaction");
		}

		try {
			Thread executor = new Thread(this::run);
			executor.start();
			executor.join();

			if (exception instanceof InvocationTargetException) {
				ConstructorCallTransactionResponse response = new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
				chargeForStorage(sizeCalculator.sizeOf(response));
				increaseBalance(deserializedCaller);
				return new ConstructorCallTransactionExceptionResponse((Exception) exception.getCause(), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			}

			if (exception != null)
				throw exception;

			ConstructorCallTransactionResponse response = new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
			chargeForStorage(sizeCalculator.sizeOf(response));
			increaseBalance(deserializedCaller);
			return new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(), events(), gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
		}
		catch (IllegalTransactionRequestException e) {
			throw e;
		}
		catch (Throwable t) {
			// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
			BigInteger gasConsumedForPenalty = request.gas.subtract(gasConsumedForCPU).subtract(gasConsumedForRAM).subtract(gasConsumedForStorage);
			return new ConstructorCallTransactionFailedResponse(wrapAsTransactionException(t, "Failed transaction"), balanceUpdateInCaseOfFailure, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage, gasConsumedForPenalty);
		}
	}

	private void run() {
		try {
			Constructor<?> constructorJVM;
			Object[] deserializedActuals;

			try {
				// we first try to call the constructor with exactly the parameter types explicitly provided
				constructorJVM = getConstructor();
				deserializedActuals = this.deserializedActuals;
			}
			catch (NoSuchMethodException e) {
				// if not found, we try to add the trailing types that characterize the @Entry constructors
				try {
					constructorJVM = getEntryConstructor();
					deserializedActuals = addExtraActualsForEntry();
				}
				catch (NoSuchMethodException ee) {
					throw e; // the message must be relative to the constructor as the user sees it
				}
			}

			ensureWhiteListingOf(constructorJVM, deserializedActuals);
			if (hasAnnotation(constructorJVM, Constants.RED_PAYABLE_NAME))
				checkIsExternallyOwned(deserializedCaller);

			try {
				result = constructorJVM.newInstance(deserializedActuals);
			}
			catch (InvocationTargetException e) {
				exception = unwrapInvocationException(e, constructorJVM);
			}
		}
		catch (Throwable t) {
			exception = t;
		}
	}

	@Override
	public final CodeSignature getMethodOrConstructor() {
		return constructor;
	}
}
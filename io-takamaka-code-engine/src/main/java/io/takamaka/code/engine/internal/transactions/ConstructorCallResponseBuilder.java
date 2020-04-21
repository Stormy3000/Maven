package io.takamaka.code.engine.internal.transactions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.AbstractNode;

/**
 * The creator of a response for a transaction that executes a constructor of Takamaka code.
 */
public class ConstructorCallResponseBuilder extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse> {

	/**
	 * Creates the builder of the response.
	 * 
	 * @param request the request of the transaction
	 * @param node the node that is running the transaction
	 * @throws TransactionRejectedException if the builder cannot be created
	 */
	public ConstructorCallResponseBuilder(ConstructorCallTransactionRequest request, AbstractNode node) throws TransactionRejectedException {
		super(request, node);
	}

	@Override
	protected final BigInteger gasForStoringFailedResponse() {
		BigInteger gas = request.gasLimit;

		return sizeCalculator.sizeOfResponse(new ConstructorCallTransactionFailedResponse
			("placeholder for the name of the exception", "placeholder for the message of the exception", "placeholder for where",
			Stream.empty(), gas, gas, gas, gas));
	}

	@Override
	public ConstructorCallTransactionResponse build(TransactionReference current) throws TransactionRejectedException {
		return new ResponseCreator(current).create();
	}

	private class ResponseCreator extends CodeCallResponseBuilder<ConstructorCallTransactionRequest, ConstructorCallTransactionResponse>.ResponseCreator {

		/**
		 * The deserialized actual arguments of the constructor.
		 */
		private Object[] deserializedActuals;

		private ResponseCreator(TransactionReference current) throws TransactionRejectedException {
			super(current);
		}

		@Override
		protected ConstructorCallTransactionResponse body() {
			try {
				this.deserializedActuals = request.actuals().map(deserializer::deserialize).toArray(Object[]::new);
		
				formalsAndActualsMustMatch();
				Object[] deserializedActuals;
				Constructor<?> constructorJVM;
		
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
					callerMustBeRedGreenExternallyOwnedAccount();
		
				//ConstructorThread thread = new ConstructorThread(constructorJVM, deserializedActuals);
				Object result;
				try {
					result = constructorJVM.newInstance(deserializedActuals);
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (isCheckedForThrowsExceptions(cause, constructorJVM)) {
						chargeGasForStorageOf(new ConstructorCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
						payBackAllRemainingGasToCaller();
						return new ConstructorCallTransactionExceptionResponse(cause.getClass().getName(), cause.getMessage(), where(cause), updates(), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
					}
					else
						throw cause;
				}
		
				chargeGasForStorageOf(new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage()));
				payBackAllRemainingGasToCaller();
				return new ConstructorCallTransactionSuccessfulResponse
					((StorageReference) serializer.serialize(result), updates(result), storageReferencesOfEvents(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage());
			}
			catch (Throwable t) {
				// we do not pay back the gas: the only update resulting from the transaction is one that withdraws all gas from the balance of the caller
				return new ConstructorCallTransactionFailedResponse(t.getClass().getName(), t.getMessage(), where(t), updatesToBalanceOrNonceOfCaller(), gasConsumedForCPU(), gasConsumedForRAM(), gasConsumedForStorage(), gasConsumedForPenalty());
			}
		}

		/**
		 * Resolves the constructor that must be called.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClass();

			return classLoader.resolveConstructor(request.constructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(request.constructor.toString()));
		}

		/**
		 * Resolves the constructor that must be called, assuming that it is an entry.
		 * 
		 * @return the constructor
		 * @throws NoSuchMethodException if the constructor could not be found
		 * @throws SecurityException if the constructor could not be accessed
		 * @throws ClassNotFoundException if the class of the constructor or of some parameter cannot be found
		 */
		private Constructor<?> getEntryConstructor() throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] argTypes = formalsAsClassForEntry();

			return classLoader.resolveConstructor(request.constructor.definingClass.name, argTypes)
				.orElseThrow(() -> new NoSuchMethodException(request.constructor.toString()));
		}

		/**
		 * Adds to the actual parameters the implicit actuals that are passed
		 * to {@link io.takamaka.code.lang.Entry} methods or constructors. They are the caller of
		 * the entry and {@code null} for the dummy argument.
		 * 
		 * @return the resulting actual parameters
		 */
		private Object[] addExtraActualsForEntry() {
			int al = deserializedActuals.length;
			Object[] result = new Object[al + 2];
			System.arraycopy(deserializedActuals, 0, result, 0, al);
			result[al] = getDeserializedCaller();
			result[al + 1] = null; // Dummy is not used
		
			return result;
		}

		/**
		 * Checks that the constructor called by this transaction is
		 * white-listed and its white-listing proof-obligations hold.
		 * 
		 * @param executable the constructor
		 * @param actuals the actual arguments passed to {@code executable}
		 * @throws ClassNotFoundException if some class could not be found during the check
		 */
		private void ensureWhiteListingOf(Constructor<?> executable, Object[] actuals) throws ClassNotFoundException {
			Optional<Constructor<?>> model = classLoader.getWhiteListingWizard().whiteListingModelOf((Constructor<?>) executable);
			if (!model.isPresent())
				throw new NonWhiteListedCallException("illegal call to non-white-listed constructor of " + request.constructor.definingClass.name);

			Annotation[][] anns = model.get().getParameterAnnotations();
			for (int pos = 0; pos < anns.length; pos++)
				checkWhiteListingProofObligations(model.get().getName(), actuals[pos], anns[pos]);
		}

		@Override
		protected final Stream<Object> getDeserializedActuals() {
			return Stream.of(deserializedActuals);
		}
	}
}
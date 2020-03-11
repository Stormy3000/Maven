package io.takamaka.code.engine.internal;

import java.math.BigInteger;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.responses.ConstructorCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.beans.responses.ConstructorCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.MethodCallTransactionExceptionResponse;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.beans.responses.MethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseFailed;
import io.hotmoka.beans.responses.TransactionResponseWithEvents;
import io.hotmoka.beans.responses.TransactionResponseWithGas;
import io.hotmoka.beans.responses.TransactionResponseWithUpdates;
import io.hotmoka.beans.responses.VoidMethodCallTransactionSuccessfulResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.updates.AbstractUpdateOfField;
import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.updates.UpdateOfBalance;
import io.hotmoka.beans.updates.UpdateOfBigInteger;
import io.hotmoka.beans.updates.UpdateOfBoolean;
import io.hotmoka.beans.updates.UpdateOfByte;
import io.hotmoka.beans.updates.UpdateOfChar;
import io.hotmoka.beans.updates.UpdateOfDouble;
import io.hotmoka.beans.updates.UpdateOfEnumEager;
import io.hotmoka.beans.updates.UpdateOfEnumLazy;
import io.hotmoka.beans.updates.UpdateOfFloat;
import io.hotmoka.beans.updates.UpdateOfInt;
import io.hotmoka.beans.updates.UpdateOfLong;
import io.hotmoka.beans.updates.UpdateOfRedBalance;
import io.hotmoka.beans.updates.UpdateOfShort;
import io.hotmoka.beans.updates.UpdateOfStorage;
import io.hotmoka.beans.updates.UpdateOfString;
import io.hotmoka.beans.updates.UpdateToNullEager;
import io.hotmoka.beans.updates.UpdateToNullLazy;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.CharValue;
import io.hotmoka.beans.values.DoubleValue;
import io.hotmoka.beans.values.EnumValue;
import io.hotmoka.beans.values.FloatValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.ShortValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.nodes.GasCostModel;
import io.takamaka.code.engine.internal.transactions.AbstractTransactionBuilder;

/**
 * An object that knows about the size of objects when stored in the node's store.
 */
public class SizeCalculator {

	/**
	 * The builder of the transaction for which this calculator works.
	 */
	private final AbstractTransactionBuilder<?,?> builder;

	/**
	 * Builds the size calculator.
	 * 
	 * @param builder the builder of the transaction for which this calculator works
	 */
	public SizeCalculator(AbstractTransactionBuilder<?,?> builder) {
		this.builder = builder;
	}

	/**
	 * Yields the size of the given request, in terms of storage gas units consumed in store.
	 * 
	 * @param request the request
	 * @return the size
	 */
	public BigInteger sizeOfRequest(NonInitialTransactionRequest<?> request) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		if (request instanceof ConstructorCallTransactionRequest)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOfValue(request.caller))
				.add(gasCostModel.storageCostOf(request.gasLimit)).add(sizeOfClasspath(request.classpath))
				.add(((ConstructorCallTransactionRequest) request).actuals().map(this::sizeOfValue).reduce(BigInteger.ZERO, BigInteger::add));
		else if (request instanceof InstanceMethodCallTransactionRequest) {
			InstanceMethodCallTransactionRequest instanceMethodCallTransactionRequest = (InstanceMethodCallTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOfValue(request.caller))
				.add(gasCostModel.storageCostOf(request.gasLimit)).add(sizeOfClasspath(request.classpath))
				.add(sizeOfCodeSignature(instanceMethodCallTransactionRequest.method))
				.add(sizeOfValue(instanceMethodCallTransactionRequest.receiver))
				.add(instanceMethodCallTransactionRequest.actuals().map(this::sizeOfValue).reduce(BigInteger.ZERO, BigInteger::add));
		}
		else if (request instanceof StaticMethodCallTransactionRequest) {
			StaticMethodCallTransactionRequest staticMethodCallTransactionRequest = (StaticMethodCallTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOfValue(request.caller))
				.add(gasCostModel.storageCostOf(request.gasLimit)).add(sizeOfClasspath(request.classpath))
				.add(sizeOfCodeSignature(staticMethodCallTransactionRequest.method))
				.add(staticMethodCallTransactionRequest.actuals().map(this::sizeOfValue).reduce(BigInteger.ZERO, BigInteger::add));
		}
		else if (request instanceof JarStoreTransactionRequest) {
			JarStoreTransactionRequest jarStoreTransactionRequest = (JarStoreTransactionRequest) request;
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot() * 2L)
				.add(sizeOfValue(request.caller)).add(gasCostModel.storageCostOf(request.gasLimit)).add(sizeOfClasspath(request.classpath))
				.add(jarStoreTransactionRequest.getDependencies().map(this::sizeOfClasspath).reduce(BigInteger.ZERO, BigInteger::add))
				.add(gasCostModel.storageCostOfJar(jarStoreTransactionRequest.getJarLength()));
		}
		else
			throw new IllegalArgumentException("unexpected transaction request");
	}

	/**
	 * Yields the size of the given response, in terms of storage gas units consumed in store.
	 * 
	 * @param response the response
	 * @return the size
	 */
	public BigInteger sizeOfResponse(NonInitialTransactionResponse response) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot());

		if (response instanceof TransactionResponseWithGas) {
			TransactionResponseWithGas responseAsWithGas = (TransactionResponseWithGas) response;

			size = size.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForCPU()))
				.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForRAM()))
				.add(gasCostModel.storageCostOf(responseAsWithGas.gasConsumedForStorage()));
		}

		if (response instanceof TransactionResponseWithUpdates)
			size = size.add(((TransactionResponseWithUpdates) response).getUpdates().map(this::sizeOfUpdate).reduce(BigInteger.ZERO, BigInteger::add));

		if (response instanceof TransactionResponseFailed)
			size = size.add(gasCostModel.storageCostOf(((TransactionResponseFailed) response).gasConsumedForPenalty()));

		if (response instanceof TransactionResponseWithEvents)
			size = size.add(((TransactionResponseWithEvents) response).getEvents().map(this::sizeOfValue).reduce(BigInteger.ZERO, BigInteger::add));

		if (response instanceof ConstructorCallTransactionSuccessfulResponse)
			return size.add(sizeOfValue(((ConstructorCallTransactionSuccessfulResponse) response).newObject));
		else if (response instanceof MethodCallTransactionSuccessfulResponse)
			return size.add(sizeOfValue(((MethodCallTransactionSuccessfulResponse) response).result));
		else if (response instanceof JarStoreTransactionSuccessfulResponse)
			return size.add(gasCostModel.storageCostOfJar(((JarStoreTransactionSuccessfulResponse) response).getInstrumentedJarLength()));
		else if (response instanceof ConstructorCallTransactionExceptionResponse) {
			ConstructorCallTransactionExceptionResponse ccter = (ConstructorCallTransactionExceptionResponse) response;
			size = size.add(gasCostModel.storageCostOf(ccter.classNameOfCause));
			if (ccter.messageOfCause != null)
				size = size.add(gasCostModel.storageCostOf(ccter.messageOfCause));

			return size;
		}
		else if (response instanceof MethodCallTransactionExceptionResponse) {
			MethodCallTransactionExceptionResponse mcter = (MethodCallTransactionExceptionResponse) response;
			size = size.add(gasCostModel.storageCostOf(mcter.classNameOfCause));
			if (mcter.messageOfCause != null)
				size = size.add(gasCostModel.storageCostOf(mcter.messageOfCause));

			return size;
		}
		else if (response instanceof ConstructorCallTransactionFailedResponse ||
				response instanceof JarStoreTransactionFailedResponse ||
				response instanceof MethodCallTransactionFailedResponse ||
				response instanceof VoidMethodCallTransactionSuccessfulResponse)
			return size;
		else
			throw new IllegalArgumentException("unexpected transaction response");
	}

	/**
	 * Yields the size of the given classpath, in terms of storage consumed in store.
	 * 
	 * @param classpath the classpath
	 * @return the size
	 */
	public BigInteger sizeOfClasspath(Classpath classpath) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()))
			.add(gasCostModel.storageCostOf(classpath.transaction));
	}

	/**
	 * Yields the size of the given field, in terms of storage consumed in store.
	 * 
	 * @param field the field
	 * @return the size
	 */
	public BigInteger sizeOfFieldSignature(FieldSignature field) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		return BigInteger.valueOf(gasCostModel.storageCostPerSlot()).add(sizeOfType(field.definingClass))
			.add(gasCostModel.storageCostOf(field.name)).add(sizeOfType(field.type));
	}

	/**
	 * Yields the size of the given method or constructor, in terms of storage gas units consumed in store.
	 * 
	 * @param methodOrConstructor the method or constructor
	 * @return the size
	 */
	public BigInteger sizeOfCodeSignature(CodeSignature methodOrConstructor) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot())
			.add(sizeOfType(methodOrConstructor.definingClass))
			.add(methodOrConstructor.formals().map(this::sizeOfType).reduce(BigInteger.ZERO, BigInteger::add));

		if (methodOrConstructor instanceof ConstructorSignature)
			return size;
		else if (methodOrConstructor instanceof VoidMethodSignature)
			return size.add(gasCostModel.storageCostOf(((VoidMethodSignature) methodOrConstructor).methodName));
		else if (methodOrConstructor instanceof NonVoidMethodSignature) {
			NonVoidMethodSignature method = (NonVoidMethodSignature) methodOrConstructor;
			return size.add(gasCostModel.storageCostOf(method.methodName)).add(sizeOfType(method.returnType));
		}
		else
			throw new IllegalArgumentException("unexpected code signature");
	}

	/**
	 * Yields the size of the given type, in terms of storage gas units consumed in store.
	 * 
	 * @param type the type
	 * @return the size
	 */
	public BigInteger sizeOfType(StorageType type) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		if (type instanceof BasicTypes)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot());
		else if (type instanceof ClassType)
			return BigInteger.valueOf(gasCostModel.storageCostPerSlot())
				.add(gasCostModel.storageCostOf(((ClassType) type).name));
		else
			throw new IllegalArgumentException("unexpected storage type");
	}


	/**
	 * Yields the size of the given value, in terms of storage gas units consumed in store.
	 * 
	 * @param value the value
	 * @return the size
	 */
	public BigInteger sizeOfValue(StorageValue value) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		BigInteger size = BigInteger.valueOf(gasCostModel.storageCostPerSlot());
		if (value instanceof BigIntegerValue)
			return size.add(gasCostModel.storageCostOf(((BigIntegerValue) value).value));
		else if (value instanceof StringValue)
			return size.add(gasCostModel.storageCostOf(((StringValue) value).value));
		else if (value instanceof StorageReference) {
			StorageReference storageReference = (StorageReference) value;
			return size.add(gasCostModel.storageCostOf(storageReference.progressive))
				.add(gasCostModel.storageCostOf(storageReference.transaction));
		}
		else if (value instanceof EnumValue) {
			EnumValue enumValue = (EnumValue) value;
			return size.add(gasCostModel.storageCostOf(enumValue.enumClassName))
					.add(gasCostModel.storageCostOf(enumValue.name));
		}
		else if (value instanceof BooleanValue || value instanceof IntValue || value instanceof CharValue
				|| value instanceof DoubleValue || value instanceof FloatValue || value instanceof NullValue
				|| value instanceof LongValue || value instanceof ByteValue || value instanceof ShortValue)
			return size;
		else
			throw new IllegalArgumentException("unexpected storage value");
	}

	/**
	 * Yields the size of the given update, in terms of storage gas units consumed in store.
	 * 
	 * @param update the update
	 * @return the size
	 */
	public BigInteger sizeOfUpdate(Update update) {
		GasCostModel gasCostModel = builder.node.getGasCostModel();

		BigInteger size = sizeOfValue(update.object);

		if (update instanceof ClassTag) {
			ClassTag ct = (ClassTag) update;
			return size.add(gasCostModel.storageCostOf(ct.className)).add(gasCostModel.storageCostOf(ct.jar));
		}
		else if (update instanceof UpdateOfBalance)
			return size.add(gasCostModel.storageCostOf(((UpdateOfBalance) update).balance));
		else if (update instanceof UpdateOfRedBalance)
			return size.add(gasCostModel.storageCostOf(((UpdateOfRedBalance) update).balanceRed));

		size = size.add(sizeOfFieldSignature(((AbstractUpdateOfField) update).field));
		if (update instanceof UpdateOfBigInteger)
			return size.add(gasCostModel.storageCostOf(((UpdateOfBigInteger) update).value));
		else if (update instanceof UpdateOfBoolean || update instanceof UpdateOfByte ||
				update instanceof UpdateOfChar || update instanceof UpdateOfDouble ||
				update instanceof UpdateOfFloat || update instanceof UpdateOfInt ||
				update instanceof UpdateOfLong || update instanceof UpdateOfShort)
			return size.add(BigInteger.valueOf(gasCostModel.storageCostPerSlot()));
		else if (update instanceof UpdateToNullEager || update instanceof UpdateToNullLazy)
			return size;
		else if (update instanceof UpdateOfEnumEager) {
			UpdateOfEnumEager ee = (UpdateOfEnumEager) update;
			return size.add(gasCostModel.storageCostOf(ee.enumClassName)).add(gasCostModel.storageCostOf(ee.name));
		}
		else if (update instanceof UpdateOfEnumLazy) {
			UpdateOfEnumLazy el = (UpdateOfEnumLazy) update;
			return size.add(gasCostModel.storageCostOf(el.enumClassName)).add(gasCostModel.storageCostOf(el.name));
		}
		else if (update instanceof UpdateOfString)
			return size.add(gasCostModel.storageCostOf(((UpdateOfString) update).value));
		else if (update instanceof UpdateOfStorage)
			return size.add(sizeOfValue(((UpdateOfStorage) update).value));
		else
			throw new IllegalArgumentException("unexpected update");
	}
}
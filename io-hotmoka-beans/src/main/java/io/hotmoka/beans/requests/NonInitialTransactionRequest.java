package io.hotmoka.beans.requests;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.responses.NonInitialTransactionResponse;
import io.hotmoka.beans.values.StorageReference;

@Immutable
public abstract class NonInitialTransactionRequest<R extends NonInitialTransactionResponse> implements TransactionRequest<R> {
	private static final long serialVersionUID = 8584281399116101538L;

	/**
	 * The externally owned caller contract that pays for the transaction.
	 */
	public final StorageReference caller;

	/**
	 * The gas provided to the transaction.
	 */
	public final BigInteger gasLimit;

	/**
	 * The coins payed for each unit of gas consumed by the transaction.
	 */
	public final BigInteger gasPrice;

	/**
	 * The class path that specifies where the {@code caller} should be interpreted.
	 */
	public final Classpath classpath;

	/**
	 * Builds the transaction request.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gasLimit the maximal amount of gas that can be consumed by the transaction
	 * @param gasPrice the coins payed for each unit of gas consumed by the transaction
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 */
	protected NonInitialTransactionRequest(StorageReference caller, BigInteger gasLimit, BigInteger gasPrice, Classpath classpath) {
		this.caller = caller;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.classpath = classpath;
	}
}
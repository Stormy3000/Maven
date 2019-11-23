package io.takamaka.code.blockchain;

/**
 * A transaction reference for a blockchain where transactions are executed
 * immediately and stacked sequentially.
 */
public abstract class SequentialTransactionReference extends TransactionReference {

	private static final long serialVersionUID = 367515181596412034L;

	/**
	 * Yields the reference to the transaction that precedes this one.
	 * 
	 * @return the previous transaction reference, if any. Yields {@code null} if this
	 *         refers to the first transaction in blockchain
	 */
	public abstract SequentialTransactionReference getPrevious();
}
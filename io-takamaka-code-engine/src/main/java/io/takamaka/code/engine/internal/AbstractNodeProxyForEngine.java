package io.takamaka.code.engine.internal;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.internal.transactions.AbstractNodeProxyForTransactions;
import io.takamaka.code.instrumentation.GasCostModel;

/**
 * The methods of an abstract node that are only used inside this package.
 * By using this proxy class, we avoid to define them as public.
 */
public abstract class AbstractNodeProxyForEngine extends AbstractNodeProxyForTransactions {

	/**
	 * The default gas model of the node.
	 */
	private final static GasCostModel defaultGasCostModel = GasCostModel.standard();

	/**
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. It is guaranteed that the transaction has been
	 * already successfully delivered. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @return the response
	 * @throws IllegalArgumentException if the transaction does not exist in the store, or
	 *                                  did not generate a response with instrumented jar
	 */
	protected abstract TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarUncommittedAt(TransactionReference reference) throws IllegalArgumentException;

	/**
	 * Yields the last updates to the fields of the given object.
	 * 
	 * @param object the reference to the object
	 * @param onlyEager true if and only if only the fields of eager type are required
	 * @param chargeGasForCPU what to apply to charge gas for CPU usage
	 * @return the updates
	 */
	protected abstract Stream<Update> getLastUpdates(StorageReference object, boolean onlyEager, EngineClassLoader classLoader, Consumer<BigInteger> chargeGasForCPU);

	@Override
	protected final EngineClassLoader mkClassLoader(TransactionReference classpath) throws Exception {
		return new EngineClassLoader(classpath, this);
	}

	/**
	 * Yields the gas cost model of this node.
	 * 
	 * @return the default gas cost model. Subclasses may redefine
	 */
	public GasCostModel getGasCostModel() {
		return defaultGasCostModel;
	}
}
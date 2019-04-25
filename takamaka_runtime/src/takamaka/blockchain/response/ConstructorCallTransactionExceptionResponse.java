package takamaka.blockchain.response;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import takamaka.blockchain.Update;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.Immutable;

/**
 * A response for a successful transaction that calls a constructor of a storage
 * class in blockchain. The constructor is annotated as {@link takamaka.lang.ThrowsExceptions}.
 * It has been called without problems but it threw an instance of {@link java.lang.Exception}.
 */
@Immutable
public class ConstructorCallTransactionExceptionResponse extends ConstructorCallTransactionResponse implements AbstractTransactionResponseWithEvents {

	private static final long serialVersionUID = -1571448149485752630L;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * The exception that has been thrown by the constructor.
	 */
	public final transient Exception exception;

	/**
	 * Builds the transaction response.
	 * 
	 * @param exception the exception that has been thrown by the constructor
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param consumedGas the amount of gas consumed by the transaction
	 */
	public ConstructorCallTransactionExceptionResponse(Exception exception, Stream<Update> updates, Stream<StorageReference> events, BigInteger consumedGas) {
		super(updates, consumedGas);

		this.events = events.toArray(StorageReference[]::new);
		this.exception = exception;
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public String toString() {
        return super.toString() + "\n"
        	+ "  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}
}
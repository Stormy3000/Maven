package io.hotmoka.beans.requests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.GameteCreationTransactionResponse;

/**
 * A request for creating an initial gamete.
 */
@Immutable
public class GameteCreationTransactionRequest extends InitialTransactionRequest<GameteCreationTransactionResponse> {
	final static byte SELECTOR = 0;

	/**
	 * The reference to the jar containing the basic Takamaka classes. This must
	 * have been already installed in the node.
	 */
	public final TransactionReference classpath;

	/**
	 * The amount of coin provided to the gamete.
	 */

	public final BigInteger initialAmount;

	/**
	 * The Base64-encoded public key that will be assigned to the gamete.
	 */
	public final String publicKey;

	/**
	 * Builds the transaction request.
	 * 
	 * @param classpath the reference to the jar containing the basic Takamaka classes. This must
	 *                  have been already installed by a previous transaction
	 * @param initialAmount the amount of coin provided to the gamete
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public GameteCreationTransactionRequest(TransactionReference classpath, BigInteger initialAmount, String publicKey) {
		if (classpath == null)
			throw new IllegalArgumentException("classpath cannot be null");

		if (initialAmount == null)
			throw new IllegalArgumentException("initialAmount cannot be null");

		if (initialAmount.signum() < 0)
			throw new IllegalArgumentException("initialAmount must be non-negative");

		if (publicKey == null)
			throw new IllegalArgumentException("publicKey cannot be null");

		this.classpath = classpath;
		this.initialAmount = initialAmount;
		this.publicKey = publicKey;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName() + ":\n"
        	+ "  class path: " + classpath + "\n"
        	+ "  initialAmount: " + initialAmount + "\n"
        	+ "  publicKey: " + publicKey;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GameteCreationTransactionRequest) {
			GameteCreationTransactionRequest otherCast = (GameteCreationTransactionRequest) other;
			return classpath.equals(otherCast.classpath) && initialAmount.equals(otherCast.initialAmount) && publicKey.equals(otherCast.publicKey);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return classpath.hashCode() ^ initialAmount.hashCode() ^ publicKey.hashCode();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.oos.writeByte(SELECTOR);
		classpath.into(context);
		marshal(initialAmount, context);
		context.oos.writeUTF(publicKey);
	}

	/**
	 * Factory method that unmarshals a request from the given stream.
	 * The selector has been already unmarshalled.
	 * 
	 * @param ois the stream
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static GameteCreationTransactionRequest from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		TransactionReference classpath = TransactionReference.from(ois);
		BigInteger initialAmount = unmarshallBigInteger(ois);
		String publicKey = ois.readUTF();

		return new GameteCreationTransactionRequest(classpath, initialAmount, publicKey);
	}
}
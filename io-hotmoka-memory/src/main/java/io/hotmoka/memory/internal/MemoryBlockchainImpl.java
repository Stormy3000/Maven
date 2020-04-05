package io.hotmoka.memory.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining.
 * Updates are stored inside the blocks, rather than in an external database.
 * It provides support for the creation of a given number of initial accounts.
 */
public class MemoryBlockchainImpl extends AbstractMemoryBlockchain implements MemoryBlockchain {

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * Builds a blockchain in disk memory and initializes user accounts with the given initial funds.
	 * 
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param funds the initial funds of the accounts that are created
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public MemoryBlockchainImpl(Path takamakaCodePath, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		super(takamakaCodePath);

		// we compute the total amount of funds needed to create the accounts
		BigInteger sum = Stream.of(funds).reduce(BigInteger.ZERO, BigInteger::add);
		StorageReference gamete = addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaCode(), sum));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length];
		ConstructorSignature constructor = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);
		BigInteger gas = BigInteger.valueOf(10_000); // enough for creating an account
		BigInteger nonce = BigInteger.ZERO;
		for (int i = 0; i < accounts.length; i++, nonce = nonce.add(BigInteger.ONE))
			this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(), constructor, new BigIntegerValue(funds[i])));
	}

	/**
	 * Builds a blockchain in disk memory and initializes red/green user accounts with the given initial funds.
	 * 
	 * @param takamakaCodePath the path where the base Takamaka classes can be found. They will be
	 *                         installed in blockchain and will be available later as {@link io.hotmoka.memory.MemoryBlockchain#takamakaCode}
	 * @param redGreen unused; only meant to distinguish the signature of this constructor from that of the previous one
	 * @param funds the initial funds of the accounts that are created; they must be understood in pairs, each pair for the green/red
	 *              initial funds of each account (green before red)
	 * @throws IOException if a disk error occurs
	 * @throws TransactionException if some transaction for initialization fails
	 * @throws CodeExecutionException if some transaction for initialization throws an exception
	 */
	public MemoryBlockchainImpl(Path takamakaCodePath, boolean redGreen, BigInteger... funds) throws IOException, TransactionException, CodeExecutionException {
		super(takamakaCodePath);

		// we compute the total amount of funds needed to create the accounts
		BigInteger green = BigInteger.ZERO;
		for (int pos = 0; pos < funds.length; pos += 2)
			green = green.add(funds[pos]);

		BigInteger red = BigInteger.ZERO;
		for (int pos = 1; pos < funds.length; pos += 2)
			red = red.add(funds[pos]);

		StorageReference gamete = addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCode(), green, red));

		// let us create the accounts
		this.accounts = new StorageReference[funds.length / 2];
		BigInteger gas = BigInteger.valueOf(10000); // enough for creating an account
		BigInteger nonce = BigInteger.ZERO;
		for (int i = 0; i < accounts.length; i++) {
			// the constructor provides the green coins
			this.accounts[i] = addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(), new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER), new BigIntegerValue(funds[i * 2])));
			nonce = nonce.add(BigInteger.ONE);

			// then we add the red coins
			addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(gamete, nonce, gas, BigInteger.ZERO, takamakaCode(),
				new VoidMethodSignature(ClassType.RGPAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER),
				this.accounts[i], new BigIntegerValue(funds[i * 2 + 1])));

			nonce = nonce.add(BigInteger.ONE);
		}
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}
}
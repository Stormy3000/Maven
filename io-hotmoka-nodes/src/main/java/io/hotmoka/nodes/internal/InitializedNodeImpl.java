package io.hotmoka.nodes.internal;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.nodes.Node;
import io.takamaka.code.constants.Constants;

/**
 * A decorator of a node, that installs a jar and creates some initial accounts in it.
 * It is mainly useful for testing.
 */
public class InitializedNodeImpl implements InitializedNode {

	/**
	 * The node that is decorated.
	 */
	protected final Node parent;

	/**
	 * The classpath of a user jar that has been installed, if any.
	 */
	public final Classpath jar;

	/**
	 * The accounts created during initialization.
	 */
	private final StorageReference[] accounts;

	/**
	 * The method of red/green contracts to send red coins.
	 */
	private final static VoidMethodSignature RECEIVE_RED = new VoidMethodSignature(ClassType.RGPAYABLE_CONTRACT, "receiveRed", ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account.
	 */
	private final static ConstructorSignature TEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TEOA, ClassType.BIG_INTEGER);

	/**
	 * The constructor of an externally owned account with red/green funds.
	 */
	private final static ConstructorSignature TRGEOA_CONSTRUCTOR = new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER);

	/**
	 * Creates a decorated node by storing into it a jar and creating initial accounts.
	 * 
	 * @param parent the node that gets decorated
	 * @param payer the payer of the initialization transactions; if red/green accounts are being created,
	 *              then this must be a red/green externally owned account; otherwise, it can also be
	 *              a normal externally owned account
	 * @param jar the path of a jar that must be further installed in blockchain. This might be {@code null}
	 * @param redGreen true if red/green accounts must be created; if false, normal externally owned accounts are created
	 * @param funds the initial funds of the accounts that are created; if {@code redGreen} is true,
	 *              they must be understood in pairs, each pair for the green/red initial funds of each account (red before green)
	 * @throws TransactionRejectedException if some transaction that installs the jar or creates the accounts is rejected
	 * @throws TransactionException if some transaction that installs the jar or creates the accounts fails
	 * @throws CodeExecutionException if some transaction that installs the jar or creates the accounts throws an exception
	 * @throws IOException if the jar file cannot be accessed
	 */
	public InitializedNodeImpl(Node parent, StorageReference payer, Path jar, boolean redGreen, BigInteger... funds) throws TransactionRejectedException, TransactionException, CodeExecutionException, IOException {
		this.parent = parent;

		Classpath takamakaCode = takamakaCode();
		
		BigInteger nonce = ((BigIntegerValue) runViewInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(payer, BigInteger.ZERO, BigInteger.valueOf(10_000), BigInteger.ZERO, takamakaCode, new NonVoidMethodSignature(Constants.ACCOUNT_NAME, "nonce", ClassType.BIG_INTEGER), payer))).value;

		JarSupplier jarSupplier;
		if (jar != null) {
			jarSupplier = postJarStoreTransaction(new JarStoreTransactionRequest(payer, nonce, BigInteger.valueOf(1_000_000), ZERO, takamakaCode, Files.readAllBytes(jar), takamakaCode));
			nonce = nonce.add(ONE);
		}
		else
			jarSupplier = null;

		// we create the accounts
		BigInteger gas = BigInteger.valueOf(10_000); // enough for creating an account
		List<CodeSupplier<StorageReference>> accounts = new ArrayList<>();

		if (redGreen)
			for (int i = 1; i < funds.length; i += 2, nonce = nonce.add(ONE))
				// the constructor provides the green coins
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(payer, nonce, gas, ZERO, takamakaCode, TRGEOA_CONSTRUCTOR, new BigIntegerValue(funds[i]))));
		else
			for (BigInteger fund: funds) {
				accounts.add(postConstructorCallTransaction(new ConstructorCallTransactionRequest
					(payer, nonce, gas, ZERO, takamakaCode, TEOA_CONSTRUCTOR, new BigIntegerValue(fund))));

				nonce = nonce.add(ONE);
			}

		int i = 0;
		this.accounts = new StorageReference[redGreen ? funds.length / 2 : funds.length];
		for (CodeSupplier<StorageReference> account: accounts) {
			this.accounts[i] = account.get();

			if (redGreen) {
				// then we add the red coins
				postInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(payer, nonce, gas, ZERO, takamakaCode,
					RECEIVE_RED, this.accounts[i], new BigIntegerValue(funds[i * 2])));

				nonce = nonce.add(ONE);
			}

			i++;
		}

		this.jar = jarSupplier != null ? new Classpath(jarSupplier.get(), true) : null;
	}

	@Override
	public Optional<Classpath> jar() {
		return Optional.ofNullable(jar);
	}

	@Override
	public StorageReference account(int i) {
		return accounts[i];
	}

	@Override
	public void close() throws Exception {
		parent.close();
	}

	@Override
	public Classpath takamakaCode() {
		return parent.takamakaCode();
	}

	@Override
	public Stream<Update> getState(StorageReference reference) throws NoSuchElementException {
		return parent.getState(reference);
	}

	@Override
	public long getNow() {
		return parent.getNow();
	}

	@Override
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionRejectedException {
		return parent.addJarStoreInitialTransaction(request);
	}

	@Override
	public StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addGameteCreationTransaction(request);
	}

	@Override
	public StorageReference addRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request) throws TransactionRejectedException {
		return parent.addRedGreenGameteCreationTransaction(request);
	}

	@Override
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException, TransactionException {
		return parent.addJarStoreTransaction(request);
	}

	@Override
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addConstructorCallTransaction(request);
	}

	@Override
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.addStaticMethodCallTransaction(request);
	}

	@Override
	public StorageValue runViewInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runViewInstanceMethodCallTransaction(request);
	}

	@Override
	public StorageValue runViewStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return parent.runViewStaticMethodCallTransaction(request);
	}

	@Override
	public JarSupplier postJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionRejectedException {
		return parent.postJarStoreTransaction(request);
	}

	@Override
	public CodeSupplier<StorageReference> postConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postConstructorCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postInstanceMethodCallTransaction(request);
	}

	@Override
	public CodeSupplier<StorageValue> postStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionRejectedException {
		return parent.postStaticMethodCallTransaction(request);
	}
}
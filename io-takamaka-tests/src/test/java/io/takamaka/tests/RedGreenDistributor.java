/**
 * 
 */
package io.takamaka.tests;

import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.Node.CodeExecutionFuture;

/**
 * A test for the remote purchase contract.
 */
class RedGreenDistributor extends TakamakaTest {
	private static final ClassType DISTRIBUTOR = new ClassType("io.takamaka.tests.redgreendistributor.Distributor");
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeEach
	void beforeEach() throws Exception {
		mkRedGreenBlockchain(
			"redgreendistributor.jar",
			BigInteger.valueOf(1_100_000L), BigInteger.valueOf(1_100_000L), // green/red of first account
			BigInteger.valueOf(100_000L), BigInteger.ZERO, // green/red of second account
			BigInteger.valueOf(100_000L), BigInteger.ZERO, // green/red of third account
			BigInteger.ZERO, BigInteger.valueOf(100_000L), // green/red of fourth account
			BigInteger.ZERO, BigInteger.ZERO  // green/red of fifth account
		);
	}

	@Test @DisplayName("new RedGreenDistributor()")
	void createDistributor() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature(DISTRIBUTOR));
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 green and their red balance is zero")
	void createDistributorAndTwoPayees() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference distributor = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		postInstanceMethodCallTransaction(
			account(1),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		);

		postInstanceMethodCallTransaction(
			account(2),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		);

		addInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "distributeGreen", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))
		);

		BigIntegerValue balanceRed1 = (BigIntegerValue) runViewInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			account(1)
		);

		BigIntegerValue balanceRed2 = (BigIntegerValue) runViewInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(BigInteger.ZERO, balanceRed1.value);
		Assertions.assertEquals(BigInteger.ZERO, balanceRed2.value);
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 red and their red balance is 500")
	void createDistributorAndTwoPayeesThenDistributes1000Red() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference distributor = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		postInstanceMethodCallTransaction(
			account(1),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		);

		postInstanceMethodCallTransaction(
			account(2),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		);

		addInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))
		);

		BigIntegerValue balanceRed1 = (BigIntegerValue) runViewInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			account(1)
		);

		BigIntegerValue balanceRed2 = (BigIntegerValue) runViewInstanceMethodCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(500, balanceRed1.value.intValue());
		Assertions.assertEquals(500, balanceRed2.value.intValue());
	}

	@Test @DisplayName("distributeRed() cannot be called from an externally owned account that is not red/green")
	void distributeRedCannotBeCalledFromNOnRedGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException {
		CodeExecutionFuture<StorageReference> distributor = postConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		CodeExecutionFuture<StorageReference> eoa = postConstructorCallTransaction(
			account(0),
			_20_000,
			BigInteger.ONE,
			jar(),
			new ConstructorSignature(ClassType.EOA, ClassType.BIG_INTEGER),
			new BigIntegerValue(_20_000)
		);

		postInstanceMethodCallTransaction(
			account(1),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor.get()
		);

		postInstanceMethodCallTransaction(
			account(2),
			_20_000,
			BigInteger.ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor.get()
		);

		throwsTransactionException(() ->
			addInstanceMethodCallTransaction(
				eoa.get(),
				_20_000,
				BigInteger.ONE,
				jar(),
				new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
				distributor.get(), new BigIntegerValue(BigInteger.valueOf(1_000))
			)
		);
	}

	@Test @DisplayName("new RedGreenDistributor() then fails while adding a payee without green coins")
	void createDistributorThenFailsByAddingPayeeWithoutGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		StorageReference distributor = addConstructorCallTransaction(account(0), _20_000, BigInteger.ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		throwsTransactionRejectedException(() ->
			addInstanceMethodCallTransaction(
				account(3),
				_20_000,
				BigInteger.ONE,
				jar(),
				new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
				distributor
			)
		);
	}
}
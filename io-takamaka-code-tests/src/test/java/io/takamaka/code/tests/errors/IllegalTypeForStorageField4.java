/**
 * 
 */
package io.takamaka.code.tests.errors;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.nodes.DeserializationError;
import io.takamaka.code.tests.TakamakaTest;

class IllegalTypeForStorageField4 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode("illegaltypeforstoragefield4.jar", _1_000_000_000);
	}

	@Test @DisplayName("storing non-storage into interface field fails")
	void triesToStoreNonStorageIntoInterfaceField() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, CodeExecutionException {
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addConstructorCallTransaction
				(privateKey(0), account(0), _20_000, BigInteger.ONE, jar(),
				new ConstructorSignature("io.takamaka.tests.errors.illegaltypeforstoragefield4.C"))
		);
	}
}
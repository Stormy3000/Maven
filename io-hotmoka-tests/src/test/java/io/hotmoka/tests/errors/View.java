/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests.errors;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.SideEffectsInViewMethodException;
import io.hotmoka.tests.HotmokaTest;

class View extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("view.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar then call to View.no1() fails")
	void callNo1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.examples.errors.view.C"));

		HotmokaTest.throwsTransactionExceptionWithCause(NoSuchMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _100_000, jar(),
				new NonVoidMethodSignature("io.hotmoka.examples.errors.view.C", "no1", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17)));
	}

	@Test @DisplayName("install jar then call to View.no2() fails")
	void callNo2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.examples.errors.view.C"));

		HotmokaTest.throwsTransactionExceptionWithCause(SideEffectsInViewMethodException.class, () -> 
			runInstanceMethodCallTransaction(account(0), _100_000, jar(),
				new NonVoidMethodSignature("io.hotmoka.examples.errors.view.C", "no2", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
				c, new IntValue(13), new IntValue(17)));
	}

	@Test @DisplayName("install jar then call to View.yes() succeeds")
	void callYes() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), new ConstructorSignature("io.hotmoka.examples.errors.view.C"));

		runInstanceMethodCallTransaction(account(0), _100_000, jar(),
			new NonVoidMethodSignature("io.hotmoka.examples.errors.view.C", "yes", BasicTypes.INT, BasicTypes.INT, BasicTypes.INT),
			c, new IntValue(13), new IntValue(17));
	}
}
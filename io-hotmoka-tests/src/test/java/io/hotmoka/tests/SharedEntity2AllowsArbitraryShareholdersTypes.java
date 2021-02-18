package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import static io.hotmoka.beans.Coin.*;
import static io.hotmoka.beans.types.BasicTypes.LONG;

/**
 * A test showing that it is possible to have a shared entity with unrelated
 * shareholders' types, since generic types are erased at compilation time.
 */
class SharedEntity2AllowsArbitraryShareholdersTypes extends TakamakaTest {
    private static final ClassType MY_CLASS = new ClassType("io.hotmoka.examples.sharedentities.MyClass");
    private static final ClassType SHARED_ENTITY_2 = new ClassType("io.hotmoka.examples.sharedentities.SharedEntity2");
    private static final ClassType SIMPLE_SHARED_ENTITY_2 = new ClassType("io.hotmoka.examples.sharedentities.SimpleSharedEntity2");
    private static final ClassType OFFER_2 = new ClassType(SHARED_ENTITY_2 + "$Offer");
    private static final ConstructorSignature MY_CLASS_CONSTRUCTOR = new ConstructorSignature(MY_CLASS);
    private static final ConstructorSignature SIMPLE_SHARED_ENTITY_2_CONSTRUCTOR = new ConstructorSignature(SIMPLE_SHARED_ENTITY_2, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
    private StorageReference creator;
    private StorageReference seller;
    private StorageReference buyer;
    private TransactionReference classpath;

    @BeforeAll
	static void beforeAll() throws Exception {
		setJar("sharedentities.jar");
	}

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(0);
        seller = account(1);
        buyer = account(2);
        classpath = jar();
    }

    @Test
    @DisplayName("acceptance with different shareholder classes works")
    void acceptanceWithDifferentShareholderClassesWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath, MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v2)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath,
                SIMPLE_SHARED_ENTITY_2_CONSTRUCTOR, sellerContractMyClass, new BigIntegerValue(BigInteger.TEN));

        // create an offer by the seller using his contract
        StorageReference offer = (StorageReference) addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new NonVoidMethodSignature(MY_CLASS, "createOffer2", OFFER_2, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, LONG),
                sellerContractMyClass, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places his offer using his contract
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath,
                new VoidMethodSignature(MY_CLASS, "placeOffer", SHARED_ENTITY_2, ClassType.BIG_INTEGER, OFFER_2),
                sellerContractMyClass, sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // the buyer is an account (EOA) and he accepts the offer: this should not be valid but the test shows that it actually works
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath,
                new VoidMethodSignature(SIMPLE_SHARED_ENTITY_2, "accept", ClassType.BIG_INTEGER, OFFER_2),
                sharedEntity, new BigIntegerValue(BigInteger.TWO), offer);
    }
}
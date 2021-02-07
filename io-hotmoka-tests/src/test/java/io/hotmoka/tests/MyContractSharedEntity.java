package io.hotmoka.tests;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import static io.hotmoka.beans.Coin.*;
import static io.hotmoka.beans.types.BasicTypes.LONG;

/**
 * A test for the shared entity contract and subclasses.
 */
class MyContractSharedEntity extends TakamakaTest {
    private static final ClassType MY_CLASS = new ClassType("io.takamaka.code.dao.MyClass");
    private static final ClassType SHARED_ENTITY = new ClassType("io.takamaka.code.dao.SharedEntity");
    private static final ClassType MY_CLASS_SHARED_ENTITY_1 = new ClassType("io.takamaka.code.dao.MyClassSharedEntity1");
    private static final ClassType MY_CLASS_SHARED_ENTITY_2 = new ClassType("io.takamaka.code.dao.MyClassSharedEntity2");
    private static final ClassType OFFER = new ClassType(SHARED_ENTITY + "$Offer");
    private static final ConstructorSignature MY_CLASS_CONSTRUCTOR = new ConstructorSignature(MY_CLASS);
    private static final ConstructorSignature MY_CLASS_SHARED_ENTITY_1_CONSTRUCTOR = new ConstructorSignature(MY_CLASS_SHARED_ENTITY_1, MY_CLASS, ClassType.BIG_INTEGER);
    private static final ConstructorSignature MY_CLASS_SHARED_ENTITY_2_CONSTRUCTOR = new ConstructorSignature(MY_CLASS_SHARED_ENTITY_2, MY_CLASS, ClassType.BIG_INTEGER);
    private static final BigInteger _200_000 = BigInteger.valueOf(200_000);
    private StorageReference creator;
    private StorageReference seller;
    private StorageReference buyer;
    private TransactionReference classpath_takamaka_code;

    @BeforeEach
    void beforeEach() throws Exception {
        setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
        creator = account(0);
        seller = account(1);
        buyer = account(2);
        classpath_takamaka_code = takamakaCode();
    }

    @Test
    @DisplayName("acceptance with different shareholder classes works in MyClassSharedEntity1")
    void MyClassSharedEntity1DifferentShareholderClassesWorks() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v3)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                MY_CLASS_SHARED_ENTITY_1_CONSTRUCTOR, sellerContractMyClass, new BigIntegerValue(BigInteger.TEN));

        // create an offer (v3) by the seller using his contract
        StorageReference offer = (StorageReference) addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new NonVoidMethodSignature(MY_CLASS, "createOffer3", OFFER, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, LONG),
                sellerContractMyClass, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places his offer using his contract
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(MY_CLASS, "placeOffer", SHARED_ENTITY, ClassType.BIG_INTEGER, OFFER),
                sellerContractMyClass, sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // the buyer is an account (EOA) and he accepts the offer
        // this would not be valid but the test passes
        addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_1, "accept", ClassType.BIG_INTEGER, ClassType.PAYABLE_CONTRACT, OFFER),
                sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer);
    }


    @Test
    @DisplayName("acceptance with different shareholder classes fails in MyClassSharedEntity2")
    void MyClassSharedEntity2DifferentShareholderClassesFails() throws SignatureException, TransactionException, CodeExecutionException, InvalidKeyException, TransactionRejectedException {
        // create the MyClass contract from the seller
        StorageReference sellerContractMyClass = addConstructorCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                MY_CLASS_CONSTRUCTOR);

        // create a shared entity contract (v3)
        StorageReference sharedEntity = addConstructorCallTransaction(privateKey(0), creator, _200_000, panarea(1), classpath_takamaka_code,
                MY_CLASS_SHARED_ENTITY_2_CONSTRUCTOR, sellerContractMyClass, new BigIntegerValue(BigInteger.TEN));

        // create an offer (v3) by the seller using his contract
        StorageReference offer = (StorageReference) addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new NonVoidMethodSignature(MY_CLASS, "createOffer3", OFFER, ClassType.BIG_INTEGER, ClassType.BIG_INTEGER, LONG),
                sellerContractMyClass, new BigIntegerValue(BigInteger.TWO), new BigIntegerValue(BigInteger.TWO), new LongValue(1893456000));

        // the seller places his offer using his contract
        addInstanceMethodCallTransaction(privateKey(1), seller, _200_000, panarea(1), classpath_takamaka_code,
                new VoidMethodSignature(MY_CLASS, "placeOffer", SHARED_ENTITY, ClassType.BIG_INTEGER, OFFER),
                sellerContractMyClass, sharedEntity, new BigIntegerValue(BigInteger.ZERO), offer);

        // the buyer is an account (EOA) and he accepts the offer
        // case 1: ClassCastException
        throwsTransactionExceptionWithCause("java.lang.ClassCastException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_2, "accept", ClassType.BIG_INTEGER, ClassType.PAYABLE_CONTRACT, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer)
        );

        // case 2: IllegalArgumentException
        throwsTransactionExceptionWithCause("java.lang.IllegalArgumentException", () ->
                addInstanceMethodCallTransaction(privateKey(2), buyer, _200_000, panarea(1), classpath_takamaka_code,
                        new VoidMethodSignature(MY_CLASS_SHARED_ENTITY_2, "accept", ClassType.BIG_INTEGER, MY_CLASS, OFFER),
                        sharedEntity, new BigIntegerValue(BigInteger.TWO), buyer, offer)
        );
    }
}

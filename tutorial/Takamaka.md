# Takamaka: Smart Contracts in Java

Takamaka is a Java framework for writing smart contracts.
This tutorial explains how Takamaka code is written and
executed in blockchain.

# Table of Contents
1. [Introduction](#introduction)
2. [A First Takamaka Program](#first-program)
    - [Create a Test Blockchain](#memory-blockchain)
    - [A Transaction that Stores a Jar in Blockchain](#jar-transaction)
    - [A Transaction that Invokes a Constructor](#constructor-transaction)
    - [A Transaction that Invokes a Method](#method-transaction)
    - [Storage Types and Constraints on Storage Classes](#storage-types)
3. [The Notion of Smart Contract](#smart-contracts)
    - [A Simple Ponzi Scheme Contract](#simple-ponzi)
    - [The `@Entry` and `@Payable` Annotations](#entry-payable)
    - [Payable Contracts](#payable-contracts)
    - [The `@View` Annotation](#view)
    - [The Hierarchy of Contracts](#hierarchy-contracts)
4. [Utility Classes](#utility-classes)
    - [Storage Lists](#storage-lists)
    - [A Note on Re-entrancy](#a-note-on-re-entrancy)
    - Storage Arrays
    - Storage Maps

# Introduction <a name="introduction"></a>

Takamaka is a Java framework for writing smart contracts.
This means that it allows programmers to use Java for writing code
that can be installed and run on blockchain. Programmers will not have
to deal with the storage of objects in blockchain: this is completely
transparent to them. This makes Takamaka completely different from other
attempts at using Java for writing smart contracts, where programmers
must use specific method calls to persist data on blockchain.

Writing smart contracts in Java entails that programmers
do not have to learn yet another programming language.
Moreover, they can use a well-understood and stable development
platform, together with all its modern tools. Programmers can use
features from the latest versions of Java, such as streams and lambda
expressions.

There are, of course, limitations to the kind of code that can
be run inside a blockchain. The most important limitation is
deterministic behavior, as we will see later.

# A First Takamaka Program <a name="first-program"></a>

Let us start from a simple example of Takamaka code. Since we are
writing Java code, there is nothing special to learn or install
before starting writing programs in Takamaka. Just use your
preferred integrated development environment (IDE) for Java. Or even
do everything from command-line, if you prefer. Our examples below will be
shown for the Eclipse IDE.

Our goal will be to create a Java class that we will instantiate
and use in blockchain. Namely, we will learn how to create an object
of the class that will persist in blockchain and how we can later
call the `toString()` method on that instance in blockchain.

Let us hence create an Eclipse Java project `takamaka1`. Add
a `lib` folder inside it and copy there the two jars that contain the
Takamaka runtime and base development classes.
Add them both to the build path. The result should look
similar to the following:

![The `takamaka1` Eclipse project](pics/takamaka1.png "The takamaka1 Eclipse project")

Let us create a package `takamaka.tests.family`. Inside that package,
create a Java source `Person.java`, by copying and pasting
the following code:

```java
package takamaka.tests.family;

public class Person {
  private final String name;
  private final int day;
  private final int month;
  private final int year;
  public final Person parent1;
  public final Person parent2;

  public Person(String name, int day, int month, int year, Person parent1, Person parent2) {
    this.name = name;
    this.day = day;
    this.month = month;
    this.year = year;
    this.parent1 = parent1;
    this.parent2 = parent2;
  }

  public Person(String name, int day, int month, int year) {
    this(name, day, month, year, null, null);
  }

  @Override
  public String toString() {
    return name +" (" + day + "/" + month + "/" + year + ")";
  }
}
```

This is plain old Java code and should not need any comment. Compile it
(this should be automatic in Eclipse, if the Project &rarr; Build Automatically
option is set), create a folder `dist` and export there the project in jar format,
with name `takamaka1.jar` (click on the
`takamaka1` project, then right-click on the project, select Export &rarr; Java &rarr; Jar File
and choose the `dist` folder and the `takamaka1.jar` name). Only the compiled
class files will be relevant: Takamaka will ignore source files, manifest
and any resources in the jar, hence you needn't add them there. The result should
look as the following:

![The `takamaka1` Eclipse project, exported in jar](pics/takamaka1_jar.png "The takamaka1 Eclipse project, exported in jar")

## Create a Test Blockchain <a name="memory-blockchain"></a>

The next step is to install that jar in blockchain, use it to create an instance
of `Person` and call `toString()` on that instance. For that, we need a running
blockchain node.

> Future versions of this document will show how to use a test network, instead of running a local simulation of a node.

Let us hence create another Eclipse project, that will start
a local simulation of a blockchain node, actually working over the disk memory
of our local machine. That blockchain simulation in memory is inside a third Takamaka jar.
Create then another Eclipse project named `blockchain`, add a `lib` folder and
include three Takamaka jars inside `lib`; both `takamaka_runtime.jar` and
`takamaka_memory.jar` must be added to the build path of this project;
do not add, instead, `takamaka_base.jar` to the build path: these base classes are
needed for developing Takamaka code (as shown before) and will be installed in blockchain
as a classpath needed by our running code. But they must not be part of the build path.
Finally, add inside `lib` and to the build path the BCEL jar that Takamaka uses for code instrumentation.
The result should look like the following:

![The `blockchain` Eclipse project](pics/blockchain1.png "The blockchain Eclipse project")

Let us write a main class that starts the blockchain in disk memory: create a package
`takamaka.tests.family` and add the following class `Main.java`:

```java
package takamaka.tests.family;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));
  }
}
```

As you can see, this class simply creates an instance of the blockchain on disk memory.
It requires to initialize that blockchain, by installing the base classes for Takamaka,
that we had previously put inside `lib`, and by creating two accounts, funded with
100,000 and 200,000 units of coin, respectively. We will use later such accounts
to run blockchain transactions. They will be available as `blockchain.account(0)`
and `blockchain.account(1)`, respectively.

So, what is the constructor of `InitializedMemoryBlockchain` doing here? Basically, it is
initializing a directory, named `chain`, and it is running a few initial transactions
that lead to the creation of two accounts. You can see the result if you run class
`takamaka.tests.family.Main`, refresh the `blockchain` project (click on it and push the F5 key)
and inspect the `chain` directory that should have appeared:

![The `chain` directory appeared](pics/blockchain2.png "The chain directory appeared")

Inside this `chain` directory, you can see that a block has been created (`b0`) inside which
four transactions (`t1`, `t2`, `t3` and `t4`) have been executed, that create and fund
our two initial accounts. Each transaction is specified by a request and a corresponding
response. They are kept in serialized form (`request` and `response`) but are also
reported in textual form (`request.txt` and `response.txt`). Such textual
representations would not be kept in a real blockchain, but are useful here, for debugging
or learning purposes. We do not investigate further the content of the `chain` directory,
for now. Later, when we will run our own transactions, we will see these files in more detail.

## A Transaction that Stores a Jar in Blockchain <a name="jar-transaction"></a>

Let us consider the `blockchain` project. The `Person` class is not in its build path
nor in its class path at run time.
If we want to call the constructor of `Person`, that class must somehow be in the class path.
In order to put `Person` in the class path, we must install
`takamaka1.jar` inside the blockchain, so that we can later refer to it and call
the constructor of `Person`. Let us hence modify the `takamaka.tests.family.Main.java`
file in order to run a transaction that install `takamaka1.jar` inside the blockchain:

```java
package takamaka.tests.family;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));

    TransactionReference takamaka1 = blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      BigInteger.valueOf(100_000L), // gas provided to the transaction
      blockchain.takamakaBase, // reference to a jar in the blockchain that includes the basic Takamaka classes
      Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar")), // bytes containing the jar to install
      blockchain.takamakaBase // dependency
    ));
  }
}
```

The `addJarStoreTransaction()` method expands the blockchain with a new transaction, whose goal
is to install a jar inside the blockchain. The jar is provided as a sequence of bytes
(`Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar"))`, assuming that the
`takamaka1` project is in the same workspace as `blockchain`). This transaction, as any
Takamaka transaction, must be payed. The payer is specified as `blockchain.account(0)`, that is,
the first of the two accounts created at the moment of creation of the blockchain.
It is specified that the transaction can cost up to 100,000 units of gas. The transaction request
specifies that its class path is `blockchain.takamakaBase`: this is the reference to a jar
installed in the blockchain at its creation time and containing `takamaka_base.jar`, that is,
the basic classes of Takamaka. Finally, the request specifies that `takamaka1.jar` has only
a single dependency: `takamaka_base.jar`. This means that when, below, we will refer to
`takamaka1` in a class path, this will indirectly include its dependency `takamaka_base.jar`.

Run the `Main` class again, refresh the `blockchain` project and see that the `chain` directory
is one transaction longer now:

![A new transaction appeared in the `chain` directory](pics/blockchain3.png "A new transaction appeared in the chain directory")

The new `t4` transaction reports a `request` that corresponds to the request that we have
coded in the `Main` class. Namely, its textual representation `request.txt` is:

```
JarStoreTransactionRequest:
  caller: 0.2#0
  gas: 100000
  class path: 0.0 non-recursively resolved
  dependencies: [0.0 non-recursively resolved]
  jar: 504b0304140008080800d294b24e000000000000000000000000140004004d4554412d494e462f4d414e49464553542e4d46f...
```

The interesting point here is that objects, such as the caller account
`blockchain.account(0)`, are represented as _storage references_ such as `0.2#0`. You can
see a storage reference as a machine-independent, deterministic pointer to an object contained
in the blockchain. Also the `takamaka_base.jar` is represented with an internal representation.
Namely, `0.0` is a _transaction reference_, that is, a reference to the transaction that installed
`takamaka_base.jar` in the blockchain: transaction 0 of block 0. The jar is the hexadecimal
representation of its byte sequence.

Let us have a look at the `response.txt` file, which is the textual representation of the outcome of
the transaction:

```
JarStoreTransactionSuccessfulResponse:
  consumed gas: 1258
  updates:
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|99874>
  instrumented jar: 504b03041400080808007ca3b24e0000000000000000000000002200040074616b616d616b612f74657374732f66616d696c792...
```

The first bit of information tells us that the transaction costed 1,258 units of gas. We had accepted to spend up to
100,000 units of gas, hence the transaction could complete correctly. The response reports also the hexadecimal representation
of a jar, which is named _instrumented_. This is because what gets installed in blockchain is not exactly the jar sent
with the transaction request, but an instrumentation of that, which adds specific features that are specific to Takamaka code.
For instance, the instrumented code will charge gas during its execution. Finally, the response reports _updates_. These are
state changes occurred during the execution of the transaction. In order terms, updates are the side-effects of the transaction,
i.e., the fields of the objects modified by the transaction. In this case, the balance of the payer of the transaction
`0.2#0` has been reduced to 99,874, since it payed for the gas (we initially funded that account with 100,000 units of coin).

> The actual amount of gas consumed by this transaction and the final balance of the payer might change in future versions of Takamaka.

## A Transaction that Invokes a Constructor <a name="constructor-transaction"></a>

We are now in condition to call the constructor of `Person` and create an instance of that class in blockchain.
First of all, we must create the class path where the constructor will run. Since the class `Person` is inside
the `takamaka1.jar` archive, the class path is simply:

```java
Classpath classpath = new Classpath(takamaka1, true);
```

The `true` flag at the end means that this class path includes the dependencies of `takamaka1`. If you look
at the code above, where `takamaka1` was defined, you see that this means that the class path will include
also the dependency `takamaka_base.jar`. If `false` would be used instead, the class path would only include
the classes in `takamaka1.jar`, which would be a problem when we will use, very soon, some support classes that
Takamaka provides, in `takamaka_base.jar`, to simplify the life of developers.

Clarified which class path to use, let us trigger a transaction that runs the constructor and adds the brand
new `Person` object into blockchain. For that, modify the `takamaka.tests.family.Main.java` source as follows:

```java
package takamaka.tests.family;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  // useful constants
  private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
  private final static ClassType PERSON = new ClassType("takamaka.tests.family.Person");

  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));

    TransactionReference takamaka1 = blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      blockchain.takamakaBase, // reference to a jar in the blockchain that includes the basic Takamaka classes
      Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar")), // bytes containing the jar to install
      blockchain.takamakaBase
    ));

    Classpath classpath = new Classpath(takamaka1, true);

    StorageReference albert = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      classpath, // reference to takamaka1.jar anbd its dependency takamaka_base.jar
      new ConstructorSignature(PERSON, ClassType.STRING, INT, INT, INT), // constructor Person(String,int,int,int)
      new StringValue("Albert Einstein"), new IntValue(14), new IntValue(4), new IntValue(1879) // actual arguments
    ));
  }
}
```

The `addConstructorCallTransaction()` method expands the blockchain with a new transaction that calls
a constructor. Again, we use `blockchain.account(0)` to pay for the transaction and we provide
100,000 units of gas, which should be enough for a constructor that just initializes a few fields.
The class path includes `takamaka1.jar` and its dependency `takamaka_base.jar`, although the latter
is not used yet. The signature of the constructor specifies that we are referring to the second
constructor of `Person`, the one that assumes `null` as parents. Finally, the actual parameters
are provided; they must be instances of the `takamaka.blockchain.values.StorageValue` interface.

Let us run the `Main` class. The result is disappointing:

```
Exception in thread "main" takamaka.blockchain.TransactionException: Failed transaction
    at takamaka.blockchain.AbstractBlockchain.wrapAsTransactionException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.lambda$runConstructorCallTransaction$11(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.wrapInCaseOfException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.runConstructorCallTransaction(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.lambda$addConstructorCallTransaction$12(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.wrapWithCodeInCaseOfException(Unknown Source)
    at takamaka.blockchain.AbstractBlockchain.addConstructorCallTransaction(Unknown Source)
    at takamaka.tests.family.Main.main(Main.java:42)
Caused by: java.lang.ClassCastException: takamaka.tests.family.Person cannot be cast to takamaka.lang.Storage
    at takamaka.blockchain.AbstractBlockchain$ConstructorExecutor.run(Unknown Source)
```

> The exact shape and line numbers of this exception trace might change in future versions of Takamaka.

The transaction failed. Nevertheless, a transaction has been added to the blockchain: refresh the
`chain` folder and look at the topmost transaction `chain/b1/t0`. There is a `request.txt`, that contains
the information that we provided in the `addConstructorCallTransaction()` specification, and there is
a `response.txt` that contains the (disappointing) outcome:

```
ConstructorCallTransactionFailedResponse:
  consumed gas: 100000
  updates:
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|88347>
```

Note that the transaction costed a lot: all 100,000 gas units have been consumed! This is a sort
of punishment for running a transaction that fails. The rationale is that this punishment should
discourage potential denial-of-service attacks, when a huge number of failing transactions are thrown
at a blockchain. At least, this attack will cost a lot.

But we still have not understood why the transaction failed. The reason is in the exception
message: `takamaka.tests.family.Person cannot be cast to takamaka.lang.Storage`. Takamaka rerquires
that all objects stored in blockchain extends the `takamaka.lang.Storage` class. That superclass
provides all the machinery needed in order to keep track of updates to such objects.

> Do not get confused here. Takamaka does **not** require all objects to extend
> `takamaka.lang.Storage`. You can use objects that do not extend that superclass in your
> Takamaka code, both instances of your classes and instances of library classes
> from the `java.*` hierarchy, for instance. What Takamaka does require, instead, is that objects
> _that must be kept in blockchain_ do implement `takamaka.lang.Storage`. This is the
> case, for instance, of objects created by the constructor invoked through the
> `addConstructorCallTransaction()` method.

Let us modify the `takamaka.tests.family.Person.java` source code then:

```java
package takamaka.tests.family;

import takamaka.lang.Storage;

public class Person extends Storage {
  ... unchanged code ...
}
```

> Extending `takamaka.lang.Storage` is all a programmer needs to do in order to let instances
> of a class be stored in blockchain. There is no explicit method to call to keep track
> of updates to such objects: Takamaka will automatically deal with the updates.

Regenerate `takamaka1.jar`, since class `Person` has changed, and export it again as
`dist/takamaka1.jar`, inside the `takamaka1` Eclipse project (some versions of Eclipse
require to delete the previous `dist/takamaka1.jar` before exporting a new version).
Run again the `takamaka.tests.family.Main` class.

> We can use the `takamaka.lang.Storage` class and we can run the resulting compiled code
> since that class is inside `takamaka_base.jar`, which as been included in the
> class path as a dependency of `takamaka1.jar`.

This time, the execution should
complete without exception. Refresh the `chain/b1/t0` directory and look at the
`response.txt` file. This time the transaction was succesful:

```
ConstructorCallTransactionSuccessfulResponse:
  consumed gas: 130
  updates:
    <THIS_TRANSACTION#0.class|takamaka.tests.family.Person>
    <0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|98334>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.day:int|14>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.month:int|4>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.year:int|1879>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.name:java.lang.String|Albert Einstein>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.parent1:takamaka.tests.family.Person|null>
    <THIS_TRANSACTION#0|takamaka.tests.family.Person.parent2:takamaka.tests.family.Person|null>
  new object: THIS_TRANSACTION#0
  events:
```

You do not need to understand the content of this response file in order to program
in Takamaka. However, it can be interesting to get an idea of its content.
The file tells that a new object has been created and stored in blockchain. It is identified as
`THIS_TRANSACTION#0` since it is the first (0th) object created during this transaction.
Its class is `takamaka.tests.family.Person`:

```
<THIS_TRANSACTION#0.class|takamaka.tests.family.Person>
```

and its fields are initialized as required:

```
<THIS_TRANSACTION#0|takamaka.tests.family.Person.day:int|14>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.month:int|4>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.year:int|1879>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.name:java.lang.String|Albert Einstein>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.parent1:takamaka.tests.family.Person|null>
<THIS_TRANSACTION#0|takamaka.tests.family.Person.parent2:takamaka.tests.family.Person|null>
```

The account that payed for the transaction sees its balance decrease:

```
<0.2#0|takamaka.lang.Contract.balance:java.math.BigInteger|98334>
```

These triples are called _updates_, since they describe how the blockchain was
updated to cope with the creation of a new object.

So where is this new `Person` object, actually? Well, it exists in blockchain only.
It did exist in RAM during the execution of the constructor. But, at the end
of the constructor,
it was deallocated from RAM and serialized in blockchain, as a set of updates.
Its storage reference has been returned to the caller of
`addConstructorCallTransaction()`:

```java
StorageReference albert = blockchain.addConstructorCallTransaction(...)
```

and can be used later to invoke methods on the object or to pass the object
as a parameter of methods or constructors: when that will occur, the object
will be deserialized from its updates in blockchain and recreated in RAM.

## A Transaction that Invokes a Method <a name="method-transaction"></a>

In our `Main` class, variable `albert` holds a machine-independent reference
to an object of class `Person`,
that has just been created in blockchain. Let us invoke the
`toString()` method on that object now. For that, we run a transaction
using `albert` as _receiver_ of `toString()`.

> In object-oriented languages, the _receiver_ of a call to a non-`static`
> method is the object over which the method is executed, that is accessible
> as `this` inside the code of the method. In our case, we want to invoke
> `albert.toString()`, hence `albert` holds the receiver of the call.

The code is the following now:

```java
package takamaka.tests.family;

import static takamaka.blockchain.types.BasicTypes.INT;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.blockchain.values.StringValue;
import takamaka.memory.InitializedMemoryBlockchain;

public class Main {
  // useful constants
  private final static BigInteger _100_000 = BigInteger.valueOf(100_000L);
  private final static ClassType PERSON = new ClassType("takamaka.tests.family.Person");

  public static void main(String[] args) throws IOException, TransactionException, CodeExecutionException {
    InitializedMemoryBlockchain blockchain = new InitializedMemoryBlockchain
      (Paths.get("lib/takamaka_base.jar"), BigInteger.valueOf(100_000), BigInteger.valueOf(200_000));

    TransactionReference takamaka1 = blockchain.addJarStoreTransaction(new JarStoreTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      blockchain.takamakaBase, // reference to a jar in the blockchain that includes the basic Takamaka classes
      Files.readAllBytes(Paths.get("../takamaka1/dist/takamaka1.jar")), // bytes containing the jar to install
      blockchain.takamakaBase
    ));

    Classpath classpath = new Classpath(takamaka1, true);

    StorageReference albert = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
      blockchain.account(0), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      classpath, // reference to takamaka1.jar and its dependency takamaka_base.jar
      new ConstructorSignature(PERSON, ClassType.STRING, INT, INT, INT), // constructor Person(String,int,int,int)
      new StringValue("Albert Einstein"), new IntValue(14), new IntValue(4), new IntValue(1879) // actual arguments
    ));

    StorageValue s = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
      blockchain.account(1), // this account pays for the transaction
      _100_000, // gas provided to the transaction
      classpath, // reference to takamaka1.jar and its dependency takamaka_base.jar
      new MethodSignature(PERSON, "toString"), // method Person.toString()
      albert // receiver of toString()
    ));

    // prints the result of the call
    System.out.println(s);
  }
}
```

Look at the call to `addInstanceMethodCallTransaction()` added at its end.
This time, we let the second account `blockchain.account(1)` pay for the transaction.
We specify to resolve method `Person.toString()` using `albert` as receiver and
to run the resolved method. The result is `s`, that we subsequently print on the standard output.
If you run class `Main`, you will see the following on the screen:

```
Albert Einstein (14/4/1879)
```

After refreshing the `chain` directory, you will see that a new transaction
`chain/b1/t1` appeared, whose `request.txt` describes the transaction that we have
requested:

```
InstanceMethodCallTransactionRequest:
  caller: 0.3#0
  gas: 100000
  class path: 0.4 recursively revolved
  method: takamaka.tests.family.Person.toString()
  receiver: 1.0#0
  actuals:
```

while the `response.txt` file reports the outcome of the transaction:

```
MethodCallTransactionSuccessfulResponse:
  consumed gas: 125
  updates:
    <0.3#0|takamaka.lang.Contract.balance:java.math.BigInteger|199987>
  returned value: Albert Einstein (14/4/1879)
  events:
```

Note that, this time, the payer is `0.3#0` and, consequently, its balance
has been updated to pay for the consumed gas.

> This `response.txt` could be surprising: by looking at the code
> of method `toString()` of `Person`, you can see that it computes a string
> concatenation `name +" (" + day + "/" + month + "/" + year + ")"`. As any
> Java programnmer knows, that is just syntactical sugar for a very
> complex sequence of operations, involving the construction of a
> `java.lang.StringBuilder` and its repeated update through a sequence of
> calls to its `concat()` methods, finalized with a call to `StringBuilder.toString()`.
> So, why are those updates
> not reported in `response.txt`? Simply because they are not updates
> to the state of the blockchain but rather updates to a `StringBuilder` object,
> local to the activation of `Person.toString()`, that dies at its end and
> is not accessible anymore afterwards. In other terms, the updates reported in
> the `response.txt` files are those observable outside the method or constructor, to
> objects that existed before the call or that are returned by the
> method or constructor itself.

As we have shown, method `addInstanceMethodCallTransaction()` can be used to
invoke an instance method on an object in blockchain. This requires some
clarification. First of all, note that the signature of the method to
call is resolved and the resolved method is then invoked. If
such resolved method is not found (for instance, if we tried to call `tostring` instead
of `toString`), then `addInstanceMethodCallTransaction()` would end up in
a failed transaction. Moreover, the usual resolution mechanism of Java methods is
applied. If, for instance, we called
`new MethodSignature(ClassType.OBJECT, "toString")`
instead of
`new MethodSignature(PERSON, "toString")`,
then method `toString` would be resolved from the run-time class of
`albert`, looking for the most specific implementation of `toString()`,
up to the `java.lang.Object` class, which would anyway end up in
running `Person.toString()`.

Method `addInstanceMethodCallTransaction()` can be used to invoke instance
methods with parameters. If a `toString(int)` method existed in `Person`,
then we could call it and pass 2019 as its argument, by writing:

```java
blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
  blockchain.account(1), // this account pays for the transaction
  _100_000, // gas provided to the transaction
  classpath, // reference to takamaka1.jar and its dependency takamaka_base.jar
  new MethodSignature(PERSON, "toString", INT), // method Person.toString(int)
  albert, // receiver of toString(int)
  new IntValue(2019) // actual argument(s)
));
```

where we have added the formal argument `INT`
(that is, `takamaka.blockchain.types.BasicTypes.INT`)
and the actual argument `new IntValue(2019)`.

Method `addInstanceMethodCallTransaction()` cannot be used to call a static
method. For that, use `addStaticMethodCallTransaction()` instead, that accepts
a request similar to that for `addInstanceMethodCallTransaction()`, but without
receiver.

## Storage Types and Constraints on Storage Classes <a name="storage-types"></a>

We have seen how to invoke a constructor of a class to build an object in
blockchain or to invoke a method on an object in blockchain. Both constructors and
methods can receive arguments. Constructors yield a reference to a new
object, freshly allocated; methods might yield a returned value, if they are
not declared as `void`. This means that there is a bidirectional
exchange of data from outside the blockchain to inside it, and back. But not any
kind of data can be exchanged. Namely, only _storage values_ can be exchanged,
that belong to the so called _storage types_. Storage values are

1. primitive values of Java (characters, bytes, shorts, integers, longs, floats,
doubles and booleans), or
2. reference values whose class extends `takamaka.lang.Storage` (that is, _storage objects_), or
3. `null`, or
4. elements of immutable `enum`s, or
5. a few special reference values: `java.math.BigInteger`s and `java.lang.String`s.

Storage values cross the
blockchain boundary inside wrapper objects. For instance the integer 2,019
is first wrapped into `new IntValue(2019)` and then passed
as a parameter of a method or constructor. In our previous example,
when we called `Person.toString()`, the result `s` was actually a wrapper
of a `java.lang.String` object. Boxing and unboxing into/from wrapper objects
is automatic: our class `Person` does not show that machinery.

What should be retained of the above discussion is that constructors and
methods of Takamaka classes, if we want them to be called from outside the
blockchain, must receive storage values as parameters and must return storage
values (if they are not `void` methods). A method that expects a parameter of
type `java.util.HashSet`, for instance, can be defined and called
from inside the Takamaka code, but cannot be called from outside the blockchain,
such as, for instance, from our `Main` class or from a wallet.

We conclude this section with a formal definition of storage objects.
We have already said that storage objects can be kept in blockchain
and their class must extend
`takamaka.lang.Storage`. But there are extra constraints. Namely,
fields of a storage objects are part of the representation of such
objects and must, themselves, be kept in blockchain. Hence, a storage object:
1. has a class that extends (directly or indirectly) `takamaka.lang.Storage`, and
2. is such that all its fields hold storage values (primitives, storage objects, `null`,
elements of immutable `enum`s, a `java.math.BigInteger` or a `java.lang.String`).

Note that the above conditions hold for the class `Person` defined above. Instead,
the following are examples of what is **not** allowed in a field of a storage object:
1. arrays
2. collections from `java.util.*`

We will see later how to overcome these limitations.

> Again, we stress that such limitations only apply to storage objects.
> Other objects, thet needn't be kept in blockchain but are useful for
> the implementation of Takamaka code, can be defined in a completely free way
> and used in code that runs in the blockchain.

# The Notion of Smart Contract <a name="smart-contracts"></a>

A contract is a legal agreement among two or more parties. A good contract
should be unambiguous, since otherwise its interpretation could be
questioned or misunderstood. A legal system normally enforces the
validity of a contract. In the context of software development, a *smart contract*
is a piece of software with deterministic behavior, whose semantics should be
clear and enforced by a consensus system. Blockchains provide the perfect
environment where smart contracts can be deployed and executed, since their
(typically) non-centralized nature reduces the risk that a single party
overthrows the rules of consensus, by providing for instance a non-standard
semantics for the code of the smart contract.

Contracts are allowed to hold and transfer money to other contracts. Hence,
traditionally, smart contracts are divided into those that hold money
but have no code (*externally owned accounts*), and those that,
instead, contain code (*smart contracts*).
The formers are typically controlled by an external agent (a wallet,
a human) while the latters are typically controlled by their code.
Takamaka implements both alternatives as instances of the abstract library class
`takamaka.lang.Contract` (inside `takamaka_base.jar`). That class extends
`takamaka.lang.Storage`, hence its instances can be kept in blockchain.
The Takamaka library defines subclasses of `takamaka.lang.Contract`, that
we will investigate later. Programmers can define their own subclasses too.

This chapter presents a simple smart contract, whose goal is to
enforce a Ponzi investment scheme: each investor pays back the previous investor,
with at least a 10% reward; as long as new
investors keep coming, each investor gets at least a 10% reward; the last
investor, instead, will never see his/her investment back.
The contract has been inspired by a similar Ethereum contract
from Iyer and Dannen,
*Building Games with Ethereum Smart Contracts*, page 145, Apress 2018.

We will develop the contract in successive versions, in order to highlight
the meaning of each language feature of Takamaka.

## A Simple Ponzi Scheme Contract <a name="simple-ponzi"></a>

Create a new `takamaka2` Java project in Eclipse. Create folders `lib`
and `dist` inside the project. Put both `takamaka_base.jar` and `takamaka_runtime.jar`
inside `lib` and add them to the build path of `takamaka2`. Create package
`takamaka.tests.ponzi`; create class `SimplePonzi.java` inside that
package and copy the following code in `SimplePonzi.java`:

```java
package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public void invest(Contract investor, BigInteger amount) {
    // new investments must be 10% greater than current
    BigInteger minimumInvestment = currentInvestment.multiply(_11).divide(_10);
    require(amount.compareTo(minimumInvestment) > 0, () -> "You must invest more than " + minimumInvestment);

    // document new investor
    currentInvestor = investor;
    currentInvestment = amount;
  }
}
```

> This code is only the starting point of our discussion.
> The real final version of this contract will appear at
> the end of this section.

Look at the code of `SimplePonzi.java` above. The contract has a single
method, named `invest`. This method lets a new `investor` invest
a given `amount` of coins. This amount must be at least 10% more than
the current investment. The expression `amount.compareTo(minimumInvestment) > 0`
is a comparison between two Java `BigInteger`s and should be read as the
more familiar `amount > minimumInvestment`: the latter cannot be
written in this form, since Java does not allow comparison operators
to work on reference types.
The static method `takamaka.lang.Takamaka.require()` can be used to require
some precondition to hold. The `require(condition, message)` call throws an
exception if `condition` does not hold, with the given `message`.
If the new investment is at least 10% larger than the current, it will be
saved in the state of the contract, together with the new investor.

> You might wonder why we have written
> `require(..., () -> "You must invest more than " + minimumInvestment)`
> instead of the simpler
> `require(..., "You must invest more than " + minimumInvestment)`.
> Both are possible and semantically identical. However, the former
> uses a lambda expression that computes the string concatenaton only if
> the message is needed; the latter always computes the string concatenation.
> Hence, the first version consumes less gas, in general, and is consequently
> preferrable. This technique simulates lazy evaluation in a language, like
> Java, that has only eager evaluation for actual parameters. This technique
> has been used since years in JUnit assertions.

## The `@Entry` and `@Payable` Annotations <a name="entry-payable"></a>

The previous code of `SimplePonzi.java` is unsatisfactory, for at least two
reasons, that we will overcome in this section:

1. any contract can call `invest()` and let another contract `investor` invest
   in the game. This is against our intuition that each investor decides when
   and how much he (himself) decides to invest;
2. there is no money transfer. Anybody can call `invest()`, with an arbitrary
   `amount` of coins. The previous investor does not get the investment back
   when a new investor arrives since, well, he never really invested anything.

Let us rewrite `SimplePonzi.java` in the following way:

```java
package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @Entry void invest(BigInteger amount) {
    // new investments must be 10% greater than current
    BigInteger minimumInvestment = currentInvestment.multiply(_11).divide(_10);
    require(amount.compareTo(minimumInvestment) > 0, () -> "You must invest more than " + minimumInvestment);

    // document new investor
    currentInvestor = caller();
    currentInvestment = amount;
  }
}
```

The difference with the previous version of `SimplePonzi.java`
is that the `investor` argument of `invest()` has disappeared.
At its place, `invest()` has been annotated as `@Entry`. This annotation
**restricts** the possible uses of method `invest()`. Namely, it can be
called  from another contract *c* or from an external wallet,
with a paying contract *c*, that pays for a transaction that runs
`invest()`. In both cases, the contract *c* is available, inside
`invest()`, as `caller()`. This is, indeed, saved, in the above code,
into `currentInvestor`.

> The annotation `@Entry` marks a boundary between contracts.
> An `@Entry` method can only be called from the code of another contract
> instance or from a wallet. It cannot, for instance, be called from
> the code of a class that is not a contract, nor from the same contract instance.
> If an `@Entry` method is redefined, the redefinitions must also be
> annotated as `@Entry`.

> Method `caller()` can only be used inside an `@Entry` method or
> constructor and refers to the contract that called that method or constructor.
> Hence, it will never yield `null`. If an `@Entry` method or constructor
> calls another method *m*, then `caller()` is **not** available inside *m*
> and must be passed as an explicit parameter to *m*, if needed there.

The use of `@Entry` solves the first problem. However, there is still no money
transfer in this version of `SimplePonzi.java`. What we still miss is to require
the caller of `invest()` to actually pay for the `amount` units of coin.
Since `@Entry` guarantees that the caller of `invest()` is a contract and since
contracts hold money, this means that the caller contract of `invest()`
must be charged `amount` coins at the moment of calling `invest()`.
This can be achieved with the `@Payable` annotation, that we apply to `invest()`:

```java
package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private Contract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @Payable @Entry void invest(BigInteger amount) {
    // new investments must be 10% greater than current
    BigInteger minimumInvestment = currentInvestment.multiply(_11).divide(_10);
    require(amount.compareTo(minimumInvestment) > 0, () -> "You must invest more than " + minimumInvestment);

    // document new investor
    currentInvestor = caller();
    currentInvestment = amount;
  }
}
```

When a contract calls `invest()` now, that contract will be charged `amount` coins,
automatically. These coins will be automatically transferred to the
balance of the instance of `SimplePonzi` that receives the call.
If the balance of the calling contract is too low for that, the call
will be automatically rejected with an insufficient funds exception. The caller
must be able to pay for both `amount` and the gas needed to run `invest()`. Hence,
he must hold a bit more than `amount` coins at the moment of calling `invest()`.

> The `@Payable` annotation can only be applied to a method or constructor that
> is also annotated as `@Entry`. If a `@Payable` method is redefined, the redefinitions
> must also be annotated as `@Payable`. A `@Payable` method or constructor
> must have a first argument of type `int`, `long` or `java.math.BigInteger`,
> dependending on the amount of coins that the programmer allows one to transfer
> at call time. The name of the argument is irrelevant, but we will keep
> using `amount` for it.

## Payable Contracts <a name="payable-contracts"></a>

The `SimplePonzi.java` class is not ready yet. Namely, investors have to pay
an always increasing amount of money to replace the current investor.
However, this one never gets the previous investment back, plus the 10% award
(at least). Coins keep flowing inside the `SimplePonzi` contract and remain
stuck there, for ever. The code needs an apparently simple change: just add a single line
before the update of the new current investor. That line should send
`amount` units of coin to `currentInvestor`, before it gets replaced:

```java
// document new investor
currentInvestor.receive(amount);
currentInvestor = caller();
currentInvestment = amount;
```

In other words, a new investor calls `invest()` and pays `amount` coins to
the `SimplePonzi` contract (since `invest()` is `@Payable`); then
this `SimplePonzi` contract transfers the same `amount` of coins to pay back the
previous investor. Money flows through the `SimplePonzi` contract but
does not stay there for long.

The problem with this simple line of code is that it does not compile.
There is no `receive()` method in `takamaka.lang.Contract`:
a contract can receive money only through calls to its `@Payable`
constructors and methods. Since `currentInvestor` is, very generically,
an instance of `Contract`, that has no `@Payable` methods,
there is no method
that we can call here for sending money to `currentInvestor`.
This limitation is a deliberate choice of the design of Takamaka.

> Solidity programmers will find this very different from what happens
> in Solidity contracts. Namely, these always have a _fallback function_ that
> can be called for sending money to a contract. A problem with Solidity's approach
> is that the balance of a contract is not fully controlled by its
> payable methods, since money can always flow in through the fallback
> function. This led to software bugs, when a contract found itself
> richer then expected, which violated some (wrong) invariants about
> its state. For more information, see Antonopoulos and Wood,
> *Mastering Ethereum*, page 181 (*Unexpected Ether*), 2019, O'Reilly Media, Inc.

So how do we send money back to `currentInvestor`? The solution is to
restrict the kind of contracts that can take part in the Ponzi scheme.
Namely, we limit the game to contracts that implement class
`takamaka.lang.PayableContract`, a subclass of `takamaka.lang.Contract`
that, yes, does have a `receive()` method. This is not really a restriction,
since the typical players of our Ponzi contract are externally
owned accounts and all externally owned contracts are `PayableContract`s.

Let us hence apply the following small changes to our `SimplePonzi.java` class:

1. the type of `currentInvestment` must be restricted to `PayableContract`;
2. the `invest()` method must be an entry for `PayableContract`s only;
3. the return value of `caller()` must be cast to `PayableContract`, which is
   safe thanks to point 2 above.

The result is the following:

```java
package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;

public class SimplePonzi extends Contract {
  private final BigInteger _10 = BigInteger.valueOf(10L);
  private final BigInteger _11 = BigInteger.valueOf(11L);
  private PayableContract currentInvestor;
  private BigInteger currentInvestment = BigInteger.ZERO;

  public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
    // new investments must be 10% greater than current
    BigInteger minimumInvestment = currentInvestment.multiply(_11).divide(_10);
    require(amount.compareTo(minimumInvestment) > 0, () -> "You must invest more than " + minimumInvestment);

    // document new investor
    currentInvestor.receive(amount);
    currentInvestor = (PayableContract) caller();
    currentInvestment = amount;
  }
}
```

Note the use of `@Entry(PayableContract.class)` in the code above:
an `@Entry(C.class)` method can only be called by a contract whose class
is `C` or a subclass of `C`. Otherwise, a run-time exception will occur.

## The `@View` Annotation <a name="view"></a>

Our `SimplePonzi.java` code can still be improved. As it is now,
an investor must call `invest()` and be ready to pay a sufficiently
large `amount` of coins to pay back and replace the previous investor.
How much is *large* actually large enough? Well, it depends on the
current investment. But that information is kept inside the contract
and there is no easy way to access it from outside.
An investor can only try with something that looks large enough,
running a transaction that might end up in two negative scenarios:

1. the amount invested was actually large enough, but larger than needed: the investor
   invested more than required in the Ponzi scheme, risking that no one
   will ever invest more and pay him back;
2. the amount invested might not be enough: the `require()` function
   will throw an exception that makes the transaction running `invest()` fail.
   The investment will not be transferred to the `SimplePonzi` contract, but
   the investor will be punished by charging him all gas provided for
   the transaction. This is unfair since, after all, the investor has no
   way to know that the investment was not enough.

Hence, it would be nice and fair to provide investors with a way of accessing
the `currentInvestment`. This is actually a piece of cake: just add
this method to `SimplePonzi.java`:

```java
public BigInteger getCurrentInvestment() {
  return currentInvestment;
}
```

This solution is pefectly fine but can be improved. Writtem this way,
an investor that wants to call `getCurentInvestment()` must run a
blockchain transaction through the `addInstanceMethodCallTransaction()`
method of the blockchain, creating a new transaction that ends up in
blockchain. That transaction will cost gas, hence its side-effect will
be to reduce the balance of the calling investor. But that is the only
side-effect of that call! In cases like this, Takamaka allows one to
specify that a method is expected to have no side-effects on the visible
state of the blockchain, but for the change of the balance of the caller.
This is possible through the `takamaka.lang.View` annotation:

```java
public @View BigInteger getCurrentInvestment() {
  return currentInvestment;
}
```

An investor can now call that method through another API method of the
blockchain, called `runInstanceMethodCallTransaction()`, that does not expand the
blockchain, but yields the response of the transaction, including the
returned balue of the call. If method
`getCurrentInvestment()` had side-effects beyond that on the balance of
the caller, then the execution will fail with a run-time exception.
Note that the execution of a `@View` method still requires gas,
but that gas is given back at the end of the call.
The advantage of `@View` is hence that of allowing the execution
of `getCurrentInvestment()` for free and without expanding the blockchain
with useless transactions, that do not modify its state.

> The annotation `@View` is checked at run time if a transaction calls the
> `@View` method from outside the blockchain, directly. It is not checked if,
> instead, the method is called indirectly, from other Takamaka code.
> The check occurs at run time, since the presence of side-effects in
> computer code is undecidable. Future versions of Takamaka might check
> `@View` at the time of installing a jar in the blockchain, as part of
> bytecode verification. That check can only be an approximation of the
> run-time check.

## The Hierarchy of Contracts <a name="hierarchy-contracts"></a>

The figure below shows the hierarchy of contract classes in Takamaka.
The topmost class is `takamaka.lang.Contract`, an abstract class that
extends `takamaka.lang.Storage` since contracts are meant to be
stored in blockchain, as well as other classes that are not contracts,
such as our first `Person` example:

<p align="center">
  <img width="600" height="700" src="pics/contracts.png" alt="The hierarchy of contracts">
</p>

Programmers typically extend `Contract` to define their own contracts.
This is the case, for instance, of our `SimplePonzi` class.
Class `Contract` provides two final protected methods: `caller()` can
be used inside an `@Entry` method or constructor to access the calling
contract and `balance()` can be used to access the private `balance` field
of the contract.

The abstract subclass `PayableContract` is meant for contracts that
can receive coins from other contracts, through their final
`receive()` methods. A concrete subclass is `ExternallyOwnedAccount`, that is,
payable contracts that can be used to pay for a blockchain transaction.
They are typically controlled by humans, through a wallet, but can be
subclassed and instantiated freely in Takamaka code. Their constructors
allow to build an externally owned account and fund it with an initial
amount of coins. As we have seen in sections
[A Transaction that Stores a Jar in Blockchain](#jar-transaction),
[A Transaction that Invokes a Constructor](#constructor-transaction) and
[A Transaction that Invokes a Method](#method-transaction),
blockchain methods that start a transaction require to specify a payer
for that transaction. Such a payer is required to be an instance of
`ExternallyOwnedAccount`, or an exception will be thrown. In our examples
using a blockchain in disk memory, the expressions
`blockchain.account(0)` and `blockchain.account(1)` actually refer to
`ExternallyOwnedAccount` created during initialization transactions triggered
inside the constructor of the blockchain.

# Utility Classes <a name="utility-classes"></a>

We have said that storage objects must obey to some constraints.
The strongest is that their fields of reference type can only hold
storage objects. In particular, arrays are not allowed there. This can
be problematic, in particular for contracts that deal with a variable,
potentially unbound number of other contracts.

This section presents some utility classes that help programmers
cope with such constraints, by providing fixed or variable-sized collections
that can be used in storage objects, since they are storage objects themselves.
Such utility classes implement lists, arrays and maps.

## Storage Lists <a name="storage-lists"></a>

Consider the Ponzi contract again. It is somehow irrealistic, since
an investor gets its investment back in full. In a more realistic scenario,
the investor will receive the investment back gradually, as soon as new
investors arrive. This is more complex to program, since
the Ponzi contract must take note of all investors that invested up to now,
not just of the current one as in `SimplePonzi.java`. This requires a
list of investors, of unbounded size. An implementation of this gradual
Ponzi contract is reported below and has been
inspired by a similar Ethereum contract from Iyer and Dannen,
*Building Games with Ethereum Smart Contracts*, page 150, Apress 2018:

```java
package takamaka.tests.ponzi;

import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.util.StorageList;

public class GradualPonzi extends Contract {
  public final BigInteger MINIMUM_INVESTMENT = BigInteger.valueOf(1_000L);

  /**
   * All investors up to now. This list might contain the same investor many times,
   * which is important to pay him back more than investors who only invested ones.
   */
  private final StorageList<PayableContract> investors = new StorageList<>();

  public @Entry(PayableContract.class) GradualPonzi() {
    investors.add((PayableContract) caller());
  }

  public @Payable @Entry(PayableContract.class) void invest(BigInteger amount) {
    require(amount.compareTo(MINIMUM_INVESTMENT) >= 0, () -> "You must invest at least " + MINIMUM_INVESTMENT);
    BigInteger eachInvestorGets = amount.divide(BigInteger.valueOf(investors.size()));
    investors.stream().forEach(investor -> send(investor, eachInvestorGets));
    investors.add((PayableContract) caller());
  }

  private void send(PayableContract investor, BigInteger amount) {
    investor.receive(amount);
  }
}
```

The construtcor of `GradualPonzi` is an `@Entry`, hence can only be
called from another contract, that gets added, as first investor,
in the `takamaka.util.StorageList` held in field `investors`.
That utility class implements an unbounded list of objects.
It is a storage object, as long as only storage objects are
added inside it.
Subsequently, other contracts can invest by calling method `invest()`.
A minimum investment is required, but this remains constant with the time.
The `amount` invested gets split by the number of the previous investors
and sent back to each of them. Note that Takamaka allows one to use
Java 8 lambdas and streams.
Old fashioned Java programmers, who don't feel at home with such treats,
can exploit the fact that
lists are iterable and replace the single line `forEach()` call
with a more traditional (but more gas hungry):

```java
for (PayableContract investor: investors)
  send(investor, eachInvestorGets)
```

It is instead **highly discouraged** to iterate the list as if it were an
array. Namely, **do not write**

```java
for (int pos = 0; pos < investors.size(); pos++)
  send(investors.get(i), eachInvestorGets);
```

since lists are not random-access data structures and the complexity of the
last loop is quadratic in the size of the list. This is not a novelty: the
same occurs with traditional Java lists (`java.util.LinkedList`, in particular).
But, in Takamaka, code execution costs gas and
computational complexity does matter.

> Method `send()` is needed only because calls to `@Entry` methods are not yet
> allowed inside lambda expressions. This limit will be lifted soon and
> programmers will be allowed to simply write:
> ```java
> investors.stream().forEach(investor -> investor.receive(eachInvestorGets));
> ```

As this example shows, Takamaka allows generic types, as it is possible
since Java 5: we have written `StorageList<PayableContract>`.
We refer to the JavaDoc of `StorageList` for a list of its methods.
They include methods adding elements to both ends of the list, accessing and
removing elements, for iterating on a list and for building an array
with the elements in a list.

## A Note on Re-entrancy <a name="a-note-on-re-entrancy"></a>

The `GradualPonzi.java` class pays back previous investors immediately:
as soon as a new investor invests something, his investment gets
split and forwarded to all previous investors. This should
make Solidity programmers uncomfortable, since the same approach,
in Solidity, might lead to the infamous re-entrancy attack, when the
contract that receives his investment back has redefined its
fallback function in such a way to re-enter the paying contract and
re-execute the distribution of the investment.
As it is well known, such an attack has made some people rich and other
desperate. Even if such a frightening scenario does not occur,
paying previous investors immediately back is discouraged in Solidity
also for other reasons. Namely, the contract that receives his
investment back might have a redefined fallback function that
consumes too much gas or does not terminate. This would hang the
loop that pays back previous investors, actually locking the
money inside the `GradualPonzi` contract. Moreover, paying back
a contract is a relatively expensive operation in Solidity, even if the
fallback function is not redefined, and this cost is payed by the
new investor that called `invest()`, in terms of gas. The cost is linear
in the number of investors that must be payed back.

As a solution to these problems, Solidity programmers do not pay previous
investors back immediately, but let the `GradualPonzi` contract take
note of the balance of each investor, through a map.
This map is updated as soon as a new investor arrives, by increasing the
balance of every previous investor. The cost of updating the balances
is still linear in the number of previous investors, but it is cheaper
(in Solidity) than sending money back to each of them, which
requires costy inter-contract calls.
With this technique, previous investors are
now required to withdraw their balance explicitly,
through a `widthdraw()` function.
This leads to the *withdrawing pattern* used for writing Solidity contracts.

We have not used the withdrawing pattern in `GradualPonzi.java`. In general,
there is no need for such pattern in Takamaka, at least not for simple
contracts like `GradualPonzi.java`. The reason is that the
`receive()` methods of a payable contracts (corresponding to the
fallback function of Solidity) are `final` in Takamaka and very cheap
in terms of gas. In particular, inter-contract calls are not
especially expensive in Takamaka, since they are just a method
invocation in Java bytecode (one bytecode instruction). They are actually cheaper than
updating a map of balances. Moroever, avoiding the `widthdraw()` transactions
means reducing the size of the blockchain. Hence, the withdrawing pattern is both
useless in Takamaka and more expensive than paying back previous contracts
immediately.
package io.hotmoka.beans.responses;

import java.util.stream.Stream;

import io.hotmoka.beans.references.TransactionReference;

/**
 * A response for a transaction that successfully installed a jar in the blockchain.
 */
public interface TransactionResponseWithInstrumentedJar {

	/**
	 * Yields the bytes of the installed jar.
	 * 
	 * @return the bytes of the installed jar
	 */
	byte[] getInstrumentedJar();

	/**
	 * Yields the size of the instrumented jar, in bytes.
	 * 
	 * @return the size
	 */
	int getInstrumentedJarLength();

	/**
	 * Yields the dependencies of the jar, previously installed in blockchain.
	 * 
	 * @return the dependencies
	 */
	Stream<TransactionReference> getDependencies();
	
	/**
	 * Yields the version of the verification tool involved in the verification process
	 * 
	 * @return the version
	 */
	int getVerificationToolVersion();
}
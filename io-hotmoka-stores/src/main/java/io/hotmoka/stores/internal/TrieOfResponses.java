package io.hotmoka.stores.internal;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.crypto.HashingAlgorithm;
import io.hotmoka.patricia.PatriciaTrie;
import io.hotmoka.xodus.env.Store;
import io.hotmoka.xodus.env.Transaction;

/**
 * A Merkle-Patricia trie that maps references to transaction requests into their responses.
 * It optimizes the trie by sharing identical jars in responses containing an instrumented jar.
 */
public class TrieOfResponses implements PatriciaTrie<TransactionReference, TransactionResponse> {

	private final static Logger logger = LoggerFactory.getLogger(TrieOfResponses.class);

	/**
	 * The supporting trie.
	 */
	private final PatriciaTrie<TransactionReference, TransactionResponse> parent;

	/**
	 * The hashing algorithm applied to transaction references when used as
	 * keys of the trie. Since these keys are transaction references,
	 * they already hold a hash, as a string. Hence, this algorithm just amounts to extracting
	 * the bytes from that string.
	 */
	private final HashingAlgorithm<TransactionReference> hashingForTransactionReferences = new HashingAlgorithm<>() {
	
		@Override
		public byte[] hash(TransactionReference reference) {
			return hexStringToByteArray(reference.getHash());
		}
	
		@Override
		public int length() {
			return 32; // transaction references are assumed to be SHA256 hashes, hence 32 bytes
		}
	
		/**
		 * Transforms a hexadecimal string into a byte array.
		 * 
		 * @param s the string
		 * @return the byte array
		 */
		private byte[] hexStringToByteArray(String s) {
		    int len = s.length();
		    byte[] data = new byte[len / 2];
		    for (int i = 0; i < len - 1; i += 2)
		        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		
		    return data;
		}
	};

	/**
	 * The hashing algorithm used for the jars in the responses that included a jar.
	 */
	private final HashingAlgorithm<byte[]> hashingForJars;

	/**
	 * The store of the underlying Patricia trie.
	 */
	private final KeyValueStoreOnXodus keyValueStoreOfResponses;

	/**
	 * Builds a Merkle-Patricia trie that maps references to transaction requests into their responses.
	 * 
	 * @param store the supporting store of the database
	 * @param txn the transaction where updates are reported
	 * @param root the root of the trie to check out; use {@code null} if the trie is empty
	 * @param garbageCollected true if and only if unused nodes must be garbage collected; in general,
	 *                         this can be true if previous configurations of the trie needn't be
	 *                         rechecked out in the future
	 */
	public TrieOfResponses(Store store, Transaction txn, byte[] root, boolean garbageCollected) {
		try {
			this.keyValueStoreOfResponses = new KeyValueStoreOnXodus(store, txn, root);
			HashingAlgorithm<io.hotmoka.patricia.Node> hashingForNodes = HashingAlgorithm.sha256(Marshallable::toByteArray);
			this.hashingForJars = HashingAlgorithm.sha256(bytes -> bytes);
			parent = PatriciaTrie.of(keyValueStoreOfResponses, hashingForTransactionReferences, hashingForNodes, TransactionResponse::from, garbageCollected);
		}
		catch (Exception e) {
			throw InternalFailureException.of(e);
		}
	}

	/**
	 * A function called on each value before being stored in the trie;
	 * the result is actually stored at its place; the goal is
	 * to implement an optimization that shares the jar in the response.
	 * 
	 * @param response the actual response inserted in this trie
	 * @return the response that is put in its place in the parent trie
	 */
	private TransactionResponse writeTransformation(TransactionResponse response) {
		if (response instanceof TransactionResponseWithInstrumentedJar) {
			TransactionResponseWithInstrumentedJar trwij = (TransactionResponseWithInstrumentedJar) response;
			byte[] jar = trwij.getInstrumentedJar();
			// we store the jar in the store: if it was already installed before, it gets shared
			byte[] reference = hashingForJars.hash(jar);
			keyValueStoreOfResponses.put(reference, jar);

			// we replace the jar with its hash
			response = replaceJar(trwij, reference);
		}

		return response;
	}

	/**
	 * A function called on each value read from the trie;
	 * the result is actually returned at its place; the goal is to
	 * recover a jar shared with other responses.
	 * 
	 * @param response the response read from the parent trie
	 * @return return the actual response returned by this trie
	 */
	private TransactionResponse readTransformation(TransactionResponse response) {
		if (response instanceof TransactionResponseWithInstrumentedJar) {
			TransactionResponseWithInstrumentedJar trwij = (TransactionResponseWithInstrumentedJar) response;

			// we replace the hash of the jar with the actual jar
			try {
				byte[] jar = keyValueStoreOfResponses.get(trwij.getInstrumentedJar());
				response = replaceJar(trwij, jar);
			}
			catch (NoSuchElementException e) {
				logger.error("cannot find the jar for the transaction response");
				throw e;
			}
		}

		// we replace the hash of the jar with the actual jar
		return response;
	}

	private TransactionResponse replaceJar(TransactionResponseWithInstrumentedJar response, byte[] newJar) {
		if (response instanceof JarStoreTransactionSuccessfulResponse) {
			JarStoreTransactionSuccessfulResponse jstsr = (JarStoreTransactionSuccessfulResponse) response;
			return new JarStoreTransactionSuccessfulResponse
				(newJar, jstsr.getDependencies(), jstsr.getVerificationVersion(), jstsr.getUpdates(),
				jstsr.gasConsumedForCPU, jstsr.gasConsumedForRAM, jstsr.gasConsumedForStorage);
		}
		else if (response instanceof JarStoreInitialTransactionResponse) {
			JarStoreInitialTransactionResponse jsitr = (JarStoreInitialTransactionResponse) response;
			return new JarStoreInitialTransactionResponse(newJar, jsitr.getDependencies(), jsitr.getVerificationVersion());
		}
		else {
			logger.error("unexpected response containing jar, of class " + response.getClass().getName());
			return (TransactionResponse) response;
		}
	}

	@Override
	public Optional<TransactionResponse> get(TransactionReference key) {
		return parent.get(key).map(this::readTransformation);
	}

	@Override
	public void put(TransactionReference key, TransactionResponse value) {
		parent.put(key, writeTransformation(value));
	}

	@Override
	public byte[] getRoot() {
		return parent.getRoot();
	}
}
package io.hotmoka.memory.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.Config;

/**
 * The store of the memory blockchain. It is not transactional and just writes
 * everything immediately into files. It keeps responses into persistent memory,
 * while the histories are kept in RAM.
 */
class Store extends io.takamaka.code.engine.Store<MemoryBlockchainImpl> {

	/**
	 * The histories of the objects created in blockchain. In a real implementation, this must
	 * be stored in a persistent state.
	 */
	private final Map<StorageReference, TransactionReference[]> histories = new HashMap<>();

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final Map<TransactionReference, String> errors = new HashMap<>();

	/**
	 * The storage reference of the manifest stored inside the node, if any.
	 */
	private final AtomicReference<StorageReference> manifest = new AtomicReference<>();

	/**
	 * The configuration of the node.
	 */
	private final Config config;

	/**
	 * The number of transactions added to the store. This is used to associate
	 * each transaction to its progressive number.
	 */
	private int transactionsCount;

	/**
	 * A map from the transactions added to the store to their progressive number.
	 * This is needed in order to give a nice presentation of transactions, inside a
	 * directory for its block.
	 */
	private final Map<TransactionReference, Integer> progressive = new HashMap<>();

	/**
     * Creates a state for a node with the given configuration.
     * 
     * @param node the node this state if being built for
     * @param config the configuration of the node
     */
    Store(MemoryBlockchainImpl node, Config config) {
    	super(node);

    	this.config = config;
    }

    @Override
    public Optional<TransactionResponse> getResponse(TransactionReference reference) {
    	return recordTime(() -> {
    		try {
    			Path response = getPathFor(reference, "response");
    			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
    				return Optional.of(TransactionResponse.from(in));
    			}
    		}
    		catch (FileNotFoundException e) {
    			return Optional.empty();
    		}
    		catch (Exception e) {
    			logger.error("unexpected exception " + e);
    			throw InternalFailureException.of(e);
    		}
    	});
	}

	@Override
	public Optional<TransactionResponse> getResponseUncommitted(TransactionReference reference) {
		return getResponse(reference);
	}

	@Override
	public Optional<String> getError(TransactionReference reference) {
		return Optional.ofNullable(errors.get(reference));
	}

	@Override
	public Stream<TransactionReference> getHistory(StorageReference object) {
		return recordTime(() -> {
			TransactionReference[] history = histories.get(object);
			return history == null ? Stream.empty() : Stream.of(history);
		});
	}

	@Override
	public Stream<TransactionReference> getHistoryUncommitted(StorageReference object) {
		return getHistory(object);
	}

	@Override
	public Optional<StorageReference> getManifest() {
		return recordTime(() -> Optional.ofNullable(manifest.get()));
	}

	@Override
	public Optional<StorageReference> getManifestUncommitted() {
		return getManifest();
	}

	@Override
	public Optional<TransactionRequest<?>> getRequest(TransactionReference reference) {
		try {
			Path response = getPathFor(reference, "request");
			try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(response)))) {
				return Optional.of(TransactionRequest.from(in));
			}
		}
		catch (FileNotFoundException e) {
			return Optional.empty();
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void setResponse(TransactionReference reference, TransactionRequest<?> request, TransactionResponse response) {
		recordTime(() -> {
			try {
				progressive.put(reference, transactionsCount++);
				Path requestPath = getPathFor(reference, "request");
				Path parent = requestPath.getParent();
				ensureDeleted(parent);
				Files.createDirectories(parent);

				try {
					try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(reference, "response.txt")))) {
						output.print(response);
					}

					try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(getPathFor(reference, "request.txt")))) {
						output.print(request);
					}
				}
				catch (IOException e) {
					logger.error("could not expand the store", e);
					throw InternalFailureException.of(e);
				}

				try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(requestPath))) {
					request.into(oos);
				}

				try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(getPathFor(reference, "response")))) {
					response.into(oos);
				}
			}
			catch (Exception e) {
				logger.error("unexpected exception", e);
				throw InternalFailureException.of(e);
			}
		});
	}

	@Override
	public void setHistory(StorageReference object, Stream<TransactionReference> history) {
		recordTime(() -> histories.put(object, history.toArray(TransactionReference[]::new)));
	}

	@Override
	public void setManifest(StorageReference manifest) {
		recordTime(() -> Store.this.manifest.set(manifest));
	}

	@Override
	public void push(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		if (errorMessage.length() > config.maxErrorLength)
			errorMessage = errorMessage.substring(0, config.maxErrorLength) + "...";
	
		errors.put(reference, errorMessage);
	}

	/**
	 * Yields the path for a file inside the directory for the given transaction.
	 * 
	 * @param reference the transaction reference
	 * @param name the name of the file
	 * @return the resulting path
	 */
	private Path getPathFor(TransactionReference reference, String name) {
		int progressive = this.progressive.get(reference);
		return config.dir.resolve("b" + progressive / config.transactionsPerBlock).resolve(progressive % config.transactionsPerBlock + "-" + reference).resolve(name);
	}

	/**
	 * Deletes the given directory, recursively, if it exists.
	 * 
	 * @param dir the directory
	 * @throws IOException if a disk error occurs
	 */
	private static void ensureDeleted(Path dir) throws IOException {
		if (Files.exists(dir))
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}
}
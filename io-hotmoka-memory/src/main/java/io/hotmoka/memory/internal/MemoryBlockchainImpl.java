package io.hotmoka.memory.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.Config;
import io.hotmoka.memory.MemoryBlockchain;
import io.takamaka.code.engine.AbstractNodeWithHistory;

/**
 * An implementation of a blockchain that stores transactions in a directory
 * on disk memory. It is only meant for experimentation and testing. It is not
 * really a blockchain, since there is no peer-to-peer network, nor mining,
 * nor transactions. Updates are stored in files, rather than in an external database.
 */
public class MemoryBlockchainImpl extends AbstractNodeWithHistory<Config> implements MemoryBlockchain {
	private final static Logger logger = LoggerFactory.getLogger(MemoryBlockchainImpl.class);

	/**
	 * The mempool where transaction requests are stored and eventually executed.
	 */
	private final Mempool mempool;

	/**
	 * The state of the node.
	 */
	private final State state;

	/**
	 * The errors generated by each transaction (if any). In a real implementation, this must
	 * be stored in a persistent memory such as a blockchain.
	 */
	private final Map<TransactionReference, String> errors = new HashMap<>();

	/**
	 * Builds a blockchain in disk memory.
	 * 
	 * @param config the configuration of the blockchain
	 */
	public MemoryBlockchainImpl(Config config) {
		super(config);

		try {
			this.state = new State(this, config);
			this.mempool = new Mempool(this);
		}
		catch (Exception e) {
			logger.error("failed creating memory blockchain", e);

			try {
				close();
			}
			catch (Exception e1) {
				logger.error("cannot close the blockchain", e1);
				throw InternalFailureException.of(e1);
			}

			throw InternalFailureException.of(e);
		}
	}

	@Override
	public void close() throws Exception {
		mempool.stop();
		super.close();
	}

	@Override
	protected long getNow() {
		return System.currentTimeMillis();
	}

	@Override
	protected void postTransaction(TransactionRequest<?> request) {
		mempool.add(request);
	}

	@Override
	protected void expandStore(TransactionReference reference, TransactionRequest<?> request, String errorMessage) {
		if (errorMessage.length() > config.maxErrorLength)
			errorMessage = errorMessage.substring(0, config.maxErrorLength) + "...";
	
		errors.put(reference, errorMessage);
	}

	@Override
	protected TransactionResponse getResponse(TransactionReference reference) throws TransactionRejectedException {
		try {
			String error = errors.get(reference);
			if (error != null)
				throw new TransactionRejectedException(error);
			else
				return state.getResponse(reference)
					.orElseThrow(() -> new InternalFailureException("transaction reference " + reference + " is committed but the state has no information about it"));
		}
		catch (TransactionRejectedException e) {
			throw e;
		}
		catch (Exception e) {
			logger.error("unexpected exception " + e);
			throw InternalFailureException.of(e);
		}
	}

	@Override
	protected State getStore() {
		return state;
	}
}
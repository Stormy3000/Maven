package io.hotmoka.memory.internal;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.annotations.GuardedBy;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.takamaka.code.engine.ResponseBuilder;

public class Mempool {
	public final static int MAX_CAPACITY = 200_000;

	private final BlockingQueue<RequestWithId> mempool = new LinkedBlockingDeque<>(MAX_CAPACITY);
	private final BlockingQueue<RequestWithId> checkedMempool = new LinkedBlockingDeque<>(MAX_CAPACITY);
	private final AbstractMemoryBlockchain node;
	private final Object idLock = new Object();

	@GuardedBy("idLock")
	private BigInteger id;

	private final Thread checker;
	private final Thread deliverer;

	Mempool(AbstractMemoryBlockchain node) {
		this.node = node;
		this.id = BigInteger.ZERO;
		this.checker = new Thread(this::check);
		this.checker.start();
		this.deliverer = new Thread(this::deliver);
		this.deliverer.start();
	}

	public String add(TransactionRequest<?> request) {
		String result;

		synchronized (idLock) {
			result = id.toString();

			if (!mempool.offer(new RequestWithId(request, result)))
				throw new IllegalStateException("mempool overflow");

			id = id.add(BigInteger.ONE);
		}

		return result;
	}

	public void stop() {
		checker.interrupt();
		deliverer.interrupt();
	}

	private void check() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RequestWithId current = mempool.take();

				try {
					//System.out.println(current + ": checking");
					node.checkTransaction(current.request);
					if (!checkedMempool.offer(current)) {
						deliverer.interrupt();
						throw new IllegalStateException("mempool overflow");
					}
					//System.out.println(current + ": checked");
				}
				catch (TransactionRejectedException e) {
					node.setTransactionErrorFor(current.id, e.getMessage());
				}
	            catch (Throwable t) {
	            	node.setTransactionErrorFor(current.id, t.toString());
	    		}
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	private void deliver() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				RequestWithId current = checkedMempool.take();
				//System.out.println(current + ": delivering");

				try {
					ResponseBuilder<?,?> builder = node.checkTransaction(current.request);
					TransactionReference next = node.nextAndIncrement();
					node.deliverTransaction(builder, next);
					node.setTransactionReferenceFor(current.id, next);
					//System.out.println(current + ": delivered");
				}
				catch (TransactionRejectedException e) {
					node.setTransactionErrorFor(current.id, e.getMessage());
				}
	            catch (Throwable t) {
	            	node.setTransactionErrorFor(current.id, t.toString());
	    		}
			}
			catch (InterruptedException e) {
				return;
			}
		}
	}

	private static class RequestWithId {
		private final TransactionRequest<?> request;
		private final String id;

		private RequestWithId(TransactionRequest<?> request, String id) {
			this.request = request;
			this.id = id;
		}

		@Override
		public String toString() {
			return id + ": " + request.getClass().getName();
		}
	}
}
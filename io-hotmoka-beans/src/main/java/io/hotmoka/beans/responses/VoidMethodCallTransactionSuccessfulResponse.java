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

package io.hotmoka.beans.responses;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.BeanMarshallingContext;
import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.updates.Update;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A response for a successful transaction that calls a method
 * in blockchain. The method has been called without problems and
 * without generating exceptions. The method returns {@code void}.
 */
@Immutable
public class VoidMethodCallTransactionSuccessfulResponse extends MethodCallTransactionResponse implements TransactionResponseWithEvents {
	final static byte SELECTOR = 12;
	final static byte SELECTOR_NO_EVENTS_NO_SELF_CHARGED = 16;

	/**
	 * The events generated by this transaction.
	 */
	private final StorageReference[] events;

	/**
	 * Builds the transaction response.
	 * 
	 * @param selfCharged true if and only if the called method was annotated as {@code @@SelfCharged}, hence the
	 *                    execution was charged to its receiver
	 * @param updates the updates resulting from the execution of the transaction
	 * @param events the events resulting from the execution of the transaction
	 * @param gasConsumedForCPU the amount of gas consumed by the transaction for CPU execution
	 * @param gasConsumedForRAM the amount of gas consumed by the transaction for RAM allocation
	 * @param gasConsumedForStorage the amount of gas consumed by the transaction for storage consumption
	 */
	public VoidMethodCallTransactionSuccessfulResponse(boolean selfCharged, Stream<Update> updates, Stream<StorageReference> events, BigInteger gasConsumedForCPU, BigInteger gasConsumedForRAM, BigInteger gasConsumedForStorage) {
		super(selfCharged, updates, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);

		this.events = events.toArray(StorageReference[]::new);
	}

	@Override
	public Stream<StorageReference> getEvents() {
		return Stream.of(events);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VoidMethodCallTransactionSuccessfulResponse) {
			VoidMethodCallTransactionSuccessfulResponse otherCast = (VoidMethodCallTransactionSuccessfulResponse) other;
			return super.equals(other) && Arrays.equals(events, otherCast.events);
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(events);
	}

	@Override
	public String toString() {
        return super.toString() + "\n  events:\n" + getEvents().map(StorageReference::toString).collect(Collectors.joining("\n    ", "    ", ""));
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return getEvents().map(event -> event.size(gasCostModel)).reduce(BigInteger.ZERO, BigInteger::add);
	}

	@Override
	public StorageValue getOutcome() {
		return null; // a void method has no result
	}

	@Override
	public void into(BeanMarshallingContext context) throws IOException {
		boolean optimized = events.length == 0 && !selfCharged;

		context.writeByte(optimized ? SELECTOR_NO_EVENTS_NO_SELF_CHARGED : SELECTOR);
		super.into(context);

		if (!optimized) {
			context.writeBoolean(selfCharged);
			intoArrayWithoutSelector(events, context);
		}
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @param selector the selector
	 * @return the request
	 * @throws IOException if the response could not be unmarshalled
	 * @throws ClassNotFoundException if the response could not be unmarshalled
	 */
	public static VoidMethodCallTransactionSuccessfulResponse from(UnmarshallingContext context, byte selector) throws IOException, ClassNotFoundException {
		Stream<Update> updates = Stream.of(context.readArray(Update::from, Update[]::new));
		BigInteger gasConsumedForCPU = context.readBigInteger();
		BigInteger gasConsumedForRAM = context.readBigInteger();
		BigInteger gasConsumedForStorage = context.readBigInteger();
		Stream<StorageReference> events;
		boolean selfCharged;

		if (selector == SELECTOR) {
			selfCharged = context.readBoolean();
			events = Stream.of(context.readArray(StorageReference::from, StorageReference[]::new));
		}
		else if (selector == SELECTOR_NO_EVENTS_NO_SELF_CHARGED) {
			selfCharged = false;
			events = Stream.empty();
		}
		else
			throw new IOException("unexpected response selector: " + selector);

		return new VoidMethodCallTransactionSuccessfulResponse(selfCharged, updates, events, gasConsumedForCPU, gasConsumedForRAM, gasConsumedForStorage);
	}
}
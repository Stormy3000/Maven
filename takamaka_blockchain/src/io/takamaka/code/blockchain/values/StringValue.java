package io.takamaka.code.blockchain.values;

import java.math.BigInteger;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.GasCosts;
import io.takamaka.code.blockchain.annotations.Immutable;

/**
 * A string stored in blockchain.
 */
@Immutable
public final class StringValue implements StorageValue {

	private static final long serialVersionUID = -2043931695947289129L;

	/**
	 * The string.
	 */
	public final String value;

	/**
	 * Builds a string that can be stored in blockchain.
	 * 
	 * @param value the string
	 */
	public StringValue(String value) {
		this.value = value;
	}

	@Override
	public String deserialize(AbstractBlockchain blockchain) {
		// we clone the value, so that the alias behavior of values coming from outside the blockchain is fixed:
		// two parameters of an entry are never alias when they come from outside the blockchain
		return new String(value);
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StringValue && ((StringValue) other).value.equals(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return value.compareTo(((StringValue) other).value);
	}

	@Override
	public BigInteger size() {
		return GasCosts.STORAGE_COST_PER_SLOT.add(GasCosts.storageCostOf(value));
	}
}
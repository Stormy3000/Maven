package takamaka.tests.collections;

import java.math.BigInteger;

import takamaka.lang.Storage;
import takamaka.lang.View;
import takamaka.util.StorageIntMap;
import takamaka.util.StorageIntMap.Entry;

/**
 * This class defines methods that test the storage map with integer keys implementation.
 */
public class IntMapTests extends Storage {

	public static @View int testIteration1() {
		StorageIntMap<BigInteger> map = new StorageIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate1() {
		StorageIntMap<BigInteger> map = new StorageIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		// we add one to the value bound to each key
		map.keyList().forEach(key -> map.update(key, BigInteger.ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View int testUpdate2() {
		StorageIntMap<BigInteger> map = new StorageIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, BigInteger.valueOf(key));

		// we add one to the value bound to each key
		map.keys().forEach(key -> map.update(key, BigInteger.ONE::add));

		return map.stream().map(Entry::getValue).mapToInt(BigInteger::intValue).sum();
	}

	public static @View long testNullValues() {
		StorageIntMap<BigInteger> map = new StorageIntMap<>();
		for (int key = 0; key < 100; key++)
			map.put(key, null);

		return map.stream().map(Entry::getValue).filter(value -> value == null).count();
	}
}
package io.takamaka.code.verification.internal;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import io.takamaka.code.verification.BcelToClass;
import io.takamaka.code.verification.ThrowIncompleteClasspathError;

/**
 * A utility that transforms a BCEL type into its corresponding class tag.
 */
public class BcelToClassImpl implements BcelToClass {

	/**
	 * The jar with whose class loader the transformation is performed.
	 */
	private final VerifiedJarImpl jar;

	/**
	 * Builds the utility object.
	 * 
	 * @param jar the jar for whose class loader the transformation is performed
	 */
	BcelToClassImpl(VerifiedJarImpl jar) {
		this.jar = jar;
	}

	@Override
	public final Class<?> of(Type type) {
		if (type == BasicType.BOOLEAN)
			return boolean.class;
		else if (type == BasicType.BYTE)
			return byte.class;
		else if (type == BasicType.CHAR)
			return char.class;
		else if (type == BasicType.DOUBLE)
			return double.class;
		else if (type == BasicType.FLOAT)
			return float.class;
		else if (type == BasicType.INT)
			return int.class;
		else if (type == BasicType.LONG)
			return long.class;
		else if (type == BasicType.SHORT)
			return short.class;
		else if (type == BasicType.VOID)
			return void.class;
		else if (type instanceof ObjectType)
			return ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> jar.classLoader.loadClass(type.toString()));
		else { // array
			Class<?> elementsClass = of(((ArrayType) type).getElementType());
			// trick: we build an array of 0 elements just to access its class token
			return java.lang.reflect.Array.newInstance(elementsClass, 0).getClass();
		}
	}

	@Override
	public final Class<?>[] of(Type[] types) {
		return Stream.of(types)
			.map(this::of)
			.collect(Collectors.toList())
			.toArray(new Class<?>[types.length]);
	}
}
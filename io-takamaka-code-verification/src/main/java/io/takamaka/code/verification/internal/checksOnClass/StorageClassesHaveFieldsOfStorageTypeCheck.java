package io.takamaka.code.verification.internal.checksOnClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.IllegalTypeForStorageFieldError;

/**
 * A checks that payable methods have an amount first argument.
 */
public class StorageClassesHaveFieldsOfStorageTypeCheck extends VerifiedClassImpl.Builder.Check {

	public StorageClassesHaveFieldsOfStorageTypeCheck(VerifiedClassImpl.Builder verification) {
		verification.super();

		if (classLoader.isStorage(className))
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException(() -> {
				Stream.of(classLoader.loadClass(className).getDeclaredFields())
					.filter(field -> !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					.filter(field -> !isTypeAllowedForStorageFields(field.getType()))
					.map(field -> new IllegalTypeForStorageFieldError(inferSourceFile(), field.getName(), field.getType().isEnum()))
					.forEach(this::issue);
			});
	}

	@SuppressWarnings("unchecked")
	private boolean isTypeAllowedForStorageFields(Class<?> type) {
		// we allow Object since it can be the erasure of a generic type: the runtime of Takamaka
		// will check later if the actual type of the object in this field is allowed;
		// we also allow interfaces since they cannot extend Storage and only at run time it will
		// be possible to determine if the content is a storage value
		return type.isPrimitive() || type == Object.class || type.isInterface() || type == String.class || type == BigInteger.class
			|| (type.isEnum() && !hasInstanceFields((Class<? extends Enum<?>>) type))
			|| (!type.isArray() && classLoader.isStorage(type.getName()));
	}

	/**
	 * Determines if the given enumeration type has at least an instance, non-transient field.
	 * 
	 * @param clazz the class
	 * @return true only if that condition holds
	 */
	private static boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(Field::getModifiers)
			.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
	}
}
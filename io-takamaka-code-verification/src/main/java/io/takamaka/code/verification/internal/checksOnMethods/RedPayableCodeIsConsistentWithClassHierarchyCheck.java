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

package io.takamaka.code.verification.internal.checksOnMethods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.generic.MethodGen;

import io.takamaka.code.verification.ThrowIncompleteClasspathError;
import io.takamaka.code.verification.internal.CheckOnMethods;
import io.takamaka.code.verification.internal.VerifiedClassImpl;
import io.takamaka.code.verification.issues.InconsistentRedPayableError;

/**
 * A check that {@code @@RedPayable} methods only redefine {@code @@RedPayable} methods and that
 * {@code @@RedPayable} methods are only redefined by {@code @@RedPayable} methods.
 */
public class RedPayableCodeIsConsistentWithClassHierarchyCheck extends CheckOnMethods {

	public RedPayableCodeIsConsistentWithClassHierarchyCheck(VerifiedClassImpl.Verification builder, MethodGen method) {
		super(builder, method);

		if (!methodName.equals(Const.CONSTRUCTOR_NAME) && !method.isPrivate()) {
			boolean wasRedPayable = annotations.isRedPayable(className, methodName, methodArgs, methodReturnType);
	
			ThrowIncompleteClasspathError.insteadOfClassNotFoundException
				(() -> isIdenticallyRedPayableInSupertypesOf(classLoader.loadClass(className), wasRedPayable));
		}
	}

	private void isIdenticallyRedPayableInSupertypesOf(Class<?> clazz, boolean wasRedPayable) {
		if (Stream.of(clazz.getDeclaredMethods())
				.filter(m -> !Modifier.isPrivate(m.getModifiers())
						&& m.getName().equals(methodName) && m.getReturnType() == bcelToClass.of(methodReturnType)
						&& Arrays.equals(m.getParameterTypes(), bcelToClass.of(methodArgs)))
				.anyMatch(m -> wasRedPayable != annotations.isRedPayable(clazz.getName(), methodName, methodArgs, methodReturnType)))
			issue(new InconsistentRedPayableError(inferSourceFile(), methodName, clazz.getName()));
	
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null)
			isIdenticallyRedPayableInSupertypesOf(superclass, wasRedPayable);
	
		for (Class<?> interf: clazz.getInterfaces())
			isIdenticallyRedPayableInSupertypesOf(interf, wasRedPayable);
	}
}
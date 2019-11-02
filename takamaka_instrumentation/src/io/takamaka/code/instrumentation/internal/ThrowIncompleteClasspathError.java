package io.takamaka.code.instrumentation.internal;

import io.takamaka.code.instrumentation.IncompleteClasspathError;

/**
 * Utilities for throwing an {@link io.takamaka.code.instrumentation.IncompleteClasspathError}
 * instead of a {@link java.lang.ClassNotFoundException}.
 */
public class ThrowIncompleteClasspathError {

	public interface Task {
		public void run() throws ClassNotFoundException;
	}

	public interface Computation<T> {
		public T run() throws ClassNotFoundException;
	}

	public static void insteadOfClassNotFoundException(Task task) {
		try {
			task.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}

	public static <T> T insteadOfClassNotFoundException(Computation<T> computation) {
		try {
			return computation.run();
		}
		catch (ClassNotFoundException e) {
			throw new IncompleteClasspathError(e);
		}
	}
}
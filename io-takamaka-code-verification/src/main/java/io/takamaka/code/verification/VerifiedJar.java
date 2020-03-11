package io.takamaka.code.verification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import io.takamaka.code.verification.internal.VerifiedJarImpl;
import io.takamaka.code.verification.issues.Issue;

/**
 * A jar that has undergone static verification, before being installed into blockchain.
 */
public interface VerifiedJar {

	/**
	 * Creates a verified jar from the given file. This verification
	 * might fail if at least a class did not verify. In that case, the issues generated
	 * during verification will contain at least an error.
	 * 
	 * @param origin the jar file to verify
	 * @param classLoader the class loader that can be used to resolve the classes of the program,
	 *                    including those of {@code origin}
	 * @param duringInitialization true if and only if verification occurs during
	 *                             blockchain initialization
	 * @throws IOException if there was a problem accessing the classes on disk
	 */
	static VerifiedJar of(Path origin, TakamakaClassLoader classLoader, boolean duringInitialization) throws IOException {
		return new VerifiedJarImpl(origin, classLoader, duringInitialization);
	}

	/**
	 * Determines if the verification of at least one class of the jar failed with an error.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean hasErrors();

	/**
	 * Yields the first error (hence not a warning) that occurred during the verification of the origin jar.
	 */
	Optional<io.takamaka.code.verification.issues.Error> getFirstError();

	/**
	 * Yields the stream of the classes of the jar that passed verification.
	 * 
	 * @return the classes, in increasing order
	 */
	Stream<VerifiedClass> classes();

	/**
	 * Yields the issues generated during the verification of the classes of the jar.
	 * 
	 * @return the issues, in increasing order
	 */
	Stream<Issue> issues();

	/**
	 * Yields the class loader used to load this jar.
	 * 
	 * @return the class loader
	 */
	TakamakaClassLoader getClassLoader();

	/**
	 * Yields the utility object that can be used to check the annotations in the methods in this jar.
	 * 
	 * @return the utility object
	 */
	Annotations getAnnotations();

	/**
	 * Yields the utility object that can be used to transform BCEL types into their corresponding
	 * Java class tag, by using the class loader of this jar.
	 * 
	 * @return the utility object
	 */
	BcelToClass getBcelToClass();
}
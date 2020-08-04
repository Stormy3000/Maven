package io.hotmoka.beans;

/**
 * A wrapper of an exception, raised during a transaction, that occurred during
 * the execution of a Takamaka constructor or method that was allowed to throw it.
 */
@SuppressWarnings("serial")
public class CodeExecutionException extends Exception {

	/**
	 * Builds the wrapper of an exception that occurred during the execution
	 * of a Takamaka constructor or method that was allowed to throw it.
	 * 
	 * @param classNameOfCause the name of the class of the cause of the exception
	 * @param messageOfCause the message of the cause of the exception. This might be {@code null}
	 * @param where a description of the program point of the exception. This might be {@code null}
	 */
	public CodeExecutionException(String classNameOfCause, String messageOfCause, String where) {
		super(classNameOfCause
			+ (messageOfCause.isEmpty() ? "" : (": " + messageOfCause))
			+ (where.isEmpty() ? "" : "@" + where));
	}

	/**
	 * Builds the wrapper of an exception that occurred during the execution
	 * of a Takamaka constructor or method that was allowed to throw it.
	 * 
	 * @param message the message of the exception
	 */
	public CodeExecutionException(String message) {
		super(message);
	}
}
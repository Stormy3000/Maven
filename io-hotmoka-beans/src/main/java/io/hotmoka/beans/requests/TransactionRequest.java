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

package io.hotmoka.beans.requests;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;

/**
 * A request of a transaction.
 * 
 * @param <R> the type of the response expected for this request
 */
@Immutable
public abstract class TransactionRequest<R extends TransactionResponse> extends Marshallable {

	/**
	 * Used to marshal requests that are specific to a node.
	 * After this selector, the qualified name of the request must follow.
	 */
	protected final static byte EXPANSION_SELECTOR = 12;

	/**
	 * The hashing algorithm for the requests.
	 */
	private final static MessageDigest HASHING_FOR_REQUESTS;

	/**
	 * The length of the hash of a transaction request.
	 */
	public final static int REQUEST_HASH_LENGTH = 32;

	static {
		try {
			HASHING_FOR_REQUESTS = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			throw new InternalFailureException("the hashing algorithm for the requests is not available");
		}
	}
	
	/**
	 * Factory method that unmarshals a request from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
	 * @throws IOException if the request could not be unmarshalled
	 * @throws ClassNotFoundException if the request could not be unmarshalled
	 */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		byte selector = context.readByte();
		switch (selector) {
		case ConstructorCallTransactionRequest.SELECTOR: return ConstructorCallTransactionRequest.from(context);
		case InitializationTransactionRequest.SELECTOR: return InitializationTransactionRequest.from(context);
		case InstanceMethodCallTransactionRequest.SELECTOR:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_INT:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_LONG:
		case InstanceMethodCallTransactionRequest.SELECTOR_TRANSFER_BIG_INTEGER:
			return InstanceMethodCallTransactionRequest.from(context, selector);
		case JarStoreInitialTransactionRequest.SELECTOR: return JarStoreInitialTransactionRequest.from(context);
		case JarStoreTransactionRequest.SELECTOR: return JarStoreTransactionRequest.from(context);
		case GameteCreationTransactionRequest.SELECTOR: return GameteCreationTransactionRequest.from(context);
		case StaticMethodCallTransactionRequest.SELECTOR: return StaticMethodCallTransactionRequest.from(context);
		case InstanceSystemMethodCallTransactionRequest.SELECTOR: return InstanceSystemMethodCallTransactionRequest.from(context);
		case EXPANSION_SELECTOR: {
			// this case deals with requests that only exist in a specific type of node;
			// hence their fully-qualified name must be available after the expansion selector

			String className = context.readUTF();
			Class<?> clazz = Class.forName(className, false, ClassLoader.getSystemClassLoader());

			// only subclass of TransactionRequest are considered, to block potential call injections
			if (!TransactionRequest.class.isAssignableFrom(clazz))
				throw new IOException("unknown request class " + className);

			Method from;
			try {
				from = clazz.getMethod("from", UnmarshallingContext.class);
			}
			catch (NoSuchMethodException | SecurityException e) {
				throw new IOException("cannot find method " + className + ".from(UnmarshallingContext)");
			}

			try {
				return (TransactionRequest<?>) from.invoke(null, context);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IOException("cannot call method " + className + ".from(UnmarshallingContext)");
			}
		}
		default: throw new IOException("unexpected request selector: " + selector);
		}
	}

	/**
	 * Yields the reference to the transaction generated by this request.
	 * 
	 * @return the transaction reference
	 */
	public final TransactionReference getReference() {
		try {
			byte[] bytes = toByteArray();

			synchronized (HASHING_FOR_REQUESTS) {
				HASHING_FOR_REQUESTS.reset();
				return new LocalTransactionReference(bytesToHex(HASHING_FOR_REQUESTS.digest(bytes)));
			}
		}
		catch(Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * The string of the hexadecimal digits.
	 */
	private final static String HEX_CHARS = "0123456789abcdef";

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = HEX_CHARS.getBytes();

	/**
	 * Unmarshals the signature from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the signature
	 * @throws IOException if the signature could not be unmarshalled
	 */
	protected static byte[] unmarshallSignature(UnmarshallingContext context) throws IOException {
		int signatureLength = context.readCompactInt();
		return context.readBytes(signatureLength, "signature length mismatch in request");
	}
}
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

package io.hotmoka.marshalling.internal;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import io.hotmoka.exceptions.UncheckedIOException;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * An object that can be marshaled into a stream, in a way
 * more compact than standard Java serialization. Typically,
 * this works because of context information about the structure
 * of the object.
 */
public abstract class MarshallableImpl implements Marshallable {

	@Override
	public final byte[] toByteArray() {
		return UncheckedIOException.wraps(() -> {
			try (var baos = new ByteArrayOutputStream(); var context = createMarshallingContext(baos)) {
				into(context);
				context.flush();
				return baos.toByteArray();
			}
		});
	}

	/**
	 * Marshals an array of marshallables into a given stream.
	 * 
	 * @param marshallables the array of marshallables
	 * @param context the context holding the stream
	 * @throws UncheckedIOException if some elements could not be marshalled
	 */
	protected static void intoArray(Marshallable[] marshallables, MarshallingContext context) {
		context.writeCompactInt(marshallables.length);

		for (Marshallable marshallable: marshallables)
			marshallable.into(context);
	}

	/**
	 * Creates a marshalling context for this object.
	 * 
	 * @param os the output stream of the context
	 * @return the default marshalling context. Subclasses may provide a more efficient context
	 * @throws UncheckedIOException if the marshalling context cannot be created
	 */
	protected MarshallingContext createMarshallingContext(OutputStream os) {
		return new MarshallingContextImpl(os);
	}
}
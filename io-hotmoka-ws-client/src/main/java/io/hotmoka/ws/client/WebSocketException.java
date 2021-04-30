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

package io.hotmoka.ws.client;

public class WebSocketException extends Exception {
	private static final long serialVersionUID = 6842969946302682805L;

	private final com.neovisionaries.ws.client.WebSocketException parent;

	private WebSocketException(com.neovisionaries.ws.client.WebSocketException parent) {
		this.parent = parent;
	}

	public static WebSocketException fromNative(com.neovisionaries.ws.client.WebSocketException parent) {
		if (parent == null)
			return null;
		else
			return new WebSocketException(parent);
	}

	@Override
	public String getMessage() {
		return parent.getMessage();
    }

	@Override
	public Throwable getCause() {
		return parent.getCause();
	}
}
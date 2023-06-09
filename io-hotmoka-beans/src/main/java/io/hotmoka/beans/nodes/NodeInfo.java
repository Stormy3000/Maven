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

package io.hotmoka.beans.nodes;

/**
 * Node-specific information about a Hotmoka node.
 */
public class NodeInfo {

	/**
	 * The type of the node.
	 */
	public final String type;

	/**
	 * The version of the node.
	 */
	public final String version;

	/**
	 * The identifier of the node inside its network, if any.
	 */
	public final String ID;

	/**
	 * Builds node-specific information about a Hotmoka node.
	 * 
	 * @param type the type of the node
	 * @param version the version of the node
	 * @param ID the identifier of the node inside its network, if any. Otherwise the empty string
	 */
	public NodeInfo(String type, String version, String ID) {
		if (type == null)
			throw new NullPointerException("type cannot be null");
		
		if (version == null)
			throw new NullPointerException("version cannot be null");

		if (ID == null)
			throw new NullPointerException("ID cannot be null");

		this.type = type;
		this.version = version;
		this.ID = ID;
	}
}
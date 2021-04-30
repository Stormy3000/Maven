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

module io.hotmoka.tendermint {
	exports io.hotmoka.tendermint;
	exports io.hotmoka.tendermint.views;
	exports io.hotmoka.tendermint.internal.beans to com.google.gson;
	requires io.hotmoka.tendermint.abci;
	requires io.hotmoka.beans;
	requires io.hotmoka.stores;
	requires transitive io.hotmoka.crypto;
	requires transitive io.hotmoka.nodes;
	requires transitive io.hotmoka.views;
	requires io.hotmoka.local;
	requires com.google.gson;
	requires com.google.protobuf;
	requires org.slf4j;
	requires org.bouncycastle.provider;
}
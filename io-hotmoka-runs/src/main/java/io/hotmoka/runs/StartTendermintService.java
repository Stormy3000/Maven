package io.hotmoka.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;


import io.hotmoka.nodes.Node;

public class StartTendermintService {

	/*private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	*//**
	 * Initial green stake.
	 *//*
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	*//**
	 * Initial red stake.
	 *//*
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().signRequestsWith("EMPTY").build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config);
			NodeService service = NodeService.of(networkConfig, blockchain)) {
			
			System.out.println("\nPress enter to turn off the server and exit this program");
			
			
			// update version number when needed
			TendermintInitializedNode.of(blockchain, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			TransactionReference takamakaCode = blockchain.getTakamakaCode();
			StorageReference manifest = blockchain.getManifest();

			System.out.println("Info about the network:");
			System.out.println("  takamakaCode: " + takamakaCode);
			System.out.println("  manifest: " + manifest);

			StorageReference gamete = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_GAMETE, manifest));
			System.out.println("    gamete: " + gamete);

			String chainId = ((StringValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;

			System.out.println("    chainId: " + chainId);

			StorageReference validators = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, CodeSignature.GET_VALIDATORS, manifest));

			System.out.println("    validators: " + validators);

			ClassType storageMapView = new ClassType("io.takamaka.code.util.StorageMapView");
			StorageReference shares = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(ClassType.VALIDATORS, "getShares", storageMapView), validators));

			int numOfValidators = ((IntValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "size", BasicTypes.INT), shares))).value;

			System.out.println("    number of validators: " + numOfValidators);

			for (int num = 0; num < numOfValidators; num++) {
				StorageReference validator = (StorageReference) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "select", ClassType.OBJECT, BasicTypes.INT), shares, new IntValue(num)));

				System.out.println("      validator #" + num + ": " + validator);

				String id = ((StringValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, CodeSignature.ID, validator))).value;

				System.out.println("        id: " + id);

				BigInteger power = ((BigIntegerValue) blockchain.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _10_000, takamakaCode, new NonVoidMethodSignature(storageMapView, "get", ClassType.OBJECT, ClassType.OBJECT), shares, validator))).value;

				System.out.println("        power: " + power);
			}
			
			System.console().readLine();
		}
	}*/
}
package io.hotmoka.tools.internal.cli;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Stream;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.remote.RemoteNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "install",
	description = "Installs a jar in a node",
	showDefaultValues = true)
public class Install extends AbstractCommand {

	@Option(names = { "--url" }, description = "the url of the node (without the protocol)", defaultValue = "localhost:8080")
    private String url;

	@Parameters(description = "the reference to the account that pays for the installation")
    private String payer;

	@Parameters(description = "the jar to install")
	private Path jar;

	@Option(names = { "--libs" }, description = "the references of the dependencies of the jar, already installed in the node (takamakaCode is automatically added)")
	private List<String> libs;

	@Option(names = "--classpath", description = "the classpath used to interpret the payer", defaultValue = "takamakaCode")
    private String classpath;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode") 
	private boolean nonInteractive;

	@Option(names = { "--gas-limit" }, description = "the gas limit used for the installation", defaultValue = "heuristic") 
	private String gasLimit;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {

		private Run() throws Exception {
			try (Node node = RemoteNode.of(remoteNodeConfig(url))) {
				TransactionReference takamakaCode = node.getTakamakaCode();
				StorageReference manifest = node.getManifest();
				StorageReference payer = new StorageReference(Install.this.payer);
				String chainId = ((StringValue) node.runInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
					(manifest, _100_000, takamakaCode, CodeSignature.GET_CHAIN_ID, manifest))).value;
				GasHelper gasHelper = new GasHelper(node);
				NonceHelper nonceHelper = new NonceHelper(node);
				byte[] bytes = Files.readAllBytes(jar);
				KeyPair keys = readKeys(payer);
				TransactionReference[] dependencies;
				if (libs != null)
					dependencies = Stream.concat(libs.stream().map(LocalTransactionReference::new), Stream.of(takamakaCode))
						.distinct().toArray(TransactionReference[]::new);
				else
					dependencies = new TransactionReference[] { takamakaCode };

				BigInteger gas = "heuristic".equals(gasLimit) ? _100_000.add(BigInteger.valueOf(100).multiply(BigInteger.valueOf(bytes.length))) : new BigInteger(gasLimit);
				TransactionReference classpath = "takamakaCode".equals(Install.this.classpath) ?
					takamakaCode : new LocalTransactionReference(Install.this.classpath);
				SignatureAlgorithm<SignedTransactionRequest> signature = SignatureAlgorithmForTransactionRequests.mk(node.getNameOfSignatureAlgorithmForRequests());

				askForConfirmation(gas);

				JarStoreTransactionRequest request = new JarStoreTransactionRequest(
						Signer.with(signature, keys),
						payer,
						nonceHelper.getNonceOf(payer),
						chainId,
						gas,
						gasHelper.getGasPrice(),
						classpath,
						bytes,
						dependencies);

				try {
					TransactionReference response = node.addJarStoreTransaction(request);
					System.out.println(jar + " has been installed at " + response);
				}
				finally {
					printCosts(node, request);
				}
			}
		}

		private void askForConfirmation(BigInteger gas) {
			if (!nonInteractive)
				yesNo("Do you really want to spend up to " + gas + " gas units to install the jar [Y/N] ");
		}
	}
}
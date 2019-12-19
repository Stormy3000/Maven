package io.takamaka.code.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.verification.VerifiedJar;

/**
 * A tool that parses and checks a jar. It performs the same verification that
 * Takamaka performs when a jar is added to blockchain.
 * 
 * Use it for instance like this:
 * 
 * java io.takamaka.code.tools.Verifier -app test_contracts_dependency.jar -lib takamaka_base.jar
 * 
 * The -lib are the dependencies that should already be in blockchain.
 */
public class Verifier {

	public static void main(String[] args) throws IOException {
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();

	    try {
	    	CommandLine line = parser.parse(options, args);
	    	String[] appJarNames = line.getOptionValues("app");
	    	String[] libJarNames = line.getOptionValues("lib");
	    	boolean duringInitialization = line.hasOption("init");

	    	for (String appJarName: appJarNames) {
		    	Path origin = Paths.get(appJarName);

		    	List<URL> urls = new ArrayList<>();
		    	urls.add(origin.toUri().toURL());
		    	if (libJarNames != null)
		    		for (String lib: libJarNames)
		    			urls.add(new File(lib).toURI().toURL());

		    	TakamakaClassLoader classLoader = TakamakaClassLoader.of(urls.toArray(new URL[urls.size()]));
		    	VerifiedJar verifiedJar = VerifiedJar.of(origin, classLoader, duringInitialization);
		    	verifiedJar.issues().forEach(System.err::println);
		    	if (verifiedJar.hasErrors())
		    		System.err.println("Verification failed because of errors");
		    	else
		    		System.out.println("Verification succeeded");
		    }
	    }
	    catch (ParseException e) {
	    	System.err.println("Syntax error: " + e.getMessage());
	    	new HelpFormatter().printHelp("java " + Verifier.class.getName(), options);
	    }
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(Option.builder("app").desc("verify the given application jars").hasArgs().argName("JARS").required().build());
		options.addOption(Option.builder("lib").desc("use the given library jars").hasArgs().argName("JARS").build());
		options.addOption(Option.builder("init").desc("verify as during blockchain initialization").build());

		return options;
	}
}
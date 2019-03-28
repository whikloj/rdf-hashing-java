package ca.umanitoba.dam.rdfhashing;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class HashCli {

    /**
     * @param fileLocation file uri.
     * @return the Jena model.
     * @throws FileNotFoundException file is not found.
     * @throws NoSuchAlgorithmException SHA-256 algorithm not available.
     */
    private static Model loadFromFile(final String fileLocation)
            throws FileNotFoundException, NoSuchAlgorithmException {
        final File rdfFile = new File(fileLocation);
        if (rdfFile.exists() && !rdfFile.isDirectory() && rdfFile.canRead()) {
            final Model graph = ModelFactory.createDefaultModel();
            graph.read(fileLocation);
            return graph;
        }
        return null;
    }

    /**
     * Print help, error message and exit.
     *
     * @param message the error message.
     * @param options the command line options.
     */
    private static void printHelpAndExit(final String message, final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        System.err.println(message);
        formatter.printHelp("java -jar rdf-hashing-VERSION-all.jar", options);
        System.exit(1);
    }

    /**
     * Load a RDF graph from a URL.
     *
     * @param sourceUrl the URL.
     * @return the Jena model
     */
    private static Model loadFromUrl(final String sourceUrl) {
        final Model graph = ModelFactory.createDefaultModel();
        graph.read(sourceUrl);
        return graph;
    }

    /**
     * The program.
     *
     * @param args program arguments.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     * @throws FileNotFoundException file is not found
     */
    public static void main(final String[] args) throws NoSuchAlgorithmException, FileNotFoundException {
        final Options options = new Options();
        final Option sourceOpt = new Option("s", "source", true, "Source of the RDF graph");
        sourceOpt.setRequired(true);
        options.addOption(sourceOpt);
        options.addOption(new Option("d", "debug", false, "Print the graph string before the hash"));

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            printHelpAndExit(e.getMessage(), options);
            return;
        }
        final String source = cmd.getOptionValue("source");
        final Model graph;
        if (source.startsWith("http")) {
            // A URL

            graph = loadFromUrl(source);
        } else {
            graph = loadFromFile(source);
        }
        if (graph != null) {
            if (cmd.hasOption("debug")) {
                final String graphString = RdfHash.getGraphString(graph);
                System.out.println(graphString);
            }
            final String hash = RdfHash.calculate(graph);
            System.out.println(hash);
        } else {
            System.err.println("No graph loaded");
        }

    }

}

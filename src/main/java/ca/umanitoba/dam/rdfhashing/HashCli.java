package ca.umanitoba.dam.rdfhashing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

public class HashCli {

    /**
     * Create a pre-emptive authorizing client.
     *
     * @param username the username
     * @param password the password
     * @return the authenticating client.
     */
    private static CloseableHttpClient createClient(final String username, final String password) {
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(username, password));
        return HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
    }

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
     * Load a RDF graph from a URL.
     *
     * @param sourceUrl the URL.
     * @return the Jena model
     * @throws IOException when opening response
     * @throws ClientProtocolException when executing GET
     */
    private static Model loadFromUrl(final String sourceUrl, final String username, final String password)
            throws ClientProtocolException, IOException {
        final CloseableHttpClient client = createClient(username, password);
        final HttpGet getReq = new HttpGet(sourceUrl);
        try (final CloseableHttpResponse response = client.execute(getReq)) {
            final String contentType;
            if (response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue().contains(";")) {
                contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                        .getValue().substring(0, response.getFirstHeader(HttpHeaders.CONTENT_TYPE)
                                .getValue().indexOf(";"));
            } else {
                contentType = response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue();
            }

            final Lang language = RDFLanguages.contentTypeToLang(contentType);
            final Model graph = ModelFactory.createDefaultModel();
            graph.read(response.getEntity().getContent(), sourceUrl, language.getName());
            return graph;
        }
    }

    /**
     * The program.
     *
     * @param args program arguments.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     * @throws IOException passed from loadFromUrl(sourceUrl, username, password)
     * @throws ClientProtocolException passed from loadFromUrl(sourceUrl, username, password)
     */
    public static void main(final String[] args) throws NoSuchAlgorithmException, ClientProtocolException,
            IOException {
        final Options options = new Options();
        final Option sourceOpt = new Option("s", "source", true, "Source of the RDF graph");
        sourceOpt.setRequired(true);
        options.addOption(sourceOpt);
        options.addOption(new Option("u", "username", true, "Username for http source (if required)"));
        options.addOption(new Option("p", "password", true, "Password for http source (if required)"));
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
            if ((cmd.hasOption("username") && !cmd.hasOption("password")) ||
                    (!cmd.hasOption("username") && cmd.hasOption("password"))) {
                printHelpAndExit("You must provide both --username and --password, or neither", options);
                return;
            }
            if (cmd.hasOption("username")) {
                graph = loadFromUrl(source, cmd.getOptionValue("username"), cmd.getOptionValue("password"));
            } else {
                graph = loadFromUrl(source);
            }
        } else {
            graph = loadFromFile(source);
        }
        if (graph != null) {
            if (cmd.hasOption("debug")) {
                final String graphString = RdfHash.getGraphString(graph);
                System.out.println(graphString);
            }
            final String hash = RdfHash.calculate(graph);
            System.out.print(hash);
        } else {
            System.err.println("No graph loaded");
        }

    }

}

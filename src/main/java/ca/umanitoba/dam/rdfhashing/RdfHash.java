package ca.umanitoba.dam.rdfhashing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class RdfHash {

    /**
     * Subject block prefix.
     */
    private static String SUBJECT_START = "{";

    /**
     * Subject block suffix.
     */
    private static String SUBJECT_END = "}";

    /**
     * Property block prefix.
     */
    private static String PROPERTY_START = "(";

    /**
     * Property block suffix.
     */
    private static String PROPERTY_END = ")";

    /**
     * Object block prefix.
     */
    private static String OBJECT_START = "[";

    /**
     * Object block suffix.
     */
    private static String OBJECT_END = "]";

    /**
     * Blank node constant.
     */
    private static String BLANK_NODE = "*";

    private static Set<String> visitedNodes;

    static {
        visitedNodes = new TreeSet<>();
    }

    /**
     * Calculate the SHA256 Hash of a graph.
     *
     * @param graph The graph.
     * @return The sha256 hash value.
     * @throws NoSuchAlgorithmException If there is no SHA-256 algorithm.
     */
    public static String calculate(final Model graph) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(getGraphString(graph).getBytes(UTF_8));

        return Base64.getEncoder().encodeToString(md.digest());

    }

    /**
     * Calculate the string definition of the graph.
     *
     * @param graph The graph.
     * @return The algorithm string.
     */
    public static String getGraphString(final Model graph)
    {

        final Set<String> subjectSet = new TreeSet<>();
        final List<Resource> subjects = graph.listSubjects().toList();

        for (final Resource resource : subjects) {
            visitedNodes.clear();
            final String encoded = encodeSubject(resource, graph);
            subjectSet.add(encoded);
        }

        final List<String> sortedSubjects = subjectSet.stream().sorted().collect(Collectors.toList());

        final List<String> result = new ArrayList<>();
        for (final String s : sortedSubjects) {
            result.add(SUBJECT_START + s + SUBJECT_END);
        }
        return String.join("", result);
    }

    /**
     * Encode a subject from the graph to a string.
     *
     * @param resource The subject resource.
     * @param visitedNodes Array of visited blank nodes.
     * @param graph The original graph.
     * @return The subject encoded as a string.
     */
    private static String encodeSubject(final Resource resource, final Model graph) {
        final String subjectResult;
        if (resource.isAnon()) {
            if (visitedNodes.contains(resource.getId().toString())) {
                return "";
            } else {
                visitedNodes.add(resource.getId().toString());
                subjectResult = BLANK_NODE;
            }
        } else {
            subjectResult = resource.getURI();
        }

        final String encodeProps = encodeProperties(resource, graph);
        return subjectResult + encodeProps;
    }

    /**
     * Encode the properties of a resource to a string.
     *
     * @param resource The subject resource.
     * @param visitedNodes Array of visited blank nodes.
     * @param graph The original graph
     * @return The properties encoded as a string.
     */
    private static String encodeProperties(final Resource resource, final Model graph) {

        final Set<Statement> all_properties = resource.listProperties().toSet();
        final List<Property> sorted_properties = all_properties.stream().map(t -> t.getPredicate()).distinct()
                .sorted((t1, t2) -> t1.getURI().compareToIgnoreCase(t2.getURI())).collect(Collectors
                .toList());

        final StringBuffer result = new StringBuffer();

        for (final Property property : sorted_properties) {
            final Set<String> objectStrings = new TreeSet<>();
            result.append(PROPERTY_START + property.getURI());
            final List<Statement> objectNodes = resource.listProperties(property).toList();
            for (final Statement object : objectNodes) {
                objectStrings.add(encodeObject(object.getObject(), graph));
            }
            final List<String> sortedObjects = objectStrings.stream().sorted().collect(Collectors.toList());
            for (final String object_string : sortedObjects) {
                result.append(OBJECT_START + object_string + OBJECT_END);
            }
            result.append(PROPERTY_END);
        }
        return result.toString();
    }

    /**
     * Encode the object of a property to a string.
     *
     * @param object The object to encode.
     * @param visitedNodes Array of visited blank nodes.
     * @param graph The original graph.
     * @return The object encoded as a string.
     */
    private static String encodeObject(final RDFNode object, final Model graph) {
        if (object.isLiteral()) {
            final String objLang = object.asLiteral().getLanguage();
            if (!objLang.isEmpty()) {
                return "\"" + object.asLiteral().getString() + "\"@" + objLang;
            } else {
                return "\"" + object.asLiteral().getString() + "\"";
            }
        } else if (object.isResource()) {
            if (object.isAnon()) {
                return encodeSubject(object.asResource(), graph);
            } else {
                return object.asResource().getURI();
            }
        }
        return "";
    }

    /**
     * @param fileLocation file uri.
     * @param baseUri base uri of the graph.
     * @param format format of the RDF.
     * @return the Jena model.
     * @throws FileNotFoundException file is not found.
     * @throws NoSuchAlgorithmException SHA-256 algorithm not available.
     */
    public static Model loadFromFile(final String fileLocation, final String baseUri, final String format)
            throws FileNotFoundException, NoSuchAlgorithmException {
        final File rdfFile = new File(fileLocation);
        if (rdfFile.exists() && !rdfFile.isDirectory() && rdfFile.canRead()) {
            final InputStream graphStream = new FileInputStream(rdfFile);
            final Model graph = ModelFactory.createDefaultModel();
            graph.read(graphStream, baseUri, format);
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
        formatter.printHelp("RdfHash", options);
        System.exit(1);
    }

    /**
     * The program.
     *
     * @param args program arguments.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static void main(final String[] args) throws FileNotFoundException, NoSuchAlgorithmException {
        final Options options = new Options();
        final Option sourceOpt = new Option("s", "source", true, "Source of the RDF graph");
        sourceOpt.setRequired(true);

        options.addOption(sourceOpt);
        options.addOption(new Option("b", "baseuri", true, "Base URI (for file parsing)"));
        options.addOption(new Option("f", "format", true, "RDF format (for file parsing)"));

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
            graph = null;
        } else {
            // A file
            final String baseuri = cmd.getOptionValue("baseuri");
            if (baseuri == null) {
                printHelpAndExit("--baseuri is required for file source", options);
                return;
            }
            final String format = cmd.getOptionValue("format");
            if (format == null) {
                printHelpAndExit("--format is required for file source", options);
                return;
            }
            graph = loadFromFile(source, baseuri, format);
        }
        if (graph != null) {
            final String hash = calculate(graph);
            System.out.println(String.format("Hash of graph is %s", hash));
        } else {
            System.err.println("No graph loaded");
        }

    }

}

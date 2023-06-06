package ca.umanitoba.dam.rdfhashing;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class RdfHash {

    /**
     * Subject block prefix.
     */
    private static final String SUBJECT_START = "{";

    /**
     * Subject block suffix.
     */
    private static final String SUBJECT_END = "}";

    /**
     * Property block prefix.
     */
    private static final String PROPERTY_START = "(";

    /**
     * Property block suffix.
     */
    private static final String PROPERTY_END = ")";

    /**
     * Object block prefix.
     */
    private static final String OBJECT_START = "[";

    /**
     * Object block suffix.
     */
    private static final String OBJECT_END = "]";

    /**
     * Blank node constant.
     */
    private static final String BLANK_NODE = "*";

    /**
     * The currently visited nodes for any resource.
     */
    private static final Set<String> visitedNodes;

    static {
        visitedNodes = new TreeSet<>();
    }

    /**
     * Calculate the SHA256 Hash of a graph.
     *
     * @param graph The graph.
     * @return The sha256 hexidecimal hash value.
     * @throws NoSuchAlgorithmException If there is no SHA-256 algorithm.
     */
    public static String calculate(final Model graph) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(getGraphString(graph).getBytes(UTF_8));
        return String.format("%064x", new BigInteger(1, md.digest()));

    }

    /**
     * Calculate the string definition of the graph.
     *
     * @param graph The graph.
     * @return The algorithm string.
     */
    public static String getGraphString(final Model graph) {
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
     * @param graph The original graph
     * @return The properties encoded as a string.
     */
    private static String encodeProperties(final Resource resource, final Model graph) {

        final Set<Statement> all_properties = resource.listProperties().toSet();
        final List<Property> sorted_properties = all_properties.stream().map(Statement::getPredicate).distinct()
                .sorted((t1, t2) -> t1.getURI().compareToIgnoreCase(t2.getURI())).collect(Collectors
                .toList());

        final StringBuilder result = new StringBuilder();

        for (final Property property : sorted_properties) {
            final Set<String> objectStrings = new TreeSet<>();
            result.append(PROPERTY_START).append(property.getURI());
            final List<Statement> objectNodes = resource.listProperties(property).toList();
            for (final Statement object : objectNodes) {
                objectStrings.add(encodeObject(object.getObject(), graph));
            }
            final List<String> sortedObjects = objectStrings.stream().sorted().collect(Collectors.toList());
            for (final String object_string : sortedObjects) {
                result.append(OBJECT_START).append(object_string).append(OBJECT_END);
            }
            result.append(PROPERTY_END);
        }
        return result.toString();
    }

    /**
     * Encode the object of a property to a string.
     *
     * @param object The object to encode.
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

}

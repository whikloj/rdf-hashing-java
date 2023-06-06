package ca.umanitoba.dam.rdfhashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.apache.jena.util.FileUtils.langTurtle;
import static org.apache.jena.util.FileUtils.langNTriple;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;


public class RdfHashTest {

    @Test
    public void testSuperSimple() throws Exception {
        doTest("supersimple.ttl", "supersimple.txt", "http://example.org/test1", langTurtle);
    }

    @Test
    public void testBaseGraph() throws Exception {
        doTest("base_graph.ttl", "base_graph.txt", "http://example.org/test2", langTurtle);
    }

    @Test
    public void testDoap() throws Exception {
        final Model graphTurtle = getFromFile("doap.ttl", "http://example.org/test3/ttl", langTurtle);
        assertNotNull(graphTurtle, "Could not get Turtle graph");
        final Model graphNTriples = getFromFile("doap.nt", "http://example.org/test3/nt", langNTriple);
        assertNotNull(graphNTriples, "Could not get N-Triple graph");
        final String turtleHash = RdfHash.calculate(graphTurtle);
        final String ntriplesHash = RdfHash.calculate(graphNTriples);
        assertEquals(turtleHash, ntriplesHash, "Hashes do not match");
    }

    @Test
    public void testLanguageTags() throws Exception {
        final Model lang1 = getFromFile("language_tags1.ttl", "http://example.org/test4/ttl1", langTurtle);
        final Model lang2 = getFromFile("language_tags2.ttl", "http://example.org/test4/ttl2", langTurtle);
        final Model lang3 = getFromFile("language_tags3.ttl", "http://example.org/test4/ttl3", langTurtle);
        assertNotNull(lang1);
        assertNotNull(lang2);
        assertNotNull(lang3);
        final String hash1 = RdfHash.calculate(lang1);
        final String hash2 = RdfHash.calculate(lang2);
        final String hash3 = RdfHash.calculate(lang3);
        assertEquals(hash1, hash2);
        assertEquals(hash1, hash3);
    }

    /**
     * Test runner
     *
     * @param source name of the source rdf file.
     * @param expected name of the final string file.
     * @param baseUri baseUri of the source rdf.
     * @param format format name of the rdf.
     * @throws IOException on error opening stream.
     */
    private void doTest(final String source, final String expected, final String baseUri, final String format)
            throws IOException {
        final Model originalGraph = getFromFile(source, baseUri, format);
        assertNotNull(originalGraph);
        final String graphString = RdfHash.getGraphString(originalGraph);
        final String expectedString = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(
                expected),
                Charsets.UTF_8.toString());
        assertEquals(expectedString, graphString, "Graph String does not match expected");
    }

    /**
     * Load Jena model from a file
     *
     * @param rdfFile the name of the source file.
     * @param baseUri the base uri of the rdf
     * @param format the format name of the rdf
     * @return the Jena Model
     */
    private Model getFromFile(final String rdfFile, final String baseUri, final String format) {
        final InputStream graphStream = this.getClass().getClassLoader().getResourceAsStream(rdfFile);
        final Model graph = ModelFactory.createDefaultModel();
        graph.read(graphStream, baseUri, format);
        return graph;
    }
}

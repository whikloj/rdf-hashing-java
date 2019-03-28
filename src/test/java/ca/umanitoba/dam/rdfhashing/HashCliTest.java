package ca.umanitoba.dam.rdfhashing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.HttpException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

public class HashCliTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private final PrintStream originalOut = System.out;

    private final PrintStream originalErr = System.err;

    private final URL simpleFileUrl;

    private final String simpleFile;

    private final String simpleFileHash = "c3f2f988a2e339eb6622ba2fe0d6452fffb1b123fed947ba66900d89b6e3ab5c";

    private final String simpleFileGraph =
            "{*(http://ex#pred[*(http://ex#pred[http://ex#A][http://ex#C])][http://ex" +
                    "#C])}{*(http://ex#pred[*(http://ex#pred[http://ex#B][http://ex#C])][http://ex#C])}{*(http://ex#pred[htt" +
                    "p://ex#A][http://ex#C])}{*(http://ex#pred[http://ex#B][http://ex#C])}\n";

    private static final WireMockServer webService = new WireMockServer(options().dynamicPort());

    /**
     * Constructor
     */
    public HashCliTest() {
        simpleFileUrl = this.getClass().getClassLoader().getResource("supersimple.ttl");
        simpleFile = simpleFileUrl.toExternalForm().replace(simpleFileUrl.getProtocol() + ":", "");
    }

    @BeforeAll
    public static void setUp() {
        webService.start();
    }

    @AfterAll
    public static void shutDown() {
        webService.stop();
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @BeforeEach
    public void init() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @Test
    public void testNoSource() throws Exception {
        final String[] testArgs = new String[0];
        HashCli.main(testArgs);
        assertEquals("Missing required option: s\n", errContent.toString(), "Did not get error");
    }

    @Test
    public void testLoadFile() throws Exception {
        final String[] testArgs = new String[] {
            "--source",
            simpleFile
        };
        HashCli.main(testArgs);
        assertEquals(simpleFileHash, outContent.toString(),
                "Did not get hash");
    }

    @Test
    public void testLoadFileDebug() throws Exception {
        final String[] testArgs = new String[] {
            "--source",
            simpleFile,
            "--debug"
        };
        HashCli.main(testArgs);
        assertEquals(simpleFileGraph + simpleFileHash, outContent.toString(),
                "Did not get hash");
    }

    @Test
    public void testFileNotExist() throws Exception {
        final String[] testArgs = new String[] {
            "--source",
            simpleFile.substring(0, simpleFile.length() - 1)
        };
        HashCli.main(testArgs);
        assertEquals("No graph loaded\n", errContent.toString());
    }

    @Test
    public void testLoadUrl() throws Exception {
        final String body = IOUtils.toString(simpleFileUrl.openStream(), Charsets.UTF_8);
        webService.stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "text/turtle")
                        .withBody(body)
                ));

        final String[] testArgs = new String[2];
        testArgs[0] = "-s";
        testArgs[1] = "http://localhost:" + webService.port() + "/some/thing";
        HashCli.main(testArgs);
        assertEquals(simpleFileHash, outContent.toString());
    }

    @Test
    public void testLoadUrlWithCharset() throws Exception {
        final String body = IOUtils.toString(simpleFileUrl.openStream(), Charsets.UTF_8);
        webService.stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "text/turtle;charset=UTF-8")
                        .withBody(body)));

        final String[] testArgs = new String[2];
        testArgs[0] = "-s";
        testArgs[1] = "http://localhost:" + webService.port() + "/some/thing";
        HashCli.main(testArgs);
        assertEquals(simpleFileHash, outContent.toString());
    }

    @Test
    public void testLoadUriNotAuthorized() throws Exception {
        webService.stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withStatusMessage("Not Authorized")));
        final String[] testArgs = new String[] {
            "--source",
            "http://localhost:" + webService.port() + "/some/thing"
        };
        assertThrows(HttpException.class,
                () -> HashCli.main(testArgs),
                "Expected an HttpException() to throw, but it didn't");
    }

    @Test
    public void testLoadUrlAuthenticated() throws Exception {
        final String body = IOUtils.toString(simpleFileUrl.openStream(), Charsets.UTF_8);
        webService.stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "text/turtle")
                        .withBody(body)));

        final String[] testArgs = new String[] {
            "--source",
            "http://localhost:" + webService.port() + "/some/thing",
            "--username",
            "testuser",
            "--password",
            "testpassword"
        };
        HashCli.main(testArgs);
        assertEquals(simpleFileHash, outContent.toString());
    }

    @Test
    public void testLoadUrlAuthenticatedWithCharset() throws Exception {
        final String body = IOUtils.toString(simpleFileUrl.openStream(), Charsets.UTF_8);
        webService.stubFor(get(urlEqualTo("/some/thing"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "text/turtle;charset=UTF-8")
                        .withBody(body)));

        final String[] testArgs = new String[] {
            "--source",
            "http://localhost:" + webService.port() + "/some/thing",
            "--username",
            "testuser",
            "--password",
            "testpassword"
        };
        HashCli.main(testArgs);
        assertEquals(simpleFileHash, outContent.toString());
    }

    @Test
    public void testLoadMissingPassword() throws Exception {
        final String[] testArgs = new String[] {
            "--source",
            "http://localhost:" + webService.port() + "/some/thing",
            "--username",
            "testuser",
        };
        HashCli.main(testArgs);
        assertEquals("You must provide both --username and --password, or neither\n", errContent.toString());
    }

    @Test
    public void testLoadMissingUsername() throws Exception {
        final String[] testArgs = new String[] {
            "--source",
            "http://localhost:" + webService.port() + "/some/thing",
            "--password",
            "testpassword",
        };
        HashCli.main(testArgs);
        assertEquals("You must provide both --username and --password, or neither\n", errContent.toString());
    }

}

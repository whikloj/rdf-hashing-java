[![travis](https://api.travis-ci.org/whikloj/java-rdf-hashing.svg?branch=master)](https://travis-ci.org/whikloj/java-rdf-hashing)
[![codecov](https://codecov.io/gh/whikloj/rdf-hashing-java/branch/master/graph/badge.svg)](https://codecov.io/gh/whikloj/rdf-hashing-java)


## Introduction

Based off the work of [@barmintor](https://github.com/barmintor)'s [rdf-digest](https://github.com/barmintor/rdf-digest) and my own implementation [RdfHashing](https://github.com/whikloj/RdfHashing)

java-rdf-hashing is a Java 8 implementation of the Höfig/Schieferdecker RDF hashing algorithm described in [Hashing of RDF Graphs
and a Solution to the Blank Node Problem](http://ceur-ws.org/Vol-1259/method2014_submission_1.pdf).

It generates a specifically formatted string based on the above paper and then a SHA-256 hash of that string.

## Installation

1. Clone this repository down
1. Build with using gradle wrapper

```bash
./gradlew build
```

## Usage

Once you have run the `gradlew` command you will have several **jar** files in the `build/libs` directory.

Beside the sources and javadoc jars are the jars for the two methods of use.

### Library

`rdf-hashing-VERSION.jar` is just the library code and can be used to include this tool in your code.

The `RdfHash` class has two static functions.

* `RdfHash.calculate(model)` takes a Jena Model and returns the hexadecimal sha256 hash for it.
* `RdfHash.getGraphString(model)` takes a Jena Model and returns the parsed formatted string of the graph ready for generating the hash.

```java

package default;

import static org.apache.jena.util.FileUtils.langTurtle;

import java.io.InputStream;
import ca.umanitoba.dam.rdfhashing.RdfHash;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

class Example {

    public static void main(final String[] args) {
        final File graphFile = new File("the-graph.ttl");

        final InputStream graphStream = new FileInputStream(graphFile);

        final Model graph = ModelFactory.createDefaultModel();
        graph.read(graphStream, "http://example.org/object", langTurtle);

        final String rdf_hash = RdfHash.calculate(graph);
   }
}
```

### Command line

`rdf-hashing-VERSION-all.jar` is a shadow jar containing all dependencies which allows you to access a command line interface.

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar
Missing required option: s
usage: java -jar rdf-hashing-VERSION-all.jar
 -d,--debug            Print the graph string before the hash
 -p,--password <arg>   Password for http source (if required)
 -s,--source <arg>     Source of the RDF graph
 -u,--username <arg>   Username for http source (if required)
```

Passing a source HTTP URI or file path to the tool it will attempt to retrieve and parse the graph and then provide the hash.

#### Passing a file

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s ./src/test/resources/supersimple.ttl
c3f2f988a2e339eb6622ba2fe0d6452fffb1b123fed947ba66900d89b6e3ab5c
```

You can also pass the `-d|--debug` argument to see the graph string before it is hashed.

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s ./src/test/resources/supersimple.ttl --debug
{*(http://ex#pred[*(http://ex#pred[http://ex#A][http://ex#C])][http://ex#C])}{*(http://ex#pred[*(http://ex#pred[http://ex#B][http://ex#C])][http://ex#C])}{*(http://ex#pred[http://ex#A][http://ex#C])}{*(http://ex#pred[http://ex#B][http://ex#C])}
c3f2f988a2e339eb6622ba2fe0d6452fffb1b123fed947ba66900d89b6e3ab5c
```

#### Passing a URL

If your website requires a authentication and you do not pass the `--username` and `--password` parameters you will receive an Exception.

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C/fcr:metadata
Exception in thread "main" org.apache.jena.atlas.web.HttpException: 401 - Unauthorized
	at org.apache.jena.riot.web.HttpOp.exec(HttpOp.java:1091)
	at org.apache.jena.riot.web.HttpOp.execHttpGet(HttpOp.java:308)
	at org.apache.jena.riot.web.HttpOp.execHttpGet(HttpOp.java:367)
	at org.apache.jena.riot.RDFParser.openTypedInputStream(RDFParser.java:335)
	at org.apache.jena.riot.RDFParser.parseURI(RDFParser.java:247)
	at org.apache.jena.riot.RDFParser.parse(RDFParser.java:241)
	at org.apache.jena.riot.RDFParserBuilder.parse(RDFParserBuilder.java:417)
	at org.apache.jena.riot.RDFDataMgr.parseFromURI(RDFDataMgr.java:890)
	at org.apache.jena.riot.RDFDataMgr.read(RDFDataMgr.java:221)
	at org.apache.jena.riot.RDFDataMgr.read(RDFDataMgr.java:190)
	at org.apache.jena.riot.RDFDataMgr.read(RDFDataMgr.java:120)
	at org.apache.jena.riot.RDFDataMgr.read(RDFDataMgr.java:111)
	at org.apache.jena.riot.adapters.RDFReaderRIOT.read(RDFReaderRIOT.java:76)
	at org.apache.jena.rdf.model.impl.ModelCom.read(ModelCom.java:263)
	at ca.umanitoba.dam.rdfhashing.HashCli.loadFromUrl(HashCli.java:86)
	at ca.umanitoba.dam.rdfhashing.HashCli.main(HashCli.java:156)
```

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C/fcr:metadata -utestuser -ptestpass
5b8a0152edd72ae3eda2941a78b25d24789ba46cb9e685d56749fcc5b2bfed34
```

You can also pass the `-d|--debug` argument to see the graph string before it is hashed.

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C/fcr:metadata -utestuser -ptestpass -d
{http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C(http://fedora.info/definitions/v4/repository#created["2019-02-01T14:24:10.615Z"])(http://fedora.info/definitions/v4/repository#createdBy["fedoraAdmin"])(http://fedora.info/definitions/v4/repository#hasFixityService[http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C/fcr:fixity])(http://fedora.info/definitions/v4/repository#lastModified["2019-02-01T14:24:10.615Z"])(http://fedora.info/definitions/v4/repository#lastModifiedBy["fedoraAdmin"])(http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#filename[""])(http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#hasMimeType["text/plain;charset=ISO-8859-1"])(http://www.iana.org/assignments/relation/describedby[http://localhost:8080/rest/testsuitecontainer020120190823574.1.2-C/fcr:metadata])(http://www.loc.gov/premis/rdf/v1#hasMessageDigest[urn:sha1:a94a8fe5ccb19ba61c4c0873d391e987982fbbd3])(http://www.loc.gov/premis/rdf/v1#hasSize["4"])(http://www.w3.org/1999/02/22-rdf-syntax-ns#type[http://fedora.info/definitions/v4/repository#Binary][http://fedora.info/definitions/v4/repository#NonRdfSourceDescription][http://fedora.info/definitions/v4/repository#Resource][http://www.w3.org/ns/ldp#NonRDFSource])}
5b8a0152edd72ae3eda2941a78b25d24789ba46cb9e685d56749fcc5b2bfed34
```

### License

* MIT
 

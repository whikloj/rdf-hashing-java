[![travis](https://api.travis-ci.org/whikloj/java-rdf-hashing.svg?branch=master)](https://travis-ci.org/whikloj/java-rdf-hashing)

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
usage: RdfHash
 -b,--baseuri <arg>   Base URI (for file parsing)
 -f,--format <arg>    RDF format (for file parsing)
 -s,--source <arg>    Source of the RDF graph
```

Passing a source HTTP URI to the tool it will attempt to retrieve and parse the graph and then provide the hash.

Passing a source file path to the tool, will also require a jena format (values found [here](https://jena.apache.org/documentation/javadoc/jena/constant-values.html#org.apache.jena.util.FileUtils.langXML)) and a base URI (this can be anything and I'll probably remove the requirement).

```bash
> java -jar build/libs/rdf-hashing-0.0.1-all.jar -s ./src/test/resources/supersimple.ttl -b http://example.org/object1 -f TURTLE
Hash of graph is w/L5iKLjOetmIrov4NZFL/+xsSP+2Ue6ZpANibbjq1w=
```    

### License

* MIT
 

[![travis](https://api.travis-ci.org/whikloj/java-rdf-hashing.svg?branch=master)](https://travis-ci.org/whikloj/java-rdf-hashing)

## Introduction

Based off the work of [@barmintor](https://github.com/barmintor)'s [rdf-digest](https://github.com/barmintor/rdf-digest) and my own implementation [RdfHashing](https://github.com/whikloj/RdfHashing)

java-rdf-hashing is a Java 8 implementation of the Höfig/Schieferdecker RDF hashing algorithm described in [Hashing of RDF Graphs
and a Solution to the Blank Node Problem](http://ceur-ws.org/Vol-1259/method2014_submission_1.pdf).

It generates a specifically formatted string based on the above paper and then a SHA-256 hash of that string.

### Installation

Install using gradle wrapper

```bash
./gradlew clean build
```

### Usage

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

### License

* MIT
 

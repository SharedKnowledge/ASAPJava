# ASAPJava
Asynchronous Semantic Ad-hoc Protocol (ASAP) engine for Java

# Maven Guide
## Compile and run tests
```
mvn clean package
```
This compiles all files, runs tests and, if they succeed, compiles the jar. The compiled jar (ASAPJava-{Version}.jar) will be in your target/ folder.

## Compile without tests
This is needed in case you want to build the jar while your tests fail (which currently, they do fail).
```
mvn clean package -DskipTests
```

# Wiki
Visit [the wiki](https://github.com/SharedKnowledge/ASAPJava/wiki) for all other information on what ASAP is and how to use it.

#!/usr/bin/env bash
mvn clean package
rm -rf *.arff
rm -rf Kemik42bin
rm -rf Milliyet405bin
rm -rf MIL
rm -rf TRT
rm -rf TTC3600
rm -rf AOF
java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -server -Xms18g -Xmx18g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.App
java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -server -Xms18g -Xmx18g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.ListCompoundWords
java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -server -Xms18g -Xmx18g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.Typo
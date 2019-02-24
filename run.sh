#!/usr/bin/env bash
mvn clean package
rm -rf *.arff
rm -rf Kemik42bin
rm -rf Milliyet405bin
rm -rf MIL
rm -rf TRT
rm -rf TTC3600
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.App
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.ListCompoundWords
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.Typo
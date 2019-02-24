#!/usr/bin/env bash
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.App
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.ListCompoundWords
java -server -Xms8g -Xmx8g -cp target/Kemik-1.0-SNAPSHOT.jar edu.anadolu.Typo
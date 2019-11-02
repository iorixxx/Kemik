#!/usr/bin/env bash

method[1]=weka.classifiers.trees.RandomForest
acronym[1]=RF

method[2]=weka.classifiers.bayes.NaiveBayes
acronym[2]=NB

method[3]=weka.classifiers.trees.J48
acronym[3]=J48

method[4]=weka.classifiers.functions.SMO
acronym[4]=SMO

rm -rf vectors
rm -rf reults
mkdir -p vectors
mkdir -p results

for set in MIL TRT TTC3600; do
    for f in ${set}*.arff; do

    echo ${f}
    name=$(echo "$f" | cut -f 1 -d '.')
    echo "${name}.txt"
    echo "vectors/${f##/*/}"


    java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -server -Xms18g -Xmx18g -cp weka.jar weka.filters.unsupervised.attribute.StringToWordVector -i ${f} > "vectors/${f##/*/}"

    for i in {1..4}; do

    echo "results/${name}_${acronym[${i}]}.txt"
    java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -server -Xms18g -Xmx18g -cp weka.jar ${method[${i}]} -c 1 -x 10 -t "vectors/${f##/*/}" > "results/${name}_${acronym[${i}]}.txt"

    done
    done
done
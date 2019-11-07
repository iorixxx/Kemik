#!/usr/bin/env bash

for set in MIL; do

    for f in results/${set}*.txt; do
    name=$(echo "${f##*/}" | cut -f 1 -d '.')

    res=$(grep 'Weighted Avg.' ${f} | tail -1 |cut -c 18-)
    echo "${name}"$'\t'${res}

    done
done
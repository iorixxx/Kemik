package edu.anadolu;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.util.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompoundCharFilterFactory extends CharFilterFactory implements ResourceLoaderAware, MultiTermAwareComponent {

    private NormalizeCharMap normMap = null;
    private final String mapping;
    private final boolean decompose;

    /**
     * Creates a new CompoundCharFilterFactory
     */
    public CompoundCharFilterFactory(Map<String, String> args) {
        super(args);
        mapping = require(args, "mapping");
        decompose = getBoolean(args, "decompose", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {

        Set<String> wlist = new HashSet<>();
        List<String> files = splitFileNames(mapping);

        for (String file : files) {
            List<String> lines = getLines(loader, file.trim());
            wlist.addAll(lines);
        }

        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();

        for (String s : wlist) {

            String[] parts = s.split("_");

            if (decompose)
                builder.add(String.join("", parts), String.join(" ", parts));
            else
                builder.add(String.join(" ", parts), String.join("", parts));
        }

        normMap = builder.build();
    }

    @Override
    public Reader create(Reader input) {
        return normMap == null ? input : new MappingCharFilter(normMap, input);
    }

    @Override
    public AbstractAnalysisFactory getMultiTermComponent() {
        return this;
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.filesgenerator.site;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Reads and validates the site redirect registry. */
final class RedirectRegistry {

    static final String FRAGMENT_PLACEHOLDER = "{fragment-lower}";

    private static final String FRAGMENT_RULE_SUFFIX = "#*";

    private static final Pattern SITE_PAGE = Pattern.compile(
            "^/(?:[A-Za-z0-9][A-Za-z0-9._-]*/)*[A-Za-z0-9][A-Za-z0-9._-]*\\.html$");

    private RedirectRegistry() {
    }

    /**
     * Loads and validates redirect rules.
     *
     * @param registryFile redirect registry
     * @return redirects sorted by source path
     * @throws IOException when the registry cannot be read
     */
    static List<PageRedirect> load(Path registryFile) throws IOException {
        final Properties properties = loadProperties(registryFile);
        final Map<String, String> destinations = new TreeMap<>();
        final Map<String, String> fragmentDestinations = new TreeMap<>();

        for (String key : properties.stringPropertyNames()) {
            final String destination = properties.getProperty(key).trim();

            if (key.endsWith(FRAGMENT_RULE_SUFFIX)) {
                final String source = key.substring(
                        0, key.length() - FRAGMENT_RULE_SUFFIX.length());

                validateFragmentRule(source, destination);
                fragmentDestinations.put(source, destination);
            }
            else {
                validateDefaultRule(key, destination);
                destinations.put(key, destination);
            }
        }

        validateFragmentRulesHaveDefaults(destinations, fragmentDestinations);

        return createPageRedirects(destinations, fragmentDestinations);
    }

    private static Properties loadProperties(Path registryFile) throws IOException {
        final Properties properties = new Properties();

        try (Reader reader =
                Files.newBufferedReader(registryFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        return properties;
    }

    private static void validateDefaultRule(String source, String destination) {
        validateSitePage("source", source);
        validateSitePage("destination", destination);

        if (destination.contains(FRAGMENT_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                    "Default redirect cannot contain " + FRAGMENT_PLACEHOLDER + ": " + source);
        }
    }

    private static void validateFragmentRule(String source, String destination) {
        validateSitePage("source", source);

        if (countOccurrences(destination, FRAGMENT_PLACEHOLDER) != 1) {
            throw new IllegalArgumentException(
                    "Fragment redirect must contain exactly one "
                            + FRAGMENT_PLACEHOLDER
                            + ": "
                            + source
                            + FRAGMENT_RULE_SUFFIX);
        }

        validateSitePage(
                "fragment destination",
                destination.replace(FRAGMENT_PLACEHOLDER, "fragment"));
    }

    private static void validateSitePage(String name, String path) {
        if (!SITE_PAGE.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid redirect " + name + ": " + path);
        }
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);

        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }

        return count;
    }

    private static void validateFragmentRulesHaveDefaults(
            Map<String, String> destinations,
            Map<String, String> fragmentDestinations) {

        for (String source : fragmentDestinations.keySet()) {
            if (!destinations.containsKey(source)) {
                throw new IllegalArgumentException(
                        "Fragment redirect requires a default redirect: " + source);
            }
        }
    }

    private static List<PageRedirect> createPageRedirects(
            Map<String, String> destinations,
            Map<String, String> fragmentDestinations) {

        final List<PageRedirect> redirects =
                new ArrayList<>(destinations.size());

        for (Map.Entry<String, String> entry : destinations.entrySet()) {
            final String source = entry.getKey();

            redirects.add(new PageRedirect(
                    source,
                    entry.getValue(),
                    fragmentDestinations.get(source)));
        }

        return List.copyOf(redirects);
    }
}

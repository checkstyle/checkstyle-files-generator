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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Generates XDoc redirect pages from Checkstyle's redirect registry. */
public final class SiteRedirectGenerator {

    private SiteRedirectGenerator() {
    }

    /**
     * Generates redirect XDocs.
     *
     * @param checkstylePath path to the Checkstyle checkout
     * @throws IOException when redirect files cannot be read or written
     */
    public static void generate(Path checkstylePath) throws IOException {
        final Path root = checkstylePath.toAbsolutePath().normalize();
        final Path registryFile =
                root.resolve(Path.of("src", "site", "redirects.properties"));
        final Path xdocDirectory =
                root.resolve(Path.of("src", "site", "xdoc"));
        final Path generatedSiteDirectory =
                root.resolve(Path.of("target", "generated-site"));
        final Path outputDirectory =
                generatedSiteDirectory.resolve("xdoc");

        final List<PageRedirect> redirects = RedirectRegistry.load(registryFile);

        validateSourcesDoNotCollide(xdocDirectory, redirects);
        validateDestinationsExist(xdocDirectory, redirects);

        for (PageRedirect redirect : redirects) {
            writePageRedirect(outputDirectory, redirect);
        }
    }

    private static void validateSourcesDoNotCollide(
            Path xdocDirectory, List<PageRedirect> redirects) {
        for (PageRedirect redirect : redirects) {
            final Path source = toXdocPath(xdocDirectory, redirect.source());

            if (xdocSourceExists(source)) {
                throw new IllegalArgumentException(
                        "Redirect source collides with an authored XDoc: " + redirect.source());
            }
        }
    }

    private static void validateDestinationsExist(
            Path xdocDirectory, List<PageRedirect> redirects) {
        for (PageRedirect redirect : redirects) {
            validateDestinationExists(xdocDirectory, redirect.destination());

            if (redirect.hasFragmentRedirect()) {
                validateFragmentDestinationDirectoryExists(
                        xdocDirectory, redirect.fragmentDestination());
            }
        }
    }

    private static void validateDestinationExists(Path xdocDirectory, String destination) {
        final Path xdoc = toXdocPath(xdocDirectory, destination);

        if (!xdocSourceExists(xdoc)) {
            throw new IllegalArgumentException(
                    "Redirect destination does not exist: " + destination);
        }
    }

    private static void validateFragmentDestinationDirectoryExists(
            Path xdocDirectory, String destination) {
        final String exampleDestination = destination.replace(
                RedirectRegistry.FRAGMENT_PLACEHOLDER,
                "fragment");

        final Path xdoc = toXdocPath(xdocDirectory, exampleDestination);

        if (!Files.isDirectory(xdoc.getParent())) {
            throw new IllegalArgumentException(
                    "Fragment redirect destination directory does not exist: " + destination);
        }
    }

    private static Path toXdocPath(Path xdocDirectory, String sitePage) {
        final String relativePath = sitePage
                .substring("/".length(), sitePage.length() - ".html".length());

        return xdocDirectory.resolve(relativePath + ".xml");
    }

    private static boolean xdocSourceExists(Path xdoc) {
        return Files.exists(xdoc)
                || Files.exists(Path.of(xdoc + ".vm"))
                || Files.exists(Path.of(xdoc + ".template"));
    }

    private static void writePageRedirect(Path outputDirectory, PageRedirect redirect)
            throws IOException {
        final Path output = toXdocPath(outputDirectory, redirect.source());

        Files.createDirectories(output.getParent());
        Files.writeString(output, createXdoc(redirect));
    }

    private static String createXdoc(PageRedirect redirect) throws IOException {
        final String destination = toRelativeUrl(redirect.source(), redirect.destination());

        final String template;

        if (redirect.hasFragmentRedirect()) {
            template = readTemplate("fragment-redirect.xml.template");

            final String fragmentDestination = toRelativeUrl(
                    redirect.source(), redirect.fragmentDestination());

            return template
                    .replace("${destination}", destination)
                    .replace("${fragmentDestination}", fragmentDestination);
        }

        template = readTemplate("simple-redirect.xml.template");

        return template.replace("${destination}", destination);
    }

    private static String readTemplate(String name) throws IOException {
        final String resource = "/site-redirect/" + name;

        try (InputStream stream =
                SiteRedirectGenerator.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Redirect template does not exist: " + resource);
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Converts a site-root destination into a URL relative to the redirect page.
     *
     * <p>For example, {@code /config_old.html -> /config-new.html} becomes
     * {@code config-new.html}, while {@code /checks/index.html -> /checks.html}
     * becomes {@code ../checks.html}.</p>
     *
     * @param source redirect page
     * @param destination destination page
     * @return destination relative to the redirect page
     */
    private static String toRelativeUrl(String source, String destination) {
        final Path sourceDirectory =
                Path.of(source).getParent();

        return sourceDirectory
                .relativize(Path.of(destination))
                .toString()
                .replace('\\', '/');
    }
}

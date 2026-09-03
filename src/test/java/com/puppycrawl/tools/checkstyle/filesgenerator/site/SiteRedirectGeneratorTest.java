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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SiteRedirectGeneratorTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void testGenerateSimpleRedirect() throws IOException {
        createXdoc("new-page.xml");
        writeRegistry("/old-page.html=/new-page.html\n");

        SiteRedirectGenerator.generate(temporaryDirectory);

        final String generated = readGeneratedXdoc("old-page.xml");

        assertTrue(generated.contains(
                "const destination = new URL(\"new-page.html\", currentUrl);"));
        assertTrue(generated.contains(
                "<meta http-equiv=\"refresh\" content=\"0; url=new-page.html\"/>"));
        assertTrue(generated.contains("//]]></script>"));
    }

    @Test
    void testGenerateNestedRedirect() throws IOException {
        createXdoc("checks.xml");
        writeRegistry("/checks/index.html=/checks.html\n");

        SiteRedirectGenerator.generate(temporaryDirectory);

        final String generated = readGeneratedXdoc("checks/index.xml");

        assertTrue(generated.contains(
                "const destination = new URL(\"../checks.html\", currentUrl);"));
    }

    @Test
    void testGenerateFragmentRedirect() throws IOException {
        createXdoc("checks/annotation/index.xml");
        writeRegistry("""
                /config_annotation.html=/checks/annotation/index.html
                /config_annotation.html#*=/checks/annotation/{fragment-lower}.html
                """);

        SiteRedirectGenerator.generate(temporaryDirectory);

        final String generated = readGeneratedXdoc("config_annotation.xml");

        assertTrue(generated.contains(
                "let redirectPath = \"checks/annotation/index.html\";"));
        assertTrue(generated.contains(
                "\"checks/annotation/{fragment-lower}.html\".replace("));
        assertTrue(generated.contains(
                "\"{fragment-lower}\", fragment.toLowerCase())"));
        assertTrue(generated.contains("//]]></script>"));
    }

    @Test
    void testTemplateCanBeRedirectDestination() throws IOException {
        createXdoc("generated-page.xml.template");
        writeRegistry("/old-page.html=/generated-page.html\n");

        SiteRedirectGenerator.generate(temporaryDirectory);

        assertTrue(Files.exists(generatedXdoc("old-page.xml")));
    }

    @Test
    void testRedirectSourceCannotCollideWithAuthoredXdoc() throws IOException {
        createXdoc("old-page.xml");
        createXdoc("new-page.xml");
        writeRegistry("/old-page.html=/new-page.html\n");

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SiteRedirectGenerator.generate(temporaryDirectory));

        assertEquals(
                "Redirect source collides with an authored XDoc: /old-page.html",
                exception.getMessage());
    }

    @Test
    void testRedirectDestinationMustExist() throws IOException {
        writeRegistry("/old-page.html=/missing-page.html\n");

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SiteRedirectGenerator.generate(temporaryDirectory));

        assertEquals(
                "Redirect destination does not exist: /missing-page.html",
                exception.getMessage());
    }

    private void createXdoc(String relativePath) throws IOException {
        final Path xdoc = temporaryDirectory
                .resolve(Path.of("src", "site", "xdoc"))
                .resolve(relativePath);

        Files.createDirectories(xdoc.getParent());
        Files.writeString(xdoc, "");
    }

    private void writeRegistry(String content) throws IOException {
        final Path registry = temporaryDirectory.resolve(
                Path.of("src", "site", "redirects.properties"));

        Files.createDirectories(registry.getParent());
        Files.writeString(registry, content);
    }

    private String readGeneratedXdoc(String relativePath) throws IOException {
        return Files.readString(generatedXdoc(relativePath));
    }

    private Path generatedXdoc(String relativePath) {
        return temporaryDirectory
                .resolve(Path.of("target", "generated-site", "xdoc"))
                .resolve(relativePath);
    }
}

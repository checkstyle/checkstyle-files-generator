package com.puppycrawl.tools.checkstyle.filesgenerator.site;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;

/** Generates XDoc files from Checkstyle's XDoc templates. */
public final class XdocGenerator {

    /** Plexus role hint used by the custom parser and sink. */
    private static final String XDOCS_TEMPLATE_HINT = "xdocs-template";

    private XdocGenerator() {
    }

    /**
     * Generate all XDoc files in a Checkstyle checkout.
     *
     * @param checkstylePath path to the Checkstyle checkout
     * @throws Exception when discovery, parsing, or writing fails
     */
    public static void generate(Path checkstylePath) throws Exception {
        final Path root = checkstylePath.toAbsolutePath().normalize();
        final Path xdocDirectory = root.resolve(Path.of("src", "site", "xdoc"));
        SiteUtil.initialize(root);

        final List<Path> templates;
        try (Stream<Path> paths = Files.find(xdocDirectory, Integer.MAX_VALUE,
                (path, attributes) -> {
                    return attributes.isRegularFile()
                            && path.toString().endsWith(".xml.template");
                })) {
            templates = paths.sorted().toList();
        }

        final PlexusContainer plexus = new DefaultPlexusContainer();
        try {
            final XdocsTemplateSinkFactory sinkFactory =
                    (XdocsTemplateSinkFactory) plexus.lookup(
                            SinkFactory.class, XDOCS_TEMPLATE_HINT);
            final XdocsTemplateParser parser = (XdocsTemplateParser) plexus.lookup(
                            Parser.class, XDOCS_TEMPLATE_HINT);
            parser.setCheckstyleRoot(root);

            // Initialize macros for the parser's MacroManager
            final Map<String, Macro> macros =
                    plexus.lookupMap(Macro.class);
            parser.setMacros(macros);

            for (Path template : templates) {
                generate(template, sinkFactory, parser);
            }
        }
        finally {
            plexus.dispose();
        }
    }

    private static void generate(Path template, XdocsTemplateSinkFactory sinkFactory,
                                 XdocsTemplateParser parser) throws Exception {
        final String fileName = template.getFileName().toString();
        final Path output = template.resolveSibling(
                fileName.substring(0, fileName.length() - ".template".length()));
        final Path temporary = Files.createTempFile(output.getParent(), fileName, ".tmp");
        final Sink sink = sinkFactory.createSink(temporary.getParent().toFile(),
                temporary.getFileName().toString(), StandardCharsets.UTF_8.name());

        Reader reader = null;
        try {
            reader = Files.newBufferedReader(template, StandardCharsets.UTF_8);
            parser.parse(reader, sink);
            if (sink instanceof XdocsTemplateSink xdocsTemplateSink) {
                xdocsTemplateSink.finish();
            }
            sink.close();
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception exception) {
            Files.deleteIfExists(temporary);
            throw new IllegalStateException("Exception while handling " + template, exception);
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}

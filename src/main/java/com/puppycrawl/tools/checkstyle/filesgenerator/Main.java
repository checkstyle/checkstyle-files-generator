package com.puppycrawl.tools.checkstyle.filesgenerator;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.puppycrawl.tools.checkstyle.filesgenerator.meta.MetadataGeneratorUtil;
import com.puppycrawl.tools.checkstyle.filesgenerator.site.SiteRedirectGenerator;
import com.puppycrawl.tools.checkstyle.filesgenerator.site.XdocGenerator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "checkstyle-files-generator",
    description = "Generates metadata, XDoc files, and site redirects.",
    mixinStandardHelpOptions = true
)
public final class Main implements Callable<Integer> {

    @Parameters(description = "Path to the Checkstyle source code directory.")
    private Path checkstylePath;

    @Option(names = "--generateMetadata", description = "Generate metadata files.")
    private boolean generateMetadata;

    @Option(names = "--generateXdoc", description = "Generate XDoc files.")
    private boolean generateXdoc;

    @Option(names = "--generateRedirects", description = "Generate site redirect XDoc files.")
    private boolean generateRedirects;

    /** Entry point for standalone command-line use. */
    public static void main(String[] args) {
        final int exitCode = execute(args);
        System.exit(exitCode);
    }

    /**
     * Execute the command without terminating the current JVM.
     *
     * @param args command-line arguments.
     * @return process-compatible exit code.
     */
    public static int execute(String... args) {
        return new CommandLine(new Main()).execute(args);
    }

    @Override
    public Integer call() throws Exception {
        if (!generateMetadata && !generateXdoc && !generateRedirects) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "At least one generation option is required.");
        }
        if (generateMetadata) {
            MetadataGeneratorUtil.generate(checkstylePath, "checks", "filters", "filefilters");
        }
        if (generateXdoc) {
            XdocGenerator.generate(checkstylePath);
        }
        if (generateRedirects) {
            SiteRedirectGenerator.generate(checkstylePath);
        }
        return 0;
    }
}

package com.puppycrawl.tools.checkstyle.filesgenerator;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.puppycrawl.tools.checkstyle.filesgenerator.meta.MetadataGeneratorUtil;
import com.puppycrawl.tools.checkstyle.filesgenerator.site.XdocGenerator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "checkstyle-files-generator",
    description = "Generates metadata and XDoc files in the specified Checkstyle directory.",
    mixinStandardHelpOptions = true
)
public final class Main implements Callable<Integer> {

    @Parameters(description = "Path to the Checkstyle source code directory.")
    private Path checkstylePath;

    @Option(names = "--generateMetadata", description = "Generate metadata files.")
    private boolean generateMetadata;

    @Option(names = "--metadataOutputDirectory", paramLabel = "DIR",
            description = "Write metadata only under DIR when generating metadata.")
    private Path metadataOutputDirectory;

    @Option(names = "--generateXdoc", description = "Generate XDoc files.")
    private boolean generateXdoc;

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
        if (!generateMetadata && !generateXdoc) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "At least one generation option is required.");
        }
        if (metadataOutputDirectory != null && !generateMetadata) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "--metadataOutputDirectory requires --generateMetadata.");
        }
        if (generateMetadata) {
            final List<Path> outputDirectories;
            if (metadataOutputDirectory == null) {
                outputDirectories = List.of(checkstylePath.resolve("src/main/resources"),
                        checkstylePath.resolve("target/classes"));
            }
            else {
                outputDirectories = List.of(metadataOutputDirectory);
            }
            MetadataGeneratorUtil.generate(checkstylePath, outputDirectories,
                    "checks", "filters", "filefilters");
        }
        if (generateXdoc) {
            XdocGenerator.generate(checkstylePath);
        }
        return 0;
    }
}

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
import java.io.PrintWriter;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.maven.doxia.module.xdoc.XdocSink;

/**
 * A sink for Checkstyle's xdoc templates.
 * This module will be removed once
 * <a href="https://github.com/checkstyle/checkstyle/issues/13426">#13426</a> is resolved.
 *
 * @see <a href="https://maven.apache.org/doxia/doxia/doxia-sink-api">Doxia Sink API</a>
 */
public class XdocsTemplateSink extends XdocSink {

    /**
     * Create a new instance, initialize the Writer.
     *
     * @param writer not null writer to write the result.
     * @param encoding encoding of the writer.
     */
    public XdocsTemplateSink(Writer writer, String encoding) {
        super(new CustomPrintWriter(writer));

        try {
            writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
            writer.write("\n");
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to write XML declaration", ex);
        }
    }

    /**
     * Override init() to prevent automatic body tag generation.
     * XML declaration is written in the constructor instead.
     */
    @Override
    protected void init() {
        // no-op: XML declaration is written once in the constructor instead
    }

    /**
     * Override body_() to handle cleanup.
     */
    @Override
    public void body_() {
        writeEOL();
        flush();
    }

    /**
     * Finish the document by writing a trailing newline and flushing.
     * Must be called explicitly since body_() is no longer invoked
     * (it's final in Doxia 2.1.0 and synthesizes unwanted output).
     */
    public void finish() {
        writeEOL();
        flush();
    }


    /**
     * Write a table tag. We override this method because the default implementation
     * adds different attributes which we don't want. We ignore the parameters
     * because we don't need them, but the default implementation will take them
     * into account once this class is removed.
     *
     * @param justification ignored
     * @param grid ignored
     */
    @Override
    public void tableRows(int[] justification, boolean grid) {
        writeStartTag(TABLE);
    }

    /**
     * A Custom writer that only prints Unix-style newline character.
     */
    private static final class CustomPrintWriter extends PrintWriter {

        /** A Regex pattern to represent all kinds of newline character. */
        private static final Pattern LINE_BREAK_ESCAPE = Pattern.compile("\\R");

        /** Unix-Style newline character. */
        private static final String NEWLINE = "\n";

        /**
         * Creates a new instance of this custom writer.
         *
         * @param writer not null writer to write the result
         */
        private CustomPrintWriter(Writer writer) {
            super(writer, false);
        }

        /**
         * Enforces Unix-style newline character.
         */
        @Override
        public void println() {
            write(NEWLINE, 0, NEWLINE.length());
        }

        /**
         * Unifies all newline characters to Unix-Style Newline character.
         *
         * @param line   text that is to be written in the output file.
         * @param offset starting offset value for writing data.
         * @param length total length of string to be written.
         */
        @Override
        public void write(String line, int offset, int length) {
            final String lineBreakReplacedLine =
                LINE_BREAK_ESCAPE.matcher(line).replaceAll(NEWLINE);
            super.write(lineBreakReplacedLine, 0, lineBreakReplacedLine.length());
        }
    }

}


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
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.text.html.HTML.Attribute;

import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.macro.manager.MacroManager;
import org.apache.maven.doxia.macro.manager.MacroNotFoundException;
import org.apache.maven.doxia.macro.manager.DefaultMacroManager;
import org.apache.maven.doxia.module.xdoc.XdocParser;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.Parser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;

/**
 * Parser for Checkstyle's xdoc templates.
 * This parser is responsible for generating xdocs({@code .xml}) from the xdoc
 * templates({@code .xml.template}). The templates are regular xdocs with custom
 * macros for generating dynamic content - properties, examples, etc.
 * This parser behaves just like the {@link XdocParser} with the difference that all
 * elements apart from the {@code macro} element are copied as is to the output.
 * This module will be removed once
 * <a href="https://github.com/checkstyle/checkstyle/issues/13426">#13426</a> is resolved.
 *
 * @see ExampleMacro
 */
@Component(role = Parser.class, hint = "xdocs-template")
public class XdocsTemplateParser extends XdocParser {

    /** Maximum ASCII character value. */
    private static final int MAX_ASCII = 127;

    /** The macro parameters. */
    private final Map<String, Object> macroParameters = new HashMap<>();

    /** Root directory of the Checkstyle checkout being processed. */
    private Path checkstyleRoot = Path.of("").toAbsolutePath().normalize();

    /** The source content of the input reader. Used to pass into macros. */
    private String sourceContent;

    /** A macro name. */
    private String macroName;

    /** Macro manager for executing macros. */
    private MacroManager macroManager = new DefaultMacroManager();

    /**
     * Creates a new {@code XdocsTemplateParser} instance.
     */
    public XdocsTemplateParser() {
        // no code by default
    }

    /**
     * Get the macro manager. Override to return the injected instance.
     *
     * @return the macro manager
     */
    @Override
    protected MacroManager getMacroManager() {
        return macroManager;
    }

    /**
     * Set the macro manager manually.
     *
     * @param macroManager the macro manager
     */
    public void setMacroManager(MacroManager macroManager) {
        this.macroManager = macroManager;
    }

    /**
     * Set the macros map for the MacroManager.
     *
     * @param macros the macros map
     */
    public void setMacros(Map<String, Macro> macros) {
        if (macroManager instanceof DefaultMacroManager) {
            // Use reflection to set the macros field since DefaultMacroManager doesn't have a setter
            try {
                Field macrosField = DefaultMacroManager.class.getDeclaredField("macros");
                macrosField.setAccessible(true);
                macrosField.set(macroManager, macros);
            }
            catch (Exception e) {
                throw new IllegalStateException("Failed to set macros on MacroManager", e);
            }
        }
    }

    /**
     * Set root directory of the Checkstyle checkout being processed.
     *
     * @param root Checkstyle checkout root
     */
    public void setCheckstyleRoot(Path root) {
        checkstyleRoot = root.toAbsolutePath().normalize();
    }

    @Override
    public void parse(Reader source, Sink sink, String reference) throws ParseException {
        try (StringWriter contentWriter = new StringWriter()) {
            IOUtils.copy(source, contentWriter);
            sourceContent = contentWriter.toString();
            super.parse(new StringReader(sourceContent), sink, reference);
        }
        catch (IOException ioException) {
            throw new ParseException("Error reading the input source", ioException);
        }
        finally {
            sourceContent = null;
        }
    }

    @Override
    protected void handleStartTag(XmlPullParser parser, Sink sink) throws MacroExecutionException {
        final String tagName = parser.getName();
        if (tagName.equals(DOCUMENT_TAG.toString())) {
            sink.rawText(parser.getText());
        }
        else if (tagName.equals(MACRO_TAG.toString()) && !isSecondParsing()) {
            processMacroStart(parser);
            setIgnorableWhitespace(true);
        }
        else if (tagName.equals(PARAM.toString()) && !isSecondParsing()) {
            processParamStart(parser, sink);
        }
        else {
            sink.rawText(parser.getText());
        }
    }

    @Override
    protected void handleText(XmlPullParser parser, Sink sink) {
        sink.rawText(escapeXml(parser.getText()));
    }

    /**
     * Re-escapes XML special characters in text that XmlPullParser has already
     * decoded (getText() returns decoded entities). sink.rawText() writes output
     * unescaped, so without this, decoded entities like &quot; would be written
     * back out as literal characters instead of valid XML entities.
     * Whitespace is preserved exactly, unlike sink.text() which also normalizes it.
     *
     * @param text the text to escape.
     * @return the escaped text.
     */
    private static String escapeXml(String text) {
        final StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            switch (ch) {
                case '&':
                    result.append("&amp;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                default:
                    if (ch > MAX_ASCII) {
                        result.append("&#x")
                                .append(Integer.toHexString(ch))
                                .append(';');
                    }
                    else {
                        result.append(ch);
                    }
                    break;
            }
        }
        return result.toString();
    }

    @Override
    protected void handleEndTag(XmlPullParser parser, Sink sink) throws MacroExecutionException {
        final String tagName = parser.getName();
        if (!"hr".equalsIgnoreCase(tagName)) {
            if (tagName.equals(DOCUMENT_TAG.toString())) {
                sink.rawText(parser.getText());
            }
            else if (macroName != null
                    && tagName.equals(MACRO_TAG.toString())
                    && !macroName.isEmpty()
                    && !isSecondParsing()) {
                processMacroEnd(sink);
                setIgnorableWhitespace(false);
            }
            else if (!tagName.equals(PARAM.toString())) {
                sink.rawText(parser.getText());
            }
        }
    }

    /**
     * Handle the opening tag of a macro. Gather the macro name and parameters.
     *
     * @param parser the xml parser.
     * @throws MacroExecutionException if the macro name is not specified.
     */
    private void processMacroStart(XmlPullParser parser) throws MacroExecutionException {
        macroName = parser.getAttributeValue(null, Attribute.NAME.toString());

        if (macroName == null || macroName.isEmpty()) {
            final String message = String.format(Locale.ROOT,
                    "The '%s' attribute for the '%s' tag is required.",
                    Attribute.NAME, MACRO_TAG);
            throw new MacroExecutionException(message);
        }
    }

    /**
     * Handle the opening tag of a parameter. Gather the parameter name and value.
     *
     * @param parser the xml parser.
     * @param sink the sink object.
     * @throws MacroExecutionException if the parameter name or value is not specified.
     */
    private void processParamStart(XmlPullParser parser, Sink sink) throws MacroExecutionException {
        if (macroName != null && !macroName.isEmpty()) {
            final String paramName = parser
                    .getAttributeValue(null, Attribute.NAME.toString());
            final String paramValue = parser
                    .getAttributeValue(null, Attribute.VALUE.toString());

            if (paramName == null
                    || paramValue == null
                    || paramName.isEmpty()
                    || paramValue.isEmpty()) {
                final String message = String.format(Locale.ROOT,
                        "'%s' and '%s' attributes for the '%s' tag are required"
                                + " inside the '%s' tag.",
                        Attribute.NAME, Attribute.VALUE, PARAM, MACRO_TAG);
                throw new MacroExecutionException(message);
            }

            macroParameters.put(paramName, paramValue);
        }
        else {
            sink.rawText(parser.getText());
        }
    }

    /**
     * Execute a macro. Creates a {@link MacroRequest} with the gathered
     * {@link #macroName} and {@link #macroParameters} and executes the macro.
     * Afterward, the macro fields are reinitialized.
     *
     * @param sink the sink object.
     * @throws MacroExecutionException if a macro is not found.
     */
    private void processMacroEnd(Sink sink) throws MacroExecutionException {
        final XdocsTemplateParser parser = new XdocsTemplateParser();
        parser.setCheckstyleRoot(checkstyleRoot);
        parser.setMacroManager(macroManager);

        final MacroRequest request = new MacroRequest(sourceContent,
                parser, macroParameters, checkstyleRoot.toFile());

        try {
            executeMacro(macroName, request, sink);
        }
        catch (MacroNotFoundException exception) {
            final String message = String.format(Locale.ROOT, "Macro '%s' not found.", macroName);
            throw new MacroExecutionException(message, exception);
        }

        reinitializeMacroFields();
    }

    /**
     * Reinitialize the macro fields.
     */
    private void reinitializeMacroFields() {
        macroName = "";
        macroParameters.clear();
    }

}

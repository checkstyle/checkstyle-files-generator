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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.maven.doxia.macro.AbstractMacro;
import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.sink.Sink;
import org.codehaus.plexus.component.annotations.Component;

/**
 * A macro that inserts a list of the violation messages.
 */
@Component(role = Macro.class, hint = "violation-messages")
public class ViolationMessagesMacro extends AbstractMacro {

    /**
     * Creates a new {@code ViolationMessagesMacro} instance.
     */
    public ViolationMessagesMacro() {
        // no code by default
    }

    @Override
    public void execute(Sink sink, MacroRequest request) throws MacroExecutionException {
        final String checkName = (String) request.getParameter("checkName");
        final Object instance = SiteUtil.getModuleInstance(checkName);
        final Class<?> clss = instance.getClass();
        final Set<String> messageKeys = SiteUtil.getMessageKeys(clss);
        createListOfMessages(sink, clss, messageKeys);
    }

    /**
     * Iterates through the fields of the class and creates an unordered list.
     *
     * @param sink the sink to write to.
     * @param clss the class of the fields.
     * @param messageKeys the List of message keys to iterate through.
     */
    private static void createListOfMessages(
            Sink sink, Class<?> clss, Set<String> messageKeys) {
        final String indentLevel8 = SiteUtil.getNewlineAndIndentSpaces(8);
        sink.list(null);

        for (String messageKey : messageKeys) {
            createListItem(sink, clss, messageKey);
        }
        sink.rawText(indentLevel8);
        sink.list_();
    }

    /**
     * Creates a list item for the given field.
     *
     * @param sink the sink to write to.
     * @param clss the class of the field.
     * @param messageKey the message key.
     */
    private static void createListItem(Sink sink, Class<?> clss, String messageKey) {
        final String messageKeyUrl = constructMessageKeyUrl(clss, messageKey);
        sink.rawText(SiteUtil.getNewlineAndIndentSpaces(10));
        sink.listItem(null);
        sink.rawText(SiteUtil.getNewlineAndIndentSpaces(12));
        sink.link(messageKeyUrl, null);
        sink.rawText(SiteUtil.getNewlineAndIndentSpaces(14));
        sink.text(messageKey);
        sink.rawText(SiteUtil.getNewlineAndIndentSpaces(12));
        sink.link_();
        sink.rawText(SiteUtil.getNewlineAndIndentSpaces(10));
        sink.listItem_();
    }

    /**
     * Constructs a URL to GitHub that searches for the message key.
     *
     * @param clss the class of the module.
     * @param messageKey the message key.
     * @return the URL to GitHub.
     */
    private static String constructMessageKeyUrl(Class<?> clss, String messageKey) {
        final String query = "path:src/main/resources/"
                + clss.getPackage().getName().replace('.', '/')
                + " path:**/messages*.properties repo:checkstyle/checkstyle \""
                + messageKey + "\"";

        return "https://github.com/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

}


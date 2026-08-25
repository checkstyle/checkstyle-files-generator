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

import java.util.Map;

import org.apache.maven.doxia.macro.Macro;
import org.apache.maven.doxia.macro.MacroExecutionException;
import org.apache.maven.doxia.macro.MacroExecutor;
import org.apache.maven.doxia.macro.MacroRequest;
import org.apache.maven.doxia.macro.manager.MacroNotFoundException;
import org.apache.maven.doxia.sink.Sink;

/**
 * A custom MacroExecutor that uses macros from a Plexus container.
 * This avoids the need to use reflection to set private fields in DefaultMacroManager.
 */
public class PlexusMacroExecutor implements MacroExecutor {

    /** The macros map from Plexus. */
    private final Map<String, Macro> macros;

    /**
     * Creates a new {@code PlexusMacroExecutor} instance.
     *
     * @param macros the macros map from Plexus
     */
    public PlexusMacroExecutor(Map<String, Macro> macros) {
        this.macros = macros;
    }

    @Override
    public void executeMacro(String macroId, MacroRequest request, Sink sink)
            throws MacroExecutionException, MacroNotFoundException {
        final Macro macro = macros.get(macroId);
        if (macro == null) {
            throw new MacroNotFoundException("Macro '" + macroId + "' not found");
        }

        macro.execute(sink, request);
    }
}

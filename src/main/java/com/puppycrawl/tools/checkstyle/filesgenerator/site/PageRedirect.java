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

/** A legacy site page and the destination it redirects to. */
record PageRedirect(String source, String destination, String fragmentDestination) {

    /**
     * Checks whether this redirect has a rule that uses the part after {@code #}
     * in the old URL to determine the destination page.
     *
     * <p>For example, {@code /config_annotation.html#AnnotationUseStyle} redirects to
     * {@code /checks/annotation/annotationusestyle.html}. In this case,
     * {@code AnnotationUseStyle} is the URL fragment and selects the destination page.</p>
     *
     * @return {@code true} if this redirect has a fragment-based destination
     */
    boolean hasFragmentRedirect() {
        return fragmentDestination != null;
    }
}

/*
* Copyright (c) 2026 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.mdx.parser.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;

import java.util.List;

import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

/**
 * An empty bracketed name is valid MDX: Analysis Services names a member whose key is NULL or blank
 * {@code &[]}, and a client sends that unique name back. Only an unquoted identifier needs a character.
 */
@RequireServiceComponentRuntime
class EmptyBracketedNameTest {

    @Test
    void anEmptyBracketedNameIsRead(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        for (String mdx : List.of("SELECT {[Product].[Size].&[]} ON 0 FROM [Adventure Works]",
                "SELECT {[Customer].[City].&[]&[HE]} ON 0 FROM [Adventure Works]",
                "SELECT {[Product].[Size].[]} ON 0 FROM [Adventure Works]")) {
            assertThat(mdxParserProvider.newParser(mdx, propertyWords).parseMdxStatement()).as(mdx).isNotNull();
        }
    }
}

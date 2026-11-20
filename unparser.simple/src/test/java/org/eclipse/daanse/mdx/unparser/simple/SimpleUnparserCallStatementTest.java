/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.mdx.unparser.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.record.CallStatementR;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.NumericLiteralR;
import org.junit.jupiter.api.Test;

class SimpleUnparserCallStatementTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    private static CompoundIdR id(String... names) {
        return new CompoundIdR(List.of(names).stream()
                .<ObjectIdentifier>map(n -> new NameObjectIdentifierR(n, Quoting.UNQUOTED)).toList());
    }

    @Test
    void callWithCube() {
        var procedure = new CallExpressionR(new MethodOperationAtom("Reload"),
                List.<MdxExpression>of(id("PortTools", "Loader"), new NumericLiteralR(BigDecimal.ONE)));
        var statement = new CallStatementR(procedure,
                Optional.of(new NameObjectIdentifierR("Coast", Quoting.QUOTED)));
        assertThat(unparser.unparseMdxStatement(statement)).asString()
                .isEqualTo("CALL PortTools.Loader.Reload(1) ON [Coast]");
    }

    @Test
    void callWithoutArgumentList() {
        assertThat(unparser.unparseMdxStatement(new CallStatementR(id("PortTools", "ClearCache"), Optional.empty())))
                .asString().isEqualTo("CALL PortTools.ClearCache");
    }
}

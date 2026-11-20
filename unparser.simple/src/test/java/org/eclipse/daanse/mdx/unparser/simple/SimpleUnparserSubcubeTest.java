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

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.Axis;
import org.eclipse.daanse.mdx.model.record.CreateSubcubeStatementR;
import org.eclipse.daanse.mdx.model.record.DropSubcubeStatementR;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.select.AxisR;
import org.eclipse.daanse.mdx.model.record.select.SelectCubeClauseNameR;
import org.eclipse.daanse.mdx.model.record.select.SelectCubeClauseSubStatementR;
import org.eclipse.daanse.mdx.model.record.select.SelectQueryAxesClauseR;
import org.eclipse.daanse.mdx.model.record.select.SelectQueryAxisClauseR;
import org.junit.jupiter.api.Test;

class SimpleUnparserSubcubeTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    private static final NameObjectIdentifierR COAST = new NameObjectIdentifierR("Coast", Quoting.QUOTED);

    private static SelectCubeClauseSubStatementR subcube(boolean nonVisual) {
        Axis axis = AxisR.create(0);
        return new SelectCubeClauseSubStatementR(
                new SelectQueryAxesClauseR(List.of(new SelectQueryAxisClauseR(false,
                        new CallExpressionR(new BracesOperationAtom(), List.of()), axis, null))),
                new SelectCubeClauseNameR(COAST), Optional.empty(), nonVisual);
    }

    @Test
    void createSubcube() {
        assertThat(unparser.unparseMdxStatement(new CreateSubcubeStatementR(COAST, subcube(false)))).asString()
                .startsWith("CREATE SUBCUBE [Coast] AS SELECT ").contains(" FROM [Coast]").doesNotContain("NON VISUAL");
    }

    @Test
    void createSubcubeNonVisual() {
        assertThat(unparser.unparseMdxStatement(new CreateSubcubeStatementR(COAST, subcube(true)))).asString()
                .startsWith("CREATE SUBCUBE [Coast] AS NON VISUAL ( ");
    }

    @Test
    void dropSubcube() {
        assertThat(unparser.unparseMdxStatement(new DropSubcubeStatementR(COAST))).asString()
                .isEqualTo("DROP SUBCUBE [Coast]");
    }
}

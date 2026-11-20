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
package org.eclipse.daanse.mdx.parser.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkSelectCubeClauseName;

import org.eclipse.daanse.mdx.model.api.CreateSetStatement;
import org.eclipse.daanse.mdx.model.api.CreateSubcubeStatement;
import org.eclipse.daanse.mdx.model.api.DropMemberStatement;
import org.eclipse.daanse.mdx.model.api.DropSetStatement;
import org.eclipse.daanse.mdx.model.api.DropSubcubeStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.SelectCubeClause;
import org.eclipse.daanse.mdx.model.api.select.SelectCubeClauseSubStatement;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxesClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

/**
 * CREATE SUBCUBE, DROP SUBCUBE and NON VISUAL before a subselect. The select of CREATE SUBCUBE is kept as a subselect,
 * with nonVisual() telling whether NON VISUAL was written.
 */
@RequireServiceComponentRuntime
class SubcubeStatementTest {

    private static final String S = "SELECT {[Time].[Year].&[2024]} ON 0 FROM [Coast]";

    private static MdxStatement statement(MdxParserProvider provider, String mdx) throws MdxParserException {
        return provider.newParser(mdx, propertyWords).parseMdxStatement();
    }

    private static CreateSubcubeStatement create(MdxParserProvider provider, String mdx) throws MdxParserException {
        MdxStatement statement = statement(provider, mdx);
        assertThat(statement).isInstanceOf(CreateSubcubeStatement.class);
        CreateSubcubeStatement create = (CreateSubcubeStatement) statement;
        assertThat(create.cubeName().name()).isEqualTo("Coast");
        assertThat(create.cubeName().quoting()).isEqualTo(Quoting.QUOTED);
        return create;
    }

    private static SelectCubeClauseSubStatement subselect(SelectCubeClause clause) {
        assertThat(clause).isInstanceOf(SelectCubeClauseSubStatement.class);
        return (SelectCubeClauseSubStatement) clause;
    }

    private static SelectQueryAxesClause axes(SelectCubeClauseSubStatement subcube, int count) {
        assertThat(subcube.selectQueryClause()).isInstanceOf(SelectQueryAxesClause.class);
        SelectQueryAxesClause axes = (SelectQueryAxesClause) subcube.selectQueryClause();
        assertThat(axes.selectQueryAxisClauses()).hasSize(count);
        return axes;
    }

    /** Braces of one member, e.g. {[Time].[Year].&[2024]}; the first segment is checked. */
    private static void braces(SelectQueryAxisClause axis, int members, String dimension) {
        assertThat(axis.expression()).isInstanceOf(CallExpression.class);
        CallExpression braces = (CallExpression) axis.expression();
        assertThat(braces.operationAtom()).isEqualTo(new BracesOperationAtom());
        assertThat(braces.expressions()).hasSize(members);
        assertThat(braces.expressions().get(0)).isInstanceOf(CompoundId.class);
        CompoundId first = (CompoundId) braces.expressions().get(0);
        assertThat(((NameObjectIdentifier) first.objectIdentifiers().get(0)).name()).isEqualTo(dimension);
    }

    /** SELECT {[Time].[Year].&[2024]} ON 0 FROM [Coast] */
    private static void checkS(SelectCubeClauseSubStatement subcube) {
        braces(axes(subcube, 1).selectQueryAxisClauses().get(0), 1, "Time");
        checkSelectCubeClauseName(subcube.selectCubeClause(), "Coast", Quoting.QUOTED);
        assertThat(subcube.selectSlicerAxisClause()).isEmpty();
    }

    @Test
    void createSubcube(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider, "CREATE SUBCUBE [Coast] AS " + S);
        assertThat(statement.subcube().nonVisual()).isFalse();
        checkS(statement.subcube());
    }

    @Test
    void createSubcubeNonVisual(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider, "CREATE SUBCUBE [Coast] AS NON VISUAL (" + S + ")");
        assertThat(statement.subcube().nonVisual()).isTrue();
        checkS(statement.subcube());
    }

    @Test
    void createSubcubeOverASubselect(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider,
                "CREATE SUBCUBE [Coast] AS SELECT {[Port].[Region].&[North]} ON 0 FROM (" + S + ")");
        assertThat(statement.subcube().nonVisual()).isFalse();
        braces(axes(statement.subcube(), 1).selectQueryAxisClauses().get(0), 1, "Port");
        SelectCubeClauseSubStatement inner = subselect(statement.subcube().selectCubeClause());
        assertThat(inner.nonVisual()).isFalse();
        checkS(inner);
    }

    @Test
    void createSubcubeNonVisualOverASubselect(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider,
                "CREATE SUBCUBE [Coast] AS NON VISUAL (SELECT {[Port].[Region].&[North]} ON 0 FROM (" + S + "))");
        assertThat(statement.subcube().nonVisual()).isTrue();
        SelectCubeClauseSubStatement inner = subselect(statement.subcube().selectCubeClause());
        assertThat(inner.nonVisual()).isFalse();
        checkS(inner);
    }

    @Test
    void createSubcubeTwoAxes(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider, "CREATE SUBCUBE [Coast] AS SELECT {[Time].[Year].&[2024]} "
                + "ON 0, {[Port].[Region].&[North], [Port].[Region].&[South]} ON 1 FROM [Coast]");
        SelectQueryAxesClause axes = axes(statement.subcube(), 2);
        braces(axes.selectQueryAxisClauses().get(0), 1, "Time");
        braces(axes.selectQueryAxisClauses().get(1), 2, "Port");
    }

    @Test
    void createSubcubeWithSlicer(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider,
                "CREATE SUBCUBE [Coast] AS SELECT {[Port].[Region].&[North]} ON 0 FROM [Coast] WHERE [Time].[Year].&[2024]");
        assertThat(statement.subcube().selectSlicerAxisClause()).isPresent();
        assertThat(statement.subcube().selectSlicerAxisClause().get().expression()).isInstanceOf(CompoundId.class);
        CompoundId slicer = (CompoundId) statement.subcube().selectSlicerAxisClause().get().expression();
        assertThat(slicer.objectIdentifiers()).hasSize(3);
        assertThat(((NameObjectIdentifier) slicer.objectIdentifiers().get(0)).name()).isEqualTo("Time");
    }

    @Test
    void createSubcubeLowerCaseAndLineBreaks(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateSubcubeStatement statement = create(provider, """
                create subcube [Coast] as
                select {[Time].[Year].&[2024]} on 0
                from [Coast]""");
        assertThat(statement.subcube().nonVisual()).isFalse();
        checkS(statement.subcube());
    }

    @Test
    void dropSubcube(@InjectService MdxParserProvider provider) throws MdxParserException {
        for (String mdx : new String[] { "DROP SUBCUBE [Coast]", "drop subcube [Coast]" }) {
            MdxStatement statement = statement(provider, mdx);
            assertThat(statement).isInstanceOf(DropSubcubeStatement.class);
            assertThat(((DropSubcubeStatement) statement).cubeName().name()).isEqualTo("Coast");
            assertThat(((DropSubcubeStatement) statement).cubeName().quoting()).isEqualTo(Quoting.QUOTED);
        }
    }

    @Test
    void nonVisualSubselectInFrom(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement statement = statement(provider, "SELECT {[Measures].[Tonnes]} ON 0 FROM NON VISUAL (" + S + ")");
        assertThat(statement).isInstanceOf(SelectStatement.class);
        SelectCubeClauseSubStatement subselect = subselect(((SelectStatement) statement).selectCubeClause());
        assertThat(subselect.nonVisual()).isTrue();
        checkS(subselect);
    }

    // controls

    @Test
    void subselectIsNotNonVisual(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement statement = statement(provider, "SELECT {[Measures].[Tonnes]} ON 0 FROM (" + S + ")");
        assertThat(statement).isInstanceOf(SelectStatement.class);
        SelectCubeClauseSubStatement subselect = subselect(((SelectStatement) statement).selectCubeClause());
        assertThat(subselect.nonVisual()).isFalse();
        checkS(subselect);
    }

    @Test
    void nestedSubselects(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement statement = statement(provider,
                "SELECT {[Measures].[Tonnes]} ON 0 FROM (SELECT {[Port].[Region].&[North]} ON 0 FROM (" + S + "))");
        SelectCubeClauseSubStatement outer = subselect(((SelectStatement) statement).selectCubeClause());
        assertThat(outer.nonVisual()).isFalse();
        SelectCubeClauseSubStatement inner = subselect(outer.selectCubeClause());
        assertThat(inner.nonVisual()).isFalse();
        checkS(inner);
    }

    @Test
    void setAndMemberStatementsStay(@InjectService MdxParserProvider provider) throws MdxParserException {
        assertThat(statement(provider, "CREATE SET CURRENTCUBE.[Recent] AS {[Time].[Year].&[2024]}"))
                .isInstanceOf(CreateSetStatement.class);
        assertThat(statement(provider, "DROP SET CURRENTCUBE.[Recent]")).isInstanceOf(DropSetStatement.class);
        assertThat(statement(provider, "DROP MEMBER CURRENTCUBE.[Measures].[Twice]"))
                .isInstanceOf(DropMemberStatement.class);
        MdxStatement subcubeSet = statement(provider, "DROP SET CURRENTCUBE.[Subcube]");
        assertThat(subcubeSet).isInstanceOf(DropSetStatement.class);
        assertThat(((NameObjectIdentifier) ((DropSetStatement) subcubeSet).names().get(0).compoundId()
                .objectIdentifiers().get(0)).name()).isEqualTo("Subcube");
    }
}

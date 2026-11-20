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
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkNameObjectIdentifiers;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.DrillthroughStatement;
import org.eclipse.daanse.mdx.model.api.ExplainStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.ReturnItem;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

/**
 * The RETURN list of DRILLTHROUGH: a column may be wrapped in Name, UniqueName, Key (with a column number), Caption,
 * MemberValue, CustomRollup, CustomRollupProperties or UnaryOperator, and mixed with bare columns.
 */
@RequireServiceComponentRuntime
class DrillthroughReturnFunctionTest {

    private static final String D = "DRILLTHROUGH MAXROWS 10 SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast] "
            + "WHERE ([Port].[Port].&[Kiel], [Time].[Year].&[2024])";

    private static MdxStatement statement(MdxParserProvider provider, String mdx) throws MdxParserException {
        return provider.newParser(mdx, propertyWords).parseMdxStatement();
    }

    private static DrillthroughStatement drillthrough(MdxParserProvider provider, String mdx)
            throws MdxParserException {
        MdxStatement statement = statement(provider, mdx);
        assertThat(statement).isInstanceOf(DrillthroughStatement.class);
        return (DrillthroughStatement) statement;
    }

    private static List<? extends ReturnItem> items(MdxParserProvider provider, String mdx) throws MdxParserException {
        DrillthroughStatement statement = drillthrough(provider, mdx);
        assertThat(statement.maxRows()).contains(10);
        return statement.returnItems();
    }

    private static void column(ReturnItem item, String... names) {
        List<? extends ObjectIdentifier> ids = item.compoundId().objectIdentifiers();
        assertThat(ids).hasSize(names.length);
        for (int i = 0; i < names.length; i++) {
            checkNameObjectIdentifiers(ids, i, names[i], Quoting.QUOTED);
        }
    }

    private static void bare(ReturnItem item, String... names) {
        column(item, names);
        assertThat(item.function()).isEmpty();
        assertThat(item.arguments()).isEmpty();
    }

    private static void wrapped(ReturnItem item, String function, String... names) {
        column(item, names);
        assertThat(item.function()).isPresent();
        assertThat(item.function().get()).isEqualToIgnoringCase(function);
    }

    private static void single(MdxParserProvider provider, String function) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, D + " RETURN " + function + "([$Port].[Port])");
        assertThat(items).hasSize(1);
        wrapped(items.get(0), function, "$Port", "Port");
        assertThat(items.get(0).arguments()).isEmpty();
    }

    private static void number(Object expression, int value) {
        assertThat(expression).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) expression).value()).isEqualByComparingTo(String.valueOf(value));
    }

    @Test
    void key(@InjectService MdxParserProvider provider) throws MdxParserException {
        single(provider, "Key");
    }

    @Test
    void name(@InjectService MdxParserProvider provider) throws MdxParserException {
        single(provider, "Name");
    }

    @Test
    void caption(@InjectService MdxParserProvider provider) throws MdxParserException {
        single(provider, "Caption");
    }

    @Test
    void uniqueName(@InjectService MdxParserProvider provider) throws MdxParserException {
        single(provider, "UniqueName");
    }

    @Test
    void memberValue(@InjectService MdxParserProvider provider) throws MdxParserException {
        single(provider, "MemberValue");
    }

    @Test
    void keyWithColumnNumber(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, D + " RETURN Key([$Port].[Port], 1)");
        assertThat(items).hasSize(1);
        wrapped(items.get(0), "Key", "$Port", "Port");
        assertThat(items.get(0).arguments()).hasSize(1);
        number(items.get(0).arguments().get(0), 1);
    }

    @Test
    void bareAndWrappedMixed(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, D
                + " RETURN [$Port].[Region], Name([$Port].[Port]), Key([$Time].[Month], 2), [Shipments].[Tonnes]");
        assertThat(items).hasSize(4);
        bare(items.get(0), "$Port", "Region");
        wrapped(items.get(1), "Name", "$Port", "Port");
        assertThat(items.get(1).arguments()).isEmpty();
        wrapped(items.get(2), "Key", "$Time", "Month");
        assertThat(items.get(2).arguments()).hasSize(1);
        number(items.get(2).arguments().get(0), 2);
        bare(items.get(3), "Shipments", "Tonnes");
    }

    @Test
    void lowerCaseAndLineBreaks(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, """
                drillthrough maxrows 10
                select {[Measures].[Tonnes]} on 0 from [Coast]
                return key([$Port].[Port]),
                        name([$Port].[Port])""");
        assertThat(items).hasSize(2);
        wrapped(items.get(0), "Key", "$Port", "Port");
        wrapped(items.get(1), "Name", "$Port", "Port");
    }

    @Test
    void withFirstRowSet(@InjectService MdxParserProvider provider) throws MdxParserException {
        DrillthroughStatement statement = drillthrough(provider,
                "DRILLTHROUGH MAXROWS 10 FIRSTROWSET 2 SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast] "
                        + "RETURN Caption([$Port].[Port])");
        assertThat(statement.firstRowSet()).contains(2);
        assertThat(statement.returnItems()).hasSize(1);
        wrapped(statement.returnItems().get(0), "Caption", "$Port", "Port");
    }

    @Test
    void rollupAndUnaryOperator(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, D + " RETURN CustomRollup([$Port].[Port]), "
                + "CustomRollupProperties([$Port].[Port]), UnaryOperator([$Port].[Port])");
        assertThat(items).hasSize(3);
        wrapped(items.get(0), "CustomRollup", "$Port", "Port");
        wrapped(items.get(1), "CustomRollupProperties", "$Port", "Port");
        wrapped(items.get(2), "UnaryOperator", "$Port", "Port");
    }

    @Test
    void explain(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement statement = statement(provider, "EXPLAIN PLAN FOR " + D + " RETURN Key([$Port].[Port])");
        assertThat(statement).isInstanceOf(ExplainStatement.class);
        MdxStatement inner = ((ExplainStatement) statement).mdxStatement();
        assertThat(inner).isInstanceOf(DrillthroughStatement.class);
        List<? extends ReturnItem> items = ((DrillthroughStatement) inner).returnItems();
        assertThat(items).hasSize(1);
        wrapped(items.get(0), "Key", "$Port", "Port");
    }

    // controls: bare columns keep their tree

    @Test
    void bareColumns(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider, D + " RETURN [Port].[Port], [Measures].[Tonnes]");
        assertThat(items).hasSize(2);
        bare(items.get(0), "Port", "Port");
        bare(items.get(1), "Measures", "Tonnes");
    }

    @Test
    void bareCubeDimensions(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = items(provider,
                D + " RETURN [$Port].[Port], [$Time].[Month], [Shipments].[Tonnes]");
        assertThat(items).hasSize(3);
        bare(items.get(0), "$Port", "Port");
        bare(items.get(1), "$Time", "Month");
        bare(items.get(2), "Shipments", "Tonnes");
    }

    @Test
    void bareWithFirstRowSet(@InjectService MdxParserProvider provider) throws MdxParserException {
        DrillthroughStatement statement = drillthrough(provider,
                "DRILLTHROUGH MAXROWS 10 FIRSTROWSET 2 SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast] RETURN [$Port].[Port]");
        assertThat(statement.maxRows()).contains(10);
        assertThat(statement.firstRowSet()).contains(2);
        assertThat(statement.returnItems()).hasSize(1);
        bare(statement.returnItems().get(0), "$Port", "Port");
    }

    @Test
    void noReturn(@InjectService MdxParserProvider provider) throws MdxParserException {
        DrillthroughStatement statement = drillthrough(provider,
                "DRILLTHROUGH SELECT {[Measures].[Tonnes]} ON 0 FROM [Coast]");
        assertThat(statement.maxRows()).isEmpty();
        assertThat(statement.returnItems()).isNullOrEmpty();
    }

    @Test
    void keyAsExpressionStaysAFunction(@InjectService MdxParserProvider provider) throws MdxParserException {
        var expression = provider.newParser("Key([$Port].[Port], 1)", propertyWords).parseExpression();
        assertThat(expression).isInstanceOf(CallExpression.class);
        CallExpression call = (CallExpression) expression;
        assertThat(call.operationAtom()).isEqualTo(new FunctionOperationAtom("Key"));
        assertThat(call.expressions()).hasSize(2);
    }

    @Test
    void parseReturnItems(@InjectService MdxParserProvider provider) throws MdxParserException {
        List<? extends ReturnItem> items = provider.newParser("Name([$Port].[Port]), [$Port].[Region]", propertyWords)
                .parseReturnItems();
        assertThat(items).hasSize(2);
        wrapped(items.get(0), "Name", "$Port", "Port");
        bare(items.get(1), "$Port", "Region");
    }
}

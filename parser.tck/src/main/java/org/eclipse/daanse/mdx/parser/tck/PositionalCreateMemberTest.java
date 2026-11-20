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
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.mdx.parser.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkCube;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkNameObjectIdentifiers;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.CreateMemberStatement;
import org.eclipse.daanse.mdx.model.api.CreateSetStatement;
import org.eclipse.daanse.mdx.model.api.CubeReference;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.SetEvaluation;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

/**
 * The positional CREATE of a cube script, {@code CREATE [Measures].[X] = <expression>}: no cube, no MEMBER, no AS. It
 * is a CreateMemberStatement whose cube is {@link CubeReference.Omitted}, so that a consumer can take it in a script
 * and refuse it as a session statement. The keyword form keeps its tree.
 */
@RequireServiceComponentRuntime
class PositionalCreateMemberTest {

    private static MdxStatement statement(MdxParserProvider provider, String mdx) throws MdxParserException {
        return provider.newParser(mdx, propertyWords).parseMdxStatement();
    }

    private static CreateMemberStatement parse(MdxParserProvider provider, String mdx) throws MdxParserException {
        MdxStatement statement = statement(provider, mdx);
        assertThat(statement).isInstanceOf(CreateMemberStatement.class);
        return (CreateMemberStatement) statement;
    }

    private static CallExpression call(MdxExpression expression, String operator) {
        assertThat(expression).isInstanceOf(CallExpression.class);
        CallExpression call = (CallExpression) expression;
        assertThat(call.operationAtom()).isEqualTo(new InfixOperationAtom(operator));
        return call;
    }

    private static CallExpression function(MdxExpression expression, String name) {
        assertThat(expression).isInstanceOf(CallExpression.class);
        CallExpression call = (CallExpression) expression;
        assertThat(call.operationAtom()).isEqualTo(new FunctionOperationAtom(name));
        return call;
    }

    private static void name(CompoundId compoundId, String... names) {
        List<? extends ObjectIdentifier> ids = compoundId.objectIdentifiers();
        assertThat(ids).hasSize(names.length);
        for (int i = 0; i < names.length; i++) {
            checkNameObjectIdentifiers(ids, i, names[i], Quoting.QUOTED);
        }
    }

    /** CREATE [Measures].[Twice] = [Measures].[Tonnes] * 2 */
    private static void checkTwice(CreateMemberStatement statement) {
        assertThat(statement.session()).isFalse();
        assertThat(statement.calculated()).isFalse();
        assertThat(statement.cube()).isInstanceOf(CubeReference.Omitted.class);
        name(statement.body().compoundId(), "Measures", "Twice");
        CallExpression times = call(statement.body().expression(), "*");
        assertThat(times.expressions()).hasSize(2);
        assertThat(times.expressions().get(0)).isInstanceOf(CompoundId.class);
        name((CompoundId) times.expressions().get(0), "Measures", "Tonnes");
        assertThat(times.expressions().get(1)).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) times.expressions().get(1)).value()).isEqualByComparingTo("2");
        assertThat(statement.body().memberPropertyDefinitions()).isEmpty();
    }

    @Test
    void positional(@InjectService MdxParserProvider provider) throws MdxParserException {
        checkTwice(parse(provider, "CREATE [Measures].[Twice] = [Measures].[Tonnes] * 2"));
    }

    @Test
    void caseOfCreate(@InjectService MdxParserProvider provider) throws MdxParserException {
        checkTwice(parse(provider, "create [Measures].[Twice] = [Measures].[Tonnes] * 2"));
    }

    @Test
    void commentsAndLineBreaks(@InjectService MdxParserProvider provider) throws MdxParserException {
        checkTwice(parse(provider, "CREATE /* doubled */ [Measures].[Twice]\n    = [Measures].[Tonnes] * 2 -- per trip\n"));
    }

    @Test
    void throughParseCreateMemberStatement(@InjectService MdxParserProvider provider) throws MdxParserException {
        checkTwice(provider.newParser("CREATE [Measures].[Twice] = [Measures].[Tonnes] * 2", propertyWords)
                .parseCreateMemberStatement());
    }

    @Test
    void aggregateOfTwoKeys(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateMemberStatement statement = parse(provider, """
                CREATE [Port].[Port].[All].[North and South] = \
                Aggregate({[Port].[Region].&[North], [Port].[Region].&[South]})""");
        assertThat(statement.cube()).isInstanceOf(CubeReference.Omitted.class);
        name(statement.body().compoundId(), "Port", "Port", "All", "North and South");
        CallExpression aggregate = function(statement.body().expression(), "Aggregate");
        assertThat(aggregate.expressions()).hasSize(1);
        assertThat(aggregate.expressions().get(0)).isInstanceOf(CallExpression.class);
        CallExpression braces = (CallExpression) aggregate.expressions().get(0);
        assertThat(braces.operationAtom()).isEqualTo(new BracesOperationAtom());
        assertThat(braces.expressions()).hasSize(2);
    }

    @Test
    void iifWithAnEqualityInside(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateMemberStatement statement = parse(provider, """
                CREATE [Measures].[Per Trip] = \
                IIF([Measures].[Trips] = 0, NULL, [Measures].[Tonnes] / [Measures].[Trips])""");
        assertThat(statement.cube()).isInstanceOf(CubeReference.Omitted.class);
        name(statement.body().compoundId(), "Measures", "Per Trip");
        CallExpression iif = function(statement.body().expression(), "IIF");
        assertThat(iif.expressions()).hasSize(3);
        call(iif.expressions().get(0), "=");
        call(iif.expressions().get(2), "/");
    }

    @Test
    void withoutExpressionRefused(@InjectService MdxParserProvider provider) {
        assertThatThrownBy(() -> statement(provider, "CREATE [Measures].[Twice] ="))
                .isInstanceOf(MdxParserException.class);
    }

    // the keyword form keeps its tree

    @Test
    void keywordCurrentCube(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateMemberStatement statement = parse(provider,
                "CREATE MEMBER CURRENTCUBE.[Measures].[Twice] AS [Measures].[Tonnes] * 2");
        assertThat(statement.cube()).isInstanceOf(CubeReference.Current.class);
        name(statement.body().compoundId(), "Measures", "Twice");
        call(statement.body().expression(), "*");
    }

    @Test
    void keywordNamedCubeWithProperties(@InjectService MdxParserProvider provider) throws MdxParserException {
        CreateMemberStatement statement = parse(provider,
                "CREATE MEMBER [Coast].[Measures].[Twice] AS [Measures].[Tonnes] * 2, FORMAT_STRING = '#,0', VISIBLE = 1");
        checkCube(statement.cube(), "Coast");
        assertThat(statement.body().memberPropertyDefinitions()).hasSize(2);
    }

    @Test
    void keywordSession(@InjectService MdxParserProvider provider) throws MdxParserException {
        assertThat(parse(provider, "CREATE SESSION MEMBER CURRENTCUBE.[Measures].[Twice] AS [Measures].[Tonnes] * 2")
                .session()).isTrue();
    }

    @Test
    void keywordSets(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement hidden = statement(provider,
                "CREATE HIDDEN SET CURRENTCUBE.[Big Ports] AS TopCount([Port].[Port].[Port].Members, 3, [Measures].[Tonnes])");
        assertThat(hidden).isInstanceOf(CreateSetStatement.class);
        assertThat(((CreateSetStatement) hidden).hidden()).isTrue();

        MdxStatement dynamic = statement(provider,
                "CREATE SESSION DYNAMIC SET CURRENTCUBE.[Top Port] AS TopCount([Port].[Port].[Port].Members, 1, [Measures].[Tonnes])");
        assertThat(dynamic).isInstanceOf(CreateSetStatement.class);
        assertThat(((CreateSetStatement) dynamic).session()).isTrue();
        assertThat(((CreateSetStatement) dynamic).evaluation()).contains(SetEvaluation.DYNAMIC);
    }

    @Test
    void withMember(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement select = statement(provider,
                "WITH MEMBER [Measures].[Twice] AS [Measures].[Tonnes] * 2 SELECT {[Measures].[Twice]} ON 0 FROM [Coast]");
        assertThat(select).isInstanceOf(SelectStatement.class);
        assertThat(((SelectStatement) select).selectWithClauses()).hasSize(1);
    }
}

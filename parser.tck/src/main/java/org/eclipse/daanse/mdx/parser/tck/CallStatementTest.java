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

import java.util.List;

import org.eclipse.daanse.mdx.model.api.CallStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.StringLiteral;
import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.CreateMemberBodyClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

/**
 * CALL of a stored procedure. The procedure is the tree the same text gives as an expression: a method call on the
 * qualifier, a function call when unqualified, a compound id without parentheses. The older ! form gives the same
 * tree as the . form, also inside an expression.
 */
@RequireServiceComponentRuntime
class CallStatementTest {

    private static CallStatement call(MdxParserProvider provider, String mdx) throws MdxParserException {
        MdxStatement statement = provider.newParser(mdx, propertyWords).parseMdxStatement();
        assertThat(statement).isInstanceOf(CallStatement.class);
        return (CallStatement) statement;
    }

    private static MdxExpression expression(MdxParserProvider provider, String mdx) throws MdxParserException {
        return provider.newParser(mdx, propertyWords).parseExpression();
    }

    /** The method {@code name} on the qualifier {@code qualifier}; returns the arguments after the qualifier. */
    private static List<? extends MdxExpression> method(MdxExpression expression, String name, Quoting quoting,
            String... qualifier) {
        assertThat(expression).isInstanceOf(CallExpression.class);
        CallExpression call = (CallExpression) expression;
        assertThat(call.operationAtom()).isEqualTo(new MethodOperationAtom(name));
        assertThat(call.expressions()).isNotEmpty();
        names(call.expressions().get(0), quoting, qualifier);
        return call.expressions().subList(1, call.expressions().size());
    }

    /** A name of one part (an identifier, or a compound id of one part) or a compound id of several. */
    private static void names(MdxExpression expression, Quoting quoting, String... names) {
        List<? extends ObjectIdentifier> ids;
        if (expression instanceof CompoundId compoundId) {
            ids = compoundId.objectIdentifiers();
        } else {
            assertThat(expression).isInstanceOf(NameObjectIdentifier.class);
            ids = List.of((NameObjectIdentifier) expression);
        }
        assertThat(ids).hasSize(names.length);
        for (int i = 0; i < names.length; i++) {
            assertThat(ids.get(i)).isInstanceOf(NameObjectIdentifier.class);
            assertThat(((NameObjectIdentifier) ids.get(i)).name()).isEqualTo(names[i]);
            assertThat(ids.get(i).quoting()).isEqualTo(quoting);
        }
    }

    /**
     * No argument written, {@code x.Proc()}: the method form keeps one empty argument, as in an expression today
     * (ExpressionTest), or none.
     */
    private static void noArguments(List<? extends MdxExpression> arguments) {
        if (!arguments.isEmpty()) {
            assertThat(arguments).hasSize(1);
            assertThat(arguments.get(0)).isInstanceOf(CallExpression.class);
            assertThat(((CallExpression) arguments.get(0)).operationAtom()).isEqualTo(new EmptyOperationAtom());
        }
    }

    private static void number(MdxExpression expression, int value) {
        assertThat(expression).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) expression).value()).isEqualByComparingTo(String.valueOf(value));
    }

    private static void string(MdxExpression expression, String value) {
        assertThat(expression).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) expression).value()).isEqualTo(value);
    }

    @Test
    void qualifiedWithArguments(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL MyAssembly.MyProcedure(1, 'a')");
        var arguments = method(statement.procedure(), "MyProcedure", Quoting.UNQUOTED, "MyAssembly");
        assertThat(arguments).hasSize(2);
        number(arguments.get(0), 1);
        string(arguments.get(1), "a");
        assertThat(statement.cube()).isEmpty();
    }

    @Test
    void bracketedNames(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL [My Assembly].[MyProcedure]([Measures].[Tonnes])");
        var arguments = method(statement.procedure(), "MyProcedure", Quoting.QUOTED, "My Assembly");
        assertThat(arguments).hasSize(1);
        names(arguments.get(0), Quoting.QUOTED, "Measures", "Tonnes");
    }

    @Test
    void emptyArgumentList(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL PortTools.ClearCache()");
        noArguments(method(statement.procedure(), "ClearCache", Quoting.UNQUOTED, "PortTools"));
        assertThat(statement.cube()).isEmpty();
    }

    @Test
    void noArgumentList(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL PortTools.ClearCache");
        assertThat(statement.procedure()).isInstanceOf(CompoundId.class);
        names(statement.procedure(), Quoting.UNQUOTED, "PortTools", "ClearCache");
    }

    @Test
    void lowerCase(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "call PortTools.ClearCache(1)");
        var arguments = method(statement.procedure(), "ClearCache", Quoting.UNQUOTED, "PortTools");
        assertThat(arguments).hasSize(1);
        number(arguments.get(0), 1);
    }

    @Test
    void memberStringAndExpression(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL PortTools.Reload([Port].[Port].&[Kiel], 'Tonnes', 3 * 2)");
        var arguments = method(statement.procedure(), "Reload", Quoting.UNQUOTED, "PortTools");
        assertThat(arguments).hasSize(3);
        assertThat(arguments.get(0)).isInstanceOf(CompoundId.class);
        assertThat(((CompoundId) arguments.get(0)).objectIdentifiers()).hasSize(3);
        string(arguments.get(1), "Tonnes");
        assertThat(arguments.get(2)).isInstanceOf(CallExpression.class);
        assertThat(((CallExpression) arguments.get(2)).operationAtom()).isEqualTo(new InfixOperationAtom("*"));
    }

    @Test
    void onCube(@InjectService MdxParserProvider provider) throws MdxParserException {
        CallStatement statement = call(provider, "CALL PortTools.ClearCache() ON [Coast]");
        noArguments(method(statement.procedure(), "ClearCache", Quoting.UNQUOTED, "PortTools"));
        assertThat(statement.cube()).isPresent();
        assertThat(statement.cube().get().name()).isEqualTo("Coast");
        assertThat(statement.cube().get().quoting()).isEqualTo(Quoting.QUOTED);
    }

    @Test
    void classWithNamespace(@InjectService MdxParserProvider provider) throws MdxParserException {
        var two = method(call(provider, "CALL PortTools.Loader.Reload(1)").procedure(), "Reload", Quoting.UNQUOTED,
                "PortTools", "Loader");
        assertThat(two).hasSize(1);
        number(two.get(0), 1);
        var three = method(call(provider, "CALL PortTools.Harbour.Loader.Reload(1)").procedure(), "Reload",
                Quoting.UNQUOTED, "PortTools", "Harbour", "Loader");
        assertThat(three).hasSize(1);
    }

    @Test
    void unqualified(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxExpression procedure = call(provider, "CALL ClearCache(1, 'a')").procedure();
        assertThat(procedure).isInstanceOf(CallExpression.class);
        CallExpression call = (CallExpression) procedure;
        assertThat(call.operationAtom()).isEqualTo(new FunctionOperationAtom("ClearCache"));
        assertThat(call.expressions()).hasSize(2);
    }

    @Test
    void bangForm(@InjectService MdxParserProvider provider) throws MdxParserException {
        var arguments = method(call(provider, "CALL PortTools!Loader!Reload(1)").procedure(), "Reload",
                Quoting.UNQUOTED, "PortTools", "Loader");
        assertThat(arguments).hasSize(1);
        number(arguments.get(0), 1);
    }

    @Test
    void bangFormInAnExpression(@InjectService MdxParserProvider provider) throws MdxParserException {
        var arguments = method(expression(provider, "PortTools!Loader!Share([Measures].[Tonnes])"), "Share",
                Quoting.UNQUOTED, "PortTools", "Loader");
        assertThat(arguments).hasSize(1);
        names(arguments.get(0), Quoting.QUOTED, "Measures", "Tonnes");

        MdxStatement select = provider.newParser(
                "WITH MEMBER [Measures].[X] AS PortTools!Loader!Share([Measures].[Tonnes]) SELECT {[Measures].[X]} ON 0 FROM [Coast]",
                propertyWords).parseMdxStatement();
        assertThat(select).isInstanceOf(SelectStatement.class);
        CreateMemberBodyClause member = (CreateMemberBodyClause) ((SelectStatement) select).selectWithClauses().get(0);
        method(member.expression(), "Share", Quoting.UNQUOTED, "PortTools", "Loader");
    }

    @Test
    void bangWithOneQualifier(@InjectService MdxParserProvider provider) throws MdxParserException {
        var arguments = method(expression(provider, "PortTools!Share(1)"), "Share", Quoting.UNQUOTED, "PortTools");
        assertThat(arguments).hasSize(1);
    }

    // controls

    @Test
    void methodInAnExpression(@InjectService MdxParserProvider provider) throws MdxParserException {
        var arguments = method(expression(provider, "PortTools.Share([Measures].[Tonnes], 'a')"), "Share",
                Quoting.UNQUOTED, "PortTools");
        assertThat(arguments).hasSize(2);
        var nested = method(expression(provider, "PortTools.Loader.Share([Measures].[Tonnes])"), "Share",
                Quoting.UNQUOTED, "PortTools", "Loader");
        assertThat(nested).hasSize(1);
    }

    @Test
    void selectsStay(@InjectService MdxParserProvider provider) throws MdxParserException {
        MdxStatement withMember = provider.newParser(
                "WITH MEMBER [Measures].[Share] AS PortTools.Share([Measures].[Tonnes]) SELECT {[Measures].[Share]} ON 0 FROM [Coast]",
                propertyWords).parseMdxStatement();
        assertThat(withMember).isInstanceOf(SelectStatement.class);
        CreateMemberBodyClause member = (CreateMemberBodyClause) ((SelectStatement) withMember).selectWithClauses()
                .get(0);
        method(member.expression(), "Share", Quoting.UNQUOTED, "PortTools");

        assertThat(provider.newParser("SELECT {[Measures].[Call]} ON 0 FROM [Coast]", propertyWords)
                .parseMdxStatement()).isInstanceOf(SelectStatement.class);
    }
}

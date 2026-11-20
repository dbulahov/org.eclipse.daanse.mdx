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
package org.eclipse.daanse.mdx.model.api;

import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;

/**
 * CallStatement Statement
 * Syntax CALL SP_Name [ ( SP_Argument [, SP_Argument ...] ) ] [ ON Cube_Expression ]
 * <p>
 * Runs a stored procedure outside a query. {@link #procedure()} is the tree the same text gives as an expression: a
 * method call on the qualifier, e.g. {@code MyAssembly.MyClass.Proc(a, b)} is the method {@code Proc} with the
 * arguments {@code MyAssembly.MyClass}, {@code a}, {@code b}; an unqualified {@code Proc(a)} is a function call; a
 * name without parentheses is a compound id. The older form {@code MyAssembly!MyClass!Proc(a)} gives the same tree.
 * The parser does not resolve the name.
 *
 * @see <a href="https://learn.microsoft.com/en-us/sql/mdx/mdx-data-manipulation-call">CALL</a>
 */
public non-sealed interface CallStatement extends MdxStatement {

    MdxExpression procedure();

    /** The cube after ON; empty without ON, the procedure then runs on the current cube. */
    Optional<NameObjectIdentifier> cube();
}

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

import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.select.SelectCubeClauseSubStatement;

/**
 * CreateSubcubeStatement Statement
 * Syntax CREATE SUBCUBE Cube_Name AS Select_Statement
 *        CREATE SUBCUBE Cube_Name AS NON VISUAL ( Select_Statement )
 * <p>
 * Narrows the visible space of the cube for the statements that follow in the session. The select has the parts of a
 * subselect and is kept as one, {@link SelectCubeClauseSubStatement#nonVisual()} tells whether NON VISUAL was written.
 * The restrictions of the reference (no WITH, NON EMPTY, HAVING, properties) are left to the consumer.
 *
 * @see <a href="https://learn.microsoft.com/en-us/sql/mdx/mdx-data-definition-create-subcube">CREATE SUBCUBE</a>
 */
public non-sealed interface CreateSubcubeStatement extends MdxStatement {

    NameObjectIdentifier cubeName();

    SelectCubeClauseSubStatement subcube();
}

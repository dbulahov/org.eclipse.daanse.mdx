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
package org.eclipse.daanse.mdx.model.api;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;

/**
 * A column of the RETURN list of DRILLTHROUGH: a bare attribute, e.g. {@code [$Port].[Port]}, or one wrapped in a
 * function that picks the value returned, e.g. {@code Key([$Port].[Port], 2)} or {@code Name([$Port].[Port])}.
 * <p>
 * The functions are Name, UniqueName, Key, Caption, MemberValue, CustomRollup, CustomRollupProperties and
 * UnaryOperator.
 */
public interface ReturnItem {

    /** The attribute or measure, without the function around it. */
    CompoundId compoundId();

    /** The function around the attribute as written, e.g. {@code Key}; compared without case. Empty for a bare column. */
    default Optional<String> function() {
        return Optional.empty();
    }

    /** The arguments after the attribute, e.g. the column number N of {@code Key(attribute, N)}. */
    default List<? extends MdxExpression> arguments() {
        return List.of();
    }
}

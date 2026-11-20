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
package org.eclipse.daanse.mdx.model.record;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.ReturnItem;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;

public record ReturnItemR(CompoundId compoundId, Optional<String> function, List<? extends MdxExpression> arguments)
        implements ReturnItem {

    public ReturnItemR {
        Objects.requireNonNull(function, "function must not be null");
        arguments = List.copyOf(arguments);
    }

    /** A bare column. */
    public ReturnItemR(CompoundId compoundId) {
        this(compoundId, Optional.empty(), List.of());
    }
}

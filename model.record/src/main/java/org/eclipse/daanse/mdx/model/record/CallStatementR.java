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
package org.eclipse.daanse.mdx.model.record;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.CallStatement;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;

public record CallStatementR(MdxExpression procedure, Optional<NameObjectIdentifier> cube) implements CallStatement {

    public CallStatementR {
        Objects.requireNonNull(procedure, "procedure must not be null");
        Objects.requireNonNull(cube, "cube must not be null");
    }
}

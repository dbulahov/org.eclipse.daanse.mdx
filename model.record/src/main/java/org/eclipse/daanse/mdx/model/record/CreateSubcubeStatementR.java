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

import org.eclipse.daanse.mdx.model.api.CreateSubcubeStatement;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.select.SelectCubeClauseSubStatement;

public record CreateSubcubeStatementR(NameObjectIdentifier cubeName, SelectCubeClauseSubStatement subcube)
        implements CreateSubcubeStatement {

    public CreateSubcubeStatementR {
        Objects.requireNonNull(cubeName, "cubeName must not be null");
        Objects.requireNonNull(subcube, "subcube must not be null");
    }
}

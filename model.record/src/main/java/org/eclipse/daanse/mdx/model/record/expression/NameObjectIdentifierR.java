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
package org.eclipse.daanse.mdx.model.record.expression;

import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;

public record NameObjectIdentifierR(String name,
                                    Quoting quoting)
        implements NameObjectIdentifier {

    public NameObjectIdentifierR {
        if (!(quoting == Quoting.QUOTED || quoting == Quoting.UNQUOTED)) {
            throw new IllegalArgumentException("NameObjectIdentifier has wrong quoting. quoting should be QUOTED or UNQUOTED");
        }
        // [] and &[] are valid MDX (a member with a NULL or blank key); only an unquoted name needs a character
        if (name == null || (name.isEmpty() && quoting == Quoting.UNQUOTED)) {
            throw new IllegalArgumentException("NameObjectIdentifier name should be not empty");
        }
    }
}

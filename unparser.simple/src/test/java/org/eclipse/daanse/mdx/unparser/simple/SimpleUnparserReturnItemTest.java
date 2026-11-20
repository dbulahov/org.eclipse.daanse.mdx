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
package org.eclipse.daanse.mdx.unparser.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.record.ReturnItemR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.NumericLiteralR;
import org.junit.jupiter.api.Test;

/** The RETURN list of DRILLTHROUGH: bare columns and columns inside Name, Key, ... */
class SimpleUnparserReturnItemTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    private static CompoundId id(String... names) {
        return new CompoundIdR(List.of(names).stream()
                .<ObjectIdentifier>map(n -> new NameObjectIdentifierR(n, Quoting.QUOTED)).toList());
    }

    @Test
    void bareAndWrappedColumns() {
        var items = List.of(new ReturnItemR(id("$Port", "Region")),
                new ReturnItemR(id("$Port", "Port"), Optional.of("Name"), List.of()),
                new ReturnItemR(id("$Time", "Month"), Optional.of("Key"), List.of(new NumericLiteralR(BigDecimal.TWO))),
                new ReturnItemR(id("Shipments", "Tonnes")));
        assertThat(unparser.unparseReturnItems(items)).asString()
                .isEqualTo("RETURN [$Port].[Region],Name([$Port].[Port]),Key([$Time].[Month], 2),[Shipments].[Tonnes]");
    }
}

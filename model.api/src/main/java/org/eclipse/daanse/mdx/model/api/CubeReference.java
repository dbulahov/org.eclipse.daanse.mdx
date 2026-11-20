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

/**
 * The cube a session statement names in its first segment: a cube by name, or
 * {@code CURRENTCUBE}. Whether {@code CURRENTCUBE} can be resolved is up to the
 * engine, not the parser. {@link Omitted} is the positional {@code CREATE <member> = <expression>} of a cube
 * script, which writes no cube at all.
 */
public sealed interface CubeReference permits CubeReference.Named, CubeReference.Current, CubeReference.Omitted {

    /** A cube by name, e.g. {@code [Adventure Works]}. */
    record Named(NameObjectIdentifier name) implements CubeReference {
    }

    /** {@code CURRENTCUBE}. */
    record Current() implements CubeReference {
    }

    /**
     * No cube written: the positional {@code CREATE [Measures].[X] = <expression>} of a cube script. SSAS takes it
     * only inside a cube script; as a session statement it is refused ("No cube specified").
     */
    record Omitted() implements CubeReference {
    }
}

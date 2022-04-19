/**
 * Copyright (c) 2021-2022 Contributors to the SmartHome/J project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.smarthomej.transform.basicprofiles.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.smarthomej.transform.basicprofiles.internal.profiles.RedeliverCommandProfile;

/**
 * Configuration for {@link RedeliverCommandProfile}.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class RedeliverCommandProfileConfig {
    public int redeliverDelayMillis = 1000;
    public int numRedeliveries = 1;

    @Override
    public String toString() {
        return "RedeliverCommandProfileConfig{" + "numRedeliveries=" + numRedeliveries + ", redeliverDelayMillis="
                + redeliverDelayMillis + '}';
    }
}

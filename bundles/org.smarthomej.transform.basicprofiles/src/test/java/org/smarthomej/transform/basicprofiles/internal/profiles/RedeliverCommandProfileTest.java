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
package org.smarthomej.transform.basicprofiles.internal.profiles;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.UnDefType;

/**
 * Basic unit tests for {@link RedeliverCommandProfile}.
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class RedeliverCommandProfileTest {

    @Test
    public void testWhenStateUpdateIsSameAsCommand_noRedeliveries() throws InterruptedException {
        ProfileCallback callback = mock(ProfileCallback.class);
        RedeliverCommandProfile profile = createProfile(callback, 1000, 1);

        profile.onCommandFromItem(OnOffType.ON);
        profile.onStateUpdateFromHandler(OnOffType.ON);

        // Simulate late response from handler
        Thread.sleep(1500);

        verify(callback, times(1)).handleCommand(OnOffType.ON);
    }

    @Test
    public void testWhenNoStateUpdate_singleRedelivery() throws InterruptedException {
        ProfileCallback callback = mock(ProfileCallback.class);
        RedeliverCommandProfile profile = createProfile(callback, 1000, 1);

        profile.onCommandFromItem(OnOffType.ON);

        // Simulate late response from handler
        Thread.sleep(2500);

        verify(callback, times(2)).handleCommand(OnOffType.ON);
    }

    @Test
    public void testWhenNoStateUpdate_maxRedeliveries() throws InterruptedException {
        ProfileCallback callback = mock(ProfileCallback.class);
        RedeliverCommandProfile profile = createProfile(callback, 1000, 3);

        profile.onCommandFromItem(OnOffType.ON);

        // Simulate late response from handler
        Thread.sleep(3500);

        verify(callback, times(4)).handleCommand(OnOffType.ON);
    }

    @Test
    public void testWhenUnexpectedStateUpdate_singleRedelivery() throws InterruptedException {
        ProfileCallback callback = mock(ProfileCallback.class);
        RedeliverCommandProfile profile = createProfile(callback, 1000, 2);

        profile.onCommandFromItem(OnOffType.ON);
        profile.onStateUpdateFromHandler(UnDefType.UNDEF);

        // Simulate late response from handler
        Thread.sleep(1500);
        profile.onStateUpdateFromHandler(OnOffType.ON);

        // Simulate late response from handler
        Thread.sleep(1000);

        verify(callback, times(2)).handleCommand(OnOffType.ON);
    }

    private RedeliverCommandProfile createProfile(ProfileCallback callback, int redeliverDelayMillis,
            int numRedeliveries) {
        ProfileContext context = mock(ProfileContext.class);
        when(context.getExecutorService()).thenReturn(Executors.newScheduledThreadPool(1));
        Configuration config = new Configuration();
        config.put("numRedeliveries", numRedeliveries);
        config.put("redeliverDelayMillis", redeliverDelayMillis);
        when(context.getConfiguration()).thenReturn(config);

        return new RedeliverCommandProfile(callback, context);
    }
}

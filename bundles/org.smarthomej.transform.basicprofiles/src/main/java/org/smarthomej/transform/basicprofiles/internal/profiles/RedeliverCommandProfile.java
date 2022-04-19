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

import static org.smarthomej.transform.basicprofiles.internal.factory.BasicProfilesFactory.REDELIVER_COMMAND_UID;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smarthomej.transform.basicprofiles.internal.config.RedeliverCommandProfileConfig;

/**
 * Redelivers any given command from an Item to a Handler a number of times until thing handler updates state to the
 * same value
 *
 * @author Arne Seime - Initial contribution
 */
@NonNullByDefault
public class RedeliverCommandProfile implements StateProfile {
    private final Logger logger = LoggerFactory.getLogger(RedeliverCommandProfile.class);

    private final ProfileCallback callback;
    private final RedeliverCommandProfileConfig config;
    private final ScheduledExecutorService scheduler;
    private AtomicInteger redeliveryCounter = new AtomicInteger(0);
    private RedeliverCommandRunnable redeliveryRunnable = new RedeliverCommandRunnable();
    private @Nullable ScheduledFuture<?> redeliveryScheduledFuture;

    public RedeliverCommandProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        scheduler = context.getExecutorService();
        config = context.getConfiguration().as(RedeliverCommandProfileConfig.class);
        logger.debug("Configuring profile with parameters: {}", config);

        if (config.numRedeliveries < 0) {
            throw new IllegalArgumentException(
                    String.format("numRetries has to be a non-negative integer but was '%d'.", config.numRedeliveries));
        }

        if (config.redeliverDelayMillis < 0) {
            throw new IllegalArgumentException(String
                    .format("retryDelay has to be a non-negative integer but was '%d'.", config.redeliverDelayMillis));
        }
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return REDELIVER_COMMAND_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        // cancel retry
        cancelPendingRedelivery();
    }

    @Override
    public void onCommandFromItem(final Command command) {
        logger.debug("Received command '{}' from item", command);

        // cancel if retry is in progress
        cancelPendingRedelivery();
        redeliveryRunnable.initRunnable(command);
        // Some bindings update state synchronously
        // Problem: There is no correlation id between commands and corresponding state updates
        callback.handleCommand(command);

        if (redeliveryRunnable.shouldSchedule() && config.numRedeliveries > 0) {
            // try delivery
            logger.debug("Scheduling up to {} attempt(s) every {}ms for redelivery of command '{}'",
                    config.numRedeliveries, config.redeliverDelayMillis, redeliveryRunnable.command);
            redeliveryScheduledFuture = scheduler.scheduleWithFixedDelay(redeliveryRunnable,
                    config.redeliverDelayMillis, config.redeliverDelayMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.handleCommand(command);
    }

    @Override
    public void onStateUpdateFromHandler(State state) {

        logger.debug("Received state '{}' update from Handler", state);
        if (redeliveryRunnable.isAfterCommand() && redeliveryRunnable.matchesState(state)) {
            redeliveryRunnable.markStateUpdated();
            cancelPendingRedelivery();
        }
        callback.sendUpdate(state);
    }

    private void cancelPendingRedelivery() {
        // Workaround for overagressive null checks
        ScheduledFuture<?> localRedeliveryJob = redeliveryScheduledFuture;
        if (localRedeliveryJob != null) {
            localRedeliveryJob.cancel(true);
            redeliveryScheduledFuture = null;
        }
        redeliveryCounter.set(0);
    }

    private class RedeliverCommandRunnable implements Runnable {
        public Command getCommand() {
            // Avoid nullness annotation
            return command == null ? new StringType("") : command;
        }

        public void initRunnable(Command command) {
            this.command = command;
            commandTimestamp = System.nanoTime();
        }

        private Command command = new StringType(""); // Dummy

        private long commandTimestamp = 0;

        private boolean stateUpdatedAfterCommand = false;

        @Override
        public void run() {

            int retryNo = redeliveryCounter.incrementAndGet();

            if (config.numRedeliveries > retryNo) {
                logger.info("Retrying command '{}' to handler, retry count {}, will continue for up to {} retries",
                        command, redeliveryCounter.get(), config.numRedeliveries);
                callback.handleCommand(command);
            } else if (config.numRedeliveries == retryNo) {
                logger.info("Retrying command '{}' to handler, retry count {} - this is the final attempt", command,
                        redeliveryCounter.get());
                callback.handleCommand(command);
            } else {
                logger.warn("Delivering command '{}' to handler failed", command);
                cancelPendingRedelivery();
            }
        }

        public boolean isAfterCommand() {
            return System.nanoTime() > commandTimestamp;
        }

        public boolean matchesState(State state) {
            return command.toString().equals(state.toString());
        }

        public void markStateUpdated() {
            stateUpdatedAfterCommand = true;
        }

        public boolean shouldSchedule() {
            return !stateUpdatedAfterCommand;
        }
    }
}

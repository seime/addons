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
package org.smarthomej.binding.notificationsforfiretv.internal;

import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.APP;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.BKCOLOR;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.DURATION;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FILENAME;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FILENAME2;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FONTSIZE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.FORCE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.INTERRUPT;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.MSG;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.OFFSET_X;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.OFFSET_Y;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.POSITION;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TITLE;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TRANSPARENCY;
import static org.smarthomej.binding.notificationsforfiretv.internal.NotificationsForFireTVBindingConstants.TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;

/**
 * The {@link NotificationsForFireTVHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tom Blum - Initial contribution
 */
@NonNullByDefault
public class NotificationsForFireTVHandler extends BaseThingHandler {
    private NotificationsForFireTVConfiguration config = new NotificationsForFireTVConfiguration();

    public NotificationsForFireTVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        config = getConfigAs(NotificationsForFireTVConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);
        sendNotification(null, null, null);
    }

    public boolean sendNotification(@Nullable String msg, @Nullable String filename, @Nullable String filename2) {
        try {
            // CREATE CONNECTION
            NotificationsForFireTVConnection notificationsForFireTVConnection = new NotificationsForFireTVConnection(
                    config.hostname, 7676);
            // ADD FORM FIELDS
            notificationsForFireTVConnection.addFormField(TYPE, String.valueOf(0));
            notificationsForFireTVConnection.addFormField(TITLE, config.title);
            notificationsForFireTVConnection.addFormField(MSG, msg != null ? msg : "");
            notificationsForFireTVConnection.addFormField(DURATION, String.valueOf(config.duration));
            notificationsForFireTVConnection.addFormField(FONTSIZE, String.valueOf(0));
            notificationsForFireTVConnection.addFormField(POSITION, String.valueOf(config.position));
            notificationsForFireTVConnection.addFormField(BKCOLOR, "#607d8b");
            notificationsForFireTVConnection.addFormField(TRANSPARENCY, String.valueOf(config.transparency));
            notificationsForFireTVConnection.addFormField(OFFSET_X, String.valueOf(config.offsetX));
            notificationsForFireTVConnection.addFormField(OFFSET_Y, String.valueOf(config.offsetY));
            notificationsForFireTVConnection.addFormField(APP, config.title);
            notificationsForFireTVConnection.addFormField(FORCE, String.valueOf(config.force));
            notificationsForFireTVConnection.addFormField(INTERRUPT, String.valueOf(0));
            // ADD FILE PARTS
            if (filename != null) {
                notificationsForFireTVConnection.addFilePart(FILENAME, new File(filename));
            }
            if (filename2 != null) {
                notificationsForFireTVConnection.addFilePart(FILENAME2, new File(filename2));
            }
            // POST FORM
            notificationsForFireTVConnection.send();

            // UPDATE STATUS
            updateStatus(ThingStatus.ONLINE);

            return true;
        } catch (IOException | InterruptedException e) {
            // UPDATE STATUS
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());

            return false;
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(NotificationsForFireTVThingActions.class);
    }
}

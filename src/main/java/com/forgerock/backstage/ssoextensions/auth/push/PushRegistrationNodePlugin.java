/*
 *             ______
 *           /      /  /\
 *          /      /  /  \
 *         /      /  /    \
 *        /      /  /     /
 *       /      /  /     /
 *      /      /  /     /  /\
 *     /      /  /     /  /  \
 *    /______/  /     /  /    \
 *    _______  /     /  /      \
 *    \      \ \    /  /       /
 *     \      \ \  /  /       /
 *      \      \ \/  /       /
 *       \      \   /       /
 *        \      \  \      /
 *         \______\  \____/
 *
 *            ForgeRock®
 *            BackStage
 *
 *
 *   https://backstage.forgerock.com/
 *   Copyright (c) 2013-2019 ForgeRock BackStage All rights reserved.
 */

package com.forgerock.backstage.ssoextensions.auth.push;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;

import java.util.Collections;
import java.util.Map;

/**
 * Core nodes installed by default with no engine dependencies.
 */
public class PushRegistrationNodePlugin extends AbstractNodeAmPlugin {

    private static String currentVersion = "1.0.0";

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return Collections.singletonMap(PushRegistrationNodePlugin.currentVersion,
                Collections.singletonList(PushRegistrationNode.class));
    }

    @Override
    public String getPluginVersion() {
        return PushRegistrationNodePlugin.currentVersion;
    }

}

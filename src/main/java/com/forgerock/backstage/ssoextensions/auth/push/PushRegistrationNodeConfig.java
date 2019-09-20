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

import org.forgerock.openam.annotations.sm.Attribute;

public interface PushRegistrationNodeConfig {

    @Attribute(order = 100)
    default String issuer() {
        return "ForgeRock";
    }

    @Attribute(order = 200)
    default int timeout() {
        return 4;
    }

    @Attribute(order = 300)
    default String color() {
        return "519387";
    }

    @Attribute(order = 400)
    String imgUrl();

    @Attribute(order = 500)
    default boolean generateRecoveryCodes() {
        return true;
    }
}

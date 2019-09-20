/*
 *
 *  * The contents of this file are subject to the terms of the Common Development and
 *  * Distribution License (the License). You may not use this file except in compliance with the
 *  * License.
 *  *
 *  * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 *  * specific language governing permission and limitations under the License.
 *  *
 *  * When distributing Covered Software, include this CDDL Header Notice in each file and include
 *  * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 *  * Header, with the fields enclosed by brackets [] replaced by your own identifying
 *  * information: "Portions copyright [year] [name of copyright owner]".
 *  *
 *  * Copyright 2017-2019 ForgeRock AS.
 *  * Portions copyright 2019 Josh Cross
 *
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

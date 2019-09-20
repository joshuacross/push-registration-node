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

import com.google.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.util.i18n.PreferredLocales;

import java.util.List;

public class PushRegistrationNodeOutcomeProvider implements OutcomeProvider {
    public enum PushRegOutcome {
        SUCCESS("Success"),
        FAILURE("Failure"),
        EXPIRED("Expired");

        private final String displayValue;

        PushRegOutcome(String displayValue) {
            this.displayValue = displayValue;
        }

        private OutcomeProvider.Outcome getOutcome() {
            return new OutcomeProvider.Outcome(name(), displayValue);
        }
    }


    @Override
    public List<OutcomeProvider.Outcome> getOutcomes(PreferredLocales preferredLocales, JsonValue jsonValue) {
        return ImmutableList.of(
                PushRegOutcome.SUCCESS.getOutcome(),
                PushRegOutcome.FAILURE.getOutcome(),
                PushRegOutcome.EXPIRED.getOutcome()
        );
    }
}

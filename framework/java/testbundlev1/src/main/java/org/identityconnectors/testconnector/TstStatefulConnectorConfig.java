/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.identityconnectors.testconnector;

import java.util.UUID;

import org.identityconnectors.framework.spi.StatefulConfiguration;

public class TstStatefulConnectorConfig extends TstConnectorConfig implements StatefulConfiguration {

    private UUID guid;

    public synchronized UUID getGuid() {
        if (null == guid) {
            guid = UUID.randomUUID();
        }
        return guid;
    }

    public void release() {
        guid = null;
    }
}

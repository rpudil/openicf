#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/CDDL-1.0
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://opensource.org/licenses/CDDL-1.0
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package ${package};

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
#if($statefulConnector == 'Y' || $statefulConnector == 'y')import org.identityconnectors.framework.spi.StatefulConfiguration;#end


/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the ${connectorName} Connector.
 *
 */
public class ${connectorName}Configuration extends AbstractConfiguration#if($statefulConnector == 'Y' || $statefulConnector == 'y') implements  StatefulConfiguration#end {


    // Exposed configuration properties.

    /**
     * The connector to connect to.
     */
    private String host;

    /**
     * The Remote user to authenticate with.
     */
    private String remoteUser = null;

    /**
     * The Password to authenticate with.
     */
    private GuardedString password = null;


    /**
     * Constructor.
     */
    public ${connectorName}Configuration() {

    }


    @ConfigurationProperty(order = 1, displayMessageKey = "host.display",
            groupMessageKey = "basic.group", helpMessageKey = "host.help",
            required = true, confidential = false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    @ConfigurationProperty(order = 2, displayMessageKey = "remoteUser.display",
            groupMessageKey = "basic.group", helpMessageKey = "remoteUser.help",
            required = true, confidential = false)
    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "password.display",
            groupMessageKey = "basic.group", helpMessageKey = "password.help",
            confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty.");
        }

        Assertions.blankCheck(remoteUser, "remoteUser");

        Assertions.nullCheck(password, "password");
    }

#if($statefulConnector == 'Y' || $statefulConnector == 'y')
    /**
     * {@inheritDoc}
     */
    public void release() {
    }
#end

}

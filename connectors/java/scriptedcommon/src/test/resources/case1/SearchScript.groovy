/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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


import ObjectCacheLibrary
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder as ICF
import org.forgerock.openicf.misc.scriptedcommon.Logger
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConnectorBase
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ConnectorObject
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter

def action = action as ScriptedConnectorBase.Action
def configuration = configuration as ScriptedConfiguration
def filter = filter as Filter
def log = log as Logger
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def EMPTY = new ObjectClass("__EMPTY__")

switch (objectClass) {
    case ObjectClass.ACCOUNT:

        def resultSet = ObjectCacheLibrary.instance.search(objectClass, filter, options.getSortKeys())

        // Handle the results
        if (null != options.getPageSize()) {
            // Paged Search
            final String pagedResultsCookie = options.getPagedResultsCookie();
            String currentPagedResultsCookie = options.getPagedResultsCookie();
            final Integer pagedResultsOffset =
                null != options.getPagedResultsOffset() ? Math.max(0, options
                        .getPagedResultsOffset()) : 0;
            final Integer pageSize = options.getPageSize();

            int index = 0;
            int pageStartIndex = null == pagedResultsCookie ? 0 : -1;
            int handled = 0;

            for (ConnectorObject entry : resultSet) {
                if (pageStartIndex < 0 && pagedResultsCookie.equals(entry.getName().getNameValue())) {
                    pageStartIndex = index + 1;
                }

                if (pageStartIndex < 0 || index < pageStartIndex) {
                    index++;
                    continue;
                }

                if (handled >= pageSize) {
                    break;
                }

                if (index >= pagedResultsOffset + pageStartIndex) {
                    if (handler(entry)) {
                        handled++;
                        currentPagedResultsCookie = entry.getName().getNameValue();
                    } else {
                        break;
                    }
                }
                index++;
            }

            if (index == resultSet.size()) {
                currentPagedResultsCookie = null;
            }

            return new SearchResult(currentPagedResultsCookie, resultSet.size() - index);
        } else {
            // Normal Search
            for (ConnectorObject entry : resultSet) {
                if (!handler(entry)) {
                    break;
                }
            }
            return new SearchResult()
        }

        break
    case ObjectClass.GROUP:
        handler(
                ICF.co {
                    uid '12'
                    id '12'
                    attribute {
                        name 'sureName'
                        value 'Foo'
                    }
                    attribute {
                        name 'lastName'
                        value 'Bar'
                    }
                    attribute {
                        name 'groups'
                        values 'Group1', 'Group2'
                    }
                    attribute 'active', true
                    attribute 'NULL'
                }
        )
        handler({
            uid '13'
            id '13'
            attribute {
                name 'sureName'
                value 'Foo'
            }
            attribute {
                name 'lastName'
                value 'Bar'
            }
            attribute {
                name 'groups'
                values 'Group1', 'Group2'
            }
            attribute 'active', true
            attribute 'NULL'
            attributes(new Attribute('emails', [
                    [
                            "address": "foo@example.com",
                            "type": "home",
                            "customType": "",
                            "primary": true]
            ]))
        }
        )
        return new SearchResult()
        break
    case EMPTY:
        return new SearchResult()
        break
    default:
        throw UnsupportedOperationException("Search operation of type:" + objectClass)
}


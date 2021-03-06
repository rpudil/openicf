/*
 * 
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 * 
 * $Id$
 */
package org.forgerock.openicf.connectors.xmlro.util;

import java.util.Arrays;
import org.identityconnectors.common.security.GuardedString;

public class GuardedStringAccessor implements GuardedString.Accessor {

    public static final String code_id = "$Id$";
    private char[] _array;

    public void access(char[] clearChars) {
        _array = new char[clearChars.length];
        System.arraycopy(clearChars, 0, _array, 0, _array.length);
    }

    public char[] getArray() {
        return _array;
    }

    public void clear() {
        Arrays.fill(_array, 0, _array.length, ' ');
    }
}
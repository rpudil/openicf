/*
 *
 * Copyright (c) 2010-2012 ForgeRock Inc. All Rights Reserved
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
package org.forgerock.openicf.connectors.xmlro;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import org.forgerock.openicf.connectors.xmlro.query.QueryBuilder;
import org.forgerock.openicf.connectors.xmlro.query.abstracts.Query;
import org.forgerock.openicf.connectors.xmlro.xsdparser.SchemaParser;
import static org.forgerock.openicf.connectors.xmlro.XmlConnectorTestUtil.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Set;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;

public class XMLHandlerTests {

    private static XMLHandler handler;
    //private static final String XML_FILE_PATH = "test/xml_store/test.xml";
    private static ConnectorObject existingUsrConObj;

    @BeforeClass
    public static void setUp() {

        XMLConfiguration config = new XMLConfiguration();
        config.setXmlFilePath(getRandomXMLFile());
        config.setXsdFilePath(XSD_SCHEMA_FILEPATH);
        config.setCreateFileIfNotExists(true);
        SchemaParser parser = new SchemaParser(XMLConnector.class, config.getXsdFilePath());

        handler = new ConcurrentXMLHandler(config, parser.parseSchema(), parser.getXsdSchema());
        handler.init();
        
        Set<Attribute> attributes = getRequiredAccountAttributes();
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_MS_EMPLOYED, ATTR_ACCOUNT_VALUE_MS_EMPLOYED));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_SIXTH_SENSE, ATTR_ACCOUNT_VALUE_SIXTH_SENSE));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_FIRST_NAME, ATTR_ACCOUNT_VALUE_FIRST_NAME));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_FIRST_LETTER_LAST_NAME, ATTR_ACCOUNT_VALUE_FIRST_LETTER_LAST_NAME));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_EMAIL, ATTR_ACCOUNT_VALUE_EMAIL_1, ATTR_ACCOUNT_VALUE_EMAIL_2, "whooot@foo.com"));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_SECRET_PIN, new GuardedString(ATTR_ACCOUNT_VALUE_SECRET_PIN.toCharArray())));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_OVERTIME_COMISSION, ATTR_ACCOUNT_VALUE_OVERTIME_COMMISSION));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_PERMANTENT_EMPLOYEE, ATTR_ACCOUNT_VALUE_PERMANENT_EPLOYEE));
        attributes.add(AttributeBuilder.build(ATTR_ACCOUNT_YEARS_EMPLOYED, ATTR_ACCOUNT_VALUE_YEARS_EPLOYED));

        handler.create(ObjectClass.ACCOUNT, attributes);

        // INITIALIZE QUERY FOR TESTING
        XMLFilterTranslator filterTranslator = new XMLFilterTranslator(true);

        EqualsFilter filter = new EqualsFilter(AttributeBuilder.build(ATTR_ACCOUNT_FIRST_NAME, ATTR_ACCOUNT_VALUE_FIRST_NAME));
        Query query = filterTranslator.createEqualsExpression(filter, false);
        QueryBuilder queryBuilder = new QueryBuilder(query, ObjectClass.ACCOUNT);

        ArrayList<ConnectorObject> hits = (ArrayList<ConnectorObject>) handler.search(queryBuilder.toString(), ObjectClass.ACCOUNT);
        existingUsrConObj = hits.get(0);
    }

    @Test
    public void searchWithEmptyQueryShouldReturnSizeZero() {
        Collection<ConnectorObject> hits = handler.search("", null);

        AssertJUnit.assertEquals(0, hits.size());
    }

    @Test
    public void fistNameShouldContainValueOfTypeString() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_FIRST_NAME);
        List<Object> values = attribute.getValue();
        Object val = values.get(0);

        AssertJUnit.assertTrue(val instanceof String);
    }

    @Test
    public void yearsEmployedShouldContainValueOfTypeInteger() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_YEARS_EMPLOYED);
        List<Object> values = attribute.getValue();
        Object val = values.get(0);

        AssertJUnit.assertTrue(val instanceof Integer);
    }

    @Test
    public void overtimeCommissionShouldContainValueOfTypeDouble() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_OVERTIME_COMISSION);
        List<Object> values = attribute.getValue();
        Object val = values.get(0);

        AssertJUnit.assertTrue(val instanceof Double);
    }

    @Test
    public void permanentEmployeeShouldContainValueOfTypeBoolean() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_PERMANTENT_EMPLOYEE);
        List<Object> values = attribute.getValue();
        Object val = values.get(0);

        AssertJUnit.assertTrue(val instanceof Boolean);
    }

    @Test
    public void accessingNotReadableFieldShouldReturnNull() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_SECRET_PIN);
        AssertJUnit.assertNull(attribute);

        // testing readable field
        attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_YEARS_EMPLOYED);
        AssertJUnit.assertNotNull(attribute);
    }

    @Test
    public void readingMultivaluedFieldForEmailShouldReturnThreeResults() {
        Attribute attribute = existingUsrConObj.getAttributeByName(ATTR_ACCOUNT_EMAIL);
        int values = attribute.getValue().size();

        AssertJUnit.assertEquals(3, values);
    }
}

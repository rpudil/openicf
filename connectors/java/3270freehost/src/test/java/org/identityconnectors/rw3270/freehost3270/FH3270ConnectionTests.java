/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.rw3270.freehost3270;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.script.ScriptBuilder;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.patternparser.MapTransform;
import org.identityconnectors.rw3270.RW3270Configuration;
import org.identityconnectors.rw3270.RW3270Connection;
import org.identityconnectors.test.common.PropertyBag;
import org.identityconnectors.test.common.TestHelpers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class FH3270ConnectionTests {
    // Connector Configuration information
    //
    private static String HOST_NAME;
    private static String SYSTEM_PASSWORD;
    private static String SYSTEM_USER;

    private static final int     HOST_TELNET_PORT    = 23;
    private static final Boolean USE_SSL             = Boolean.FALSE;
    private static final int     SHORT_WAIT          = 5000;
    private static final String  READY               = "\\sREADY\\s{74}";
    private static final String  CONTINUE            = "\\s\\*\\*\\*\\s{76}";

    @BeforeClass
    public static void before() {
        PropertyBag testProps = TestHelpers.getProperties(FH3270Connection.class);
        HOST_NAME         = testProps.getStringProperty("HOST_NAME");
        SYSTEM_PASSWORD   = testProps.getStringProperty("SYSTEM_PASSWORD");
        SYSTEM_USER       = testProps.getStringProperty("SYSTEM_USER");
        System.out.println("HOST NAME="+HOST_NAME);
        System.out.println("SYSTEM_USER="+SYSTEM_USER);
    }

    private static MapTransform fillInPatternNodes(String parserString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader((parserString))));
        NodeList elements = document.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++)
            if (elements.item(i) instanceof Element) {
                return new MapTransform((Element) elements.item(i));
            }
        return null;
    }

    /*
     *  <opt><apMatch><t offset='-1'> UID= </t></apMatch><AttrParse><int name='OMVS.UID' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> HOME= </t></apMatch><AttrParse><str name='OMVS.HOME' len='73' trim='true'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> PROGRAM= </t></apMatch><AttrParse><str name='OMVS.PROGRAM' len='70' trim='true'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> CPUTIMEMAX= </t></apMatch><AttrParse><int name='OMVS.CPUTIMEMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> ASSIZEMAX= </t></apMatch><AttrParse><int name='OMVS.ASSIZEMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> FILEPROCMAX= </t></apMatch><AttrParse><int name='OMVS.FILEPROCMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> PROCUSERMAX= </t></apMatch><AttrParse><int name='OMVS.PROCUSERMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> THREADSMAX= </t></apMatch><AttrParse><int name='OMVS.THREADSMAX' noval='NONE'/></AttrParse></opt>
     *  <opt><apMatch><t offset='-1'> MMAPAREAMAX= </t></apMatch><AttrParse><int name='OMVS.MMAPAREAMAX' noval='NONE' /></AttrParse></opt>
     */
    @Test
    public void testOmvsParser() {
        String omvsParser =
            "<MapTransform>" +
            "  <PatternNode key='OMVS.HOME' pattern='HOME= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.PROGRAM' pattern='PROGRAM= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.CPUTIMEMAX' pattern='CPUTIMEMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.ASSIZEMAX' pattern='ASSIZEMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.FILEPROCMAX' pattern='FILEPROCMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.PROCUSERMAX' pattern='PROCUSERMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.THREADSMAX' pattern='THREADSMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='OMVS.MMAPAREAMAX' pattern='MMAPAREAMAX= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";

        OurConfiguration configuration = createConfiguration();
        try {
            RW3270Connection connection = createLoggedinConnection(configuration);
            try {
                // Now, display a user's OMVS info
                //
                String command = "LISTUSER "+SYSTEM_USER+" NORACF OMVS";
                String line = executeCommand(connection, command);
                if (line.contains("NO OMVS INFO")) {
                    // Oh, well
                } else {
                    MapTransform transform = fillInPatternNodes(omvsParser);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                    AssertJUnit.assertNotNull(attributes.get("TSO.MAXSIZE"));
                    AssertJUnit.assertNotNull(attributes.get("TSO.USERDATA"));
                    AssertJUnit.assertNotNull(attributes.get("TSO.JOBCLASS"));
                }
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            AssertJUnit.fail(e.toString());
        }
    }

	private FH3270Connection createLoggedinConnection(
			OurConfiguration configuration) throws NamingException {
		FH3270Connection connection = new FH3270Connection(configuration);
        boolean succeeded = false;
        try {
        	connection.loginUser();
        	succeeded = true;
        } finally {
        	if (!succeeded)
        		connection.dispose();
        }
		return connection;
	}

    @Test
    public void testCicsParser() {
        OurConfiguration configuration = createConfiguration();

        String cicsParser =
            "<MapTransform>" +
            "  <PatternNode key='CICS.OPCLASS' pattern='OPCLASS= (.*?\\s*\\n(?:\\s{10}[^\\n]*\\n)*)' optional='true' reset='false'>" +
            "     <SplitTransform splitPattern='\\s+'/>" +
            "  </PatternNode>" +
            "  <PatternNode key='CICS.OPIDENT' pattern='OPIDENT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.OPPRTY'  pattern='OPPRTY= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.TIMEOUT' pattern='TIMEOUT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='CICS.XRFSOFF' pattern='XRFSOFF= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";

        try {
            RW3270Connection connection = createLoggedinConnection(configuration);
            try {
                // Now, display a user's CICS info
                //
                String command = "LISTUSER CICSUSER NORACF CICS";
                String line = executeCommand(connection, command);
                MapTransform transform = fillInPatternNodes(cicsParser);
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                AssertJUnit.assertNotNull(attributes.get("CICS.XRFSOFF"));
                AssertJUnit.assertTrue(attributes.get("CICS.OPCLASS") instanceof List);
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            AssertJUnit.fail(e.toString());
        }
    }

    @Test
    public void testRacfParser() {
        OurConfiguration configuration = createConfiguration();

        String racfParser =
            "<MapTransform>\n" +
            "  <PatternNode key='RACF.USERID' pattern='USER=(\\w{1,8})' optional='false' reset='false'/>\n" +
            "  <PatternNode key='RACF.NAME' pattern='NAME=(.*?)\\s+(?=OWNER=)' optional='false' reset='false'/>\n" +
            "  <PatternNode key='RACF.OWNER' pattern='OWNER=(\\w{1,8})' optional='false' reset='false'>\n" +
            "    <SubstituteTransform pattern='^$' substitute='UNKNOWN'/>\n" +
            "  </PatternNode>\n" +
            "  <PatternNode key='RACF.DFLTGRP' pattern='DEFAULT-GROUP=(\\w{1,8})' optional='false' reset='false'/>\n" +
            "  <PatternNode key='RACF.PASSDATE' pattern='PASSDATE=(\\S{0,6})' optional='false' reset='false'/>\n" +
            "  <PatternNode key='RACF.PASSWORD INTERVAL' pattern='PASS-INTERVAL=(\\S*)' optional='false' reset='false'/>\n" +
            "  <PatternNode key='RACF.PHRASEDATE' pattern='PHRASEDATE=(.*?)\\s+\\n' optional='true' reset='false'/>\n" +
            "  <PatternNode key='RACF.ATTRIBUTES' pattern='((ATTRIBUTES=.*\\n\\s*)+)' optional='true' reset='false'>\n" +
            "    <SubstituteTransform pattern='ATTRIBUTES=(\\S+)\\s+' substitute='$1 '/>\n" +
            "    <SubstituteTransform pattern='(.*)\\s' substitute='$1'/>\n" +
            "    <SubstituteTransform pattern='^$' substitute='NONE'/>\n" +
            "    <SplitTransform splitPattern='\\s+'/>\n" +
            "  </PatternNode>\n" +
            //"  <PatternNode key='RACF.CLAUTH' pattern='CLASS AUTHORIZATIONS=([^\\n]*(\\s{23}.+\\n)*)' optional='true' reset='false'>\n" +
            //"    <SubstituteTransform pattern='(.*)\\s' substitute='$1'/>\n" +
            //"    <SplitTransform splitPattern='\\s+'/>\n" +
            //"  </PatternNode>\n" +
            "  <PatternNode key='RACF.DATA' pattern='INSTALLATION-DATA=([^\\n]*(\\s{20}.+\\n)*)' optional='true' reset='false'>\n" +
            "    <SubstituteTransform pattern='^(.{50})[^\\n]+' substitute='$1'/>\n" +
            "    <SubstituteTransform pattern='\\n\\s{20}(.{50})[^\\n]+' substitute='$1'/>\n" +
            "    <SubstituteTransform pattern='\\n' substitute=''/>\n" +
            "    <SubstituteTransform pattern='^$' substitute='NO-INSTALLATION-DATA'/>\n" +
            "  </PatternNode>\n" +
            "  <PatternNode key='RACF.GROUPS' pattern='((?:\\s+GROUP=\\w+\\s+AUTH=\\w*\\s+CONNECT-OWNER=(?:[^\\n]+\\n){4})+)' optional='true' reset='false'>\n" +
            "    <SubstituteTransform pattern='.*?GROUP=(\\w+)\\s+AUTH=.+?CONNECT-OWNER=\\w+([^\\n]+\\n){4}' substitute='$1 '/>\n" +
            "    <SubstituteTransform pattern='^\\s+(.*)' substitute='$1'/>\n" +
            "    <SubstituteTransform pattern='(\\s+)$' substitute=''/>\n" +
            "    <SplitTransform splitPattern='\\s+'/>\n" +
            "  </PatternNode>\n" +
            "</MapTransform>\n";
        try {
            RW3270Connection connection = createLoggedinConnection(configuration);
            try {
                // Now, display a user's CICS info
                //
                String command = "LISTUSER CICSUSER";
                String line = executeCommand(connection, command);
                System.out.println(line);
                MapTransform transform = fillInPatternNodes(racfParser);
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                System.out.println(attributes);
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            AssertJUnit.fail(e.toString());
        }
    }

    @Test
    public void testTsoParser() {
        OurConfiguration configuration = createConfiguration();

        String tsoParser =
            "<MapTransform>" +
            "  <PatternNode key='TSO.ACCTNUM' pattern='ACCTNUM= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.HOLDCLASS' pattern='HOLDCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.JOBCLASS' pattern='JOBCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.MSGCLASS' pattern='MSGCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.PROC' pattern='PROC= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.SIZE' pattern='SIZE= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.MAXSIZE' pattern='MAXSIZE= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.SYSOUTCLASS' pattern='SYSOUTCLASS= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.PROC' pattern='PROC= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.UNIT' pattern='UNIT= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.USERDATA' pattern='USERDATA= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "  <PatternNode key='TSO.COMMAND' pattern='COMMAND= (.*?)\\s*\\n' optional='true' reset='false'/>" +
            "</MapTransform>";

        try {
            RW3270Connection connection = createLoggedinConnection(configuration);
            try {
                // Now, display a user's TSO info
                //
                String command = "LISTUSER "+SYSTEM_USER+" NORACF TSO";
                String line = executeCommand(connection, command);
                MapTransform transform = fillInPatternNodes(tsoParser);
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>)transform.transform(line);
                AssertJUnit.assertNotNull(attributes.get("TSO.MAXSIZE"));
                AssertJUnit.assertNotNull(attributes.get("TSO.USERDATA"));
                AssertJUnit.assertNotNull(attributes.get("TSO.JOBCLASS"));
            } finally {
                connection.dispose();
            }
        } catch (Exception e) {
            AssertJUnit.fail(e.toString());
        }
    }

    private String executeCommand(RW3270Connection connection, String command) {
        connection.resetStandardOutput();
        connection.send("[clear]"+command+"[enter]");
        connection.waitFor(CONTINUE, READY, SHORT_WAIT);
        String line = connection.getStandardOutput();
        line = line.substring(0, line.lastIndexOf(" READY"));
        // break into lines
        //
        int index = line.indexOf(" USER=");
        System.out.println("index="+index);
        if (index>-1)
            line = line.substring(index);
        line = line.replaceAll("(.{80})", "$1\n");
        return line;
    }

    private OurConfiguration createConfiguration() {
        OurConfiguration config = new OurConfiguration();
        config.setHostNameOrIpAddr(HOST_NAME);
        config.setHostTelnetPortNumber(HOST_TELNET_PORT);
        config.setConnectionProperties(null);
        config.setConnectScript(getLoginScript());
        config.setDisconnectScript(getLogoffScript());
        config.setUserName(SYSTEM_USER);
        config.setPassword(new GuardedString(SYSTEM_PASSWORD.toCharArray()));
        config.setEvictionInterval(60000);
        config.setConnectionClassName(FH3270Connection.class.getName());

        OurConnectorMessages messages = new OurConnectorMessages();
        Map<Locale, Map<String, String>> catalogs = new HashMap<Locale, Map<String,String>>();
        Map<String, String> foo = new HashMap<String, String>();
        for (String bundleName : new String[] { "org.identityconnectors.rw3270.Messages", "org.identityconnectors.rw3270.freehost3270.Messages" }) {
            ResourceBundle messagesBundle = ResourceBundle.getBundle(bundleName);
            Enumeration<String> enumeration = messagesBundle.getKeys();
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement();
                foo.put(key, messagesBundle.getString(key));
            }
        }
        catalogs.put(Locale.getDefault(), foo);
        messages.setCatalogs(catalogs);
        config.setConnectorMessages(messages);
        return config;
    }

    private Script getLoginScript() {
        String script =
            "connection.connect();\n" +
            "connection.waitFor(\"PRESS THE ENTER KEY\", SHORT_WAIT);\n" +
            "connection.send(\"TSO[enter]\");\n" +
            "connection.waitFor(\"ENTER USERID -\", SHORT_WAIT);\n" +
            "connection.send(USERNAME+\"[enter]\");\n" +
            "connection.waitFor(\"Password  ===>\", SHORT_WAIT);\n" +
            "connection.send(PASSWORD);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"\\\\*\\\\*\\\\*\", SHORT_WAIT);\n" +
            "connection.send(\"[enter]\");\n" +
            "connection.waitFor(\"Option ===>\", SHORT_WAIT);\n" +
            "connection.send(\"[pf3]\");\n" +
            "connection.waitFor(\" READY\\\\s{74}\", SHORT_WAIT);";
        ScriptBuilder builder = new ScriptBuilder();
        builder.setScriptLanguage("GROOVY");
        builder.setScriptText(script);
        return builder.build();
    }

    private Script getLogoffScript() {
        String script = "connection.send(\"LOGOFF[enter]\");\n";
//            "connection.send(\"LOGOFF[enter]\");\n" +
//            "connection.waitFor(\"=====>\", SHORT_WAIT);\n" +
//            "connection.dispose();\n";
        ScriptBuilder builder = new ScriptBuilder();
        builder.setScriptLanguage("GROOVY");
        builder.setScriptText(script);
        return builder.build();
    }

    public static class TestHandler implements ResultsHandler, Iterable<ConnectorObject> {
        private List<ConnectorObject> objects = new LinkedList<ConnectorObject>();

        public boolean handle(ConnectorObject object) {
            objects.add(object);
            return true;
        }

        public Iterator<ConnectorObject> iterator() {
            return objects.iterator();
        }

        public int size() {
            return objects.size();
        }
    }

    public static class OurConfiguration extends AbstractConfiguration implements RW3270Configuration {
        private Script _connectScript;
        private Script _disconnectScript;
        private String _host;
        private Integer _port;
        private GuardedString _password;
        private String _userName;
        private Integer _evictionInterval;
        private String _connectClass;
        private String[] _connectionProperties;

        public Script getConnectScript() {
            return _connectScript;
        }

        public String getConnectionClassName() {
            return _connectClass;
        }

        public Script getDisconnectScript() {
            return _disconnectScript;
        }

        public String getHostNameOrIpAddr() {
            return _host;
        }

        public Integer getHostTelnetPortNumber() {
            return _port;
        }

        public GuardedString getPassword() {
            return _password;
        }

        public String[] getConnectionProperties() {
            return _connectionProperties;
        }

        public String getUserName() {
            return _userName;
        }

        public void setConnectScript(Script script) {
            _connectScript = script;
        }

        public void setConnectionClassName(String clazz) {
            _connectClass = clazz;
        }

        public void setDisconnectScript(Script script) {
            _disconnectScript = script;
        }

        public void setHostNameOrIpAddr(String host) {
            _host = host;
        }

        public void setHostTelnetPortNumber(Integer port) {
            _port = port;
        }

        public void setPassword(GuardedString password) {
            _password = password;
        }

        public void setConnectionProperties(String[] connectionProperties) {
            _connectionProperties = connectionProperties;
        }

        public void setUserName(String userName) {
            _userName = userName;
        }
        
        public Integer getEvictionInterval() {
            return _evictionInterval;
        }
        
        public void setEvictionInterval(Integer interval) {
            _evictionInterval = interval;
        }
        
        public void validate() {
            
        }
    }

    
    public class OurConnectorMessages implements ConnectorMessages {
        private Map<Locale, Map<String, String>> _catalogs = new HashMap<Locale, Map<String, String>>();

        public String format(String key, String defaultValue, Object... args) {
            Locale locale = CurrentLocale.isSet()?CurrentLocale.get():Locale.getDefault();        	
            Map<String,String> catalog = _catalogs.get(locale);
            String message = catalog.get(key);
            MessageFormat formatter = new MessageFormat(message,locale);
            return formatter.format(args, new StringBuffer(), null).toString();
        }

        public void setCatalogs(Map<Locale,Map<String,String>> catalogs) {
            _catalogs = catalogs;
        }
    }
}

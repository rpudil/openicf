// <copyright file="ExchangeUtility.cs" company="Sun Microsystems, Inc.">
// ====================
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
// 
// The contents of this file are subject to the terms of the Common Development 
// and Distribution License("CDDL") (the "License").  You may not use this file 
// except in compliance with the License.
// 
// You can obtain a copy of the License at 
// http://IdentityConnectors.dev.java.net/legal/license.txt
// See the License for the specific language governing permissions and limitations 
// under the License. 
// 
// When distributing the Covered Code, include this CDDL Header Notice in each file
// and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// ====================
// </copyright>
// <author>Tomas Knappek</author>

namespace Org.IdentityConnectors.Exchange
{
    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.Globalization;
    using System.IO;
    using System.Management.Automation.Runspaces;
    using System.Reflection;
    using Microsoft.Win32;
    using Org.IdentityConnectors.ActiveDirectory;
    using Org.IdentityConnectors.Common;
    using Org.IdentityConnectors.Framework.Common.Objects;
    using Org.IdentityConnectors.Framework.Spi;
    using System.Text.RegularExpressions;

    /// <summary>
    /// Description of ExchangeUtility.
    /// </summary>
    public sealed class ExchangeUtility : CommonUtils
    {
        /// <summary>
        /// class name, used for logging purposes
        /// </summary>
        private static readonly string ClassName = typeof(ExchangeUtility).ToString();

        /// <summary>
        /// Embedded xml resource file containg the object class definitions
        /// </summary>
        private const string FileObjectClassDef = "Org.IdentityConnectors.Exchange.ObjectClasses.xml";

        /// <summary>
        /// Exchange 2007 registry key, used for building the exchange assembly resolver
        /// </summary>
        private const string Exchange2007RegKey = "Software\\Microsoft\\Exchange\\v8.0\\Setup\\";

        /// <summary>
        /// Exchange 2010 registry key, used for building the exchange assembly resolver
        /// </summary>
        // private const string Exchange2010RegKey = "Software\\Microsoft\\ExchangeServer\\v14\\Setup\\";

        /// <summary>
        /// Exchange registry value name, used together with <see cref="Exchange2010RegKey"/> or <see cref="Exchange2007RegKey"/> w.r.t the
        /// Exchange version to manage.
        /// </summary>
        private const string ExchangeRegValueName = "MsiInstallPath";

        /// <summary>
        /// Prevents a default instance of the <see cref="ExchangeUtility" /> class from being created. 
        /// </summary>
        private ExchangeUtility()
        {
        }

        /// <summary>
        /// Creates Exchange 2010 Assembly Resolver, <see cref="ResolveEventHandler"/>
        /// </summary>
        /// <param name="sender">The source of the event</param>
        /// <param name="args">A <see cref="System.ResolveEventArgs"/> that contains the event data</param>
        /// <returns>Assembly resolver that resolves Exchange 2010 assemblies</returns>
//        internal static Assembly AssemblyResolver2010(object sender, ResolveEventArgs args)
//        {
//            // Add path for the Exchange 2010 DLLs
//            if (args.Name.Contains("Microsoft.Exchange"))
//            {
//                string installPath = GetRegistryStringValue(Exchange2010RegKey, ExchangeRegValueName);
//                installPath += "\\bin\\" + args.Name.Split(',')[0] + ".dll";
//                return Assembly.LoadFrom(installPath);
//            }
//
//            return null;
//        }

        /// <summary>
        /// Creates Exchange 2007 Assembly Resolver, <see cref="ResolveEventHandler"/>
        /// </summary>
        /// <param name="sender">The source of the event</param>
        /// <param name="args">A <see cref="System.ResolveEventArgs"/> that contains the event data</param>
        /// <returns>Assembly resolver that resolves Exchange 2007 assemblies</returns>
        internal static Assembly AssemblyResolver2007(object sender, ResolveEventArgs args)
        {
            // Add path for the Exchange 2007 DLLs
            if (args.Name.Contains("Microsoft.Exchange"))
            {
                string installPath = GetRegistryStringValue(Exchange2007RegKey, ExchangeRegValueName);
                installPath += "\\bin\\" + args.Name.Split(',')[0] + ".dll";
                return Assembly.LoadFrom(installPath);
            }

            return null;
        }

        /// <summary>
        /// Get registry value, which is expected to be a string
        /// </summary>
        /// <param name="keyName">Registry Key Name</param>
        /// <param name="valName">Registry Value Name</param>
        /// <returns>Registry value</returns>        
        /// <exception cref="ArgumentNullException">If <paramref name="valName"/> is null</exception>
        /// <exception cref="InvalidDataException">If some problem with the registry value</exception>
        internal static string GetRegistryStringValue(string keyName, string valName)
        {
            const string MethodName = "GetRegistryStringValue";
            Debug.WriteLine(MethodName + "(" + keyName + ", " + valName + ")" + ":entry", ClassName);

            // argument check            
            if (keyName == null)
            {
                keyName = string.Empty;
            }

            if (valName == null)
            {
                throw new ArgumentNullException("valName");
            }

            RegistryKey regKey = Registry.LocalMachine.OpenSubKey(keyName, false);
            try
            {
                if (regKey != null)
                {
                    object val = regKey.GetValue(valName);
                    if (val != null)
                    {
                        RegistryValueKind regType = regKey.GetValueKind(valName);
                        if (!regType.Equals(RegistryValueKind.String))
                        {
                            throw new InvalidDataException(String.Format(
                                CultureInfo.CurrentCulture,
                                "Invalid Registry data type, key name: {0} value name: {1} should be String",
                                keyName,
                                valName));
                        }

                        return Convert.ToString(val, CultureInfo.CurrentCulture);
                    }
                    else
                    {
                        throw new InvalidDataException(String.Format(
                            CultureInfo.CurrentCulture,
                            "Missing value for key name: {0} value name: {1}",
                            keyName,
                            valName));
                    }
                }
                else
                {
                    throw new InvalidDataException(String.Format(
                        CultureInfo.CurrentCulture,
                        "Unable to open registry for key: {0}",
                        keyName));
                }
            }
            finally
            {
                if (regKey != null)
                {
                    regKey.Close();
                }

                Debug.WriteLine(MethodName + ":exit", ClassName);
            }
        }

        /// <summary>
        /// reads the object class info definitions from xml
        /// </summary>
        /// <returns>Dictionary of object classes</returns>
        internal static IDictionary<ObjectClass, ObjectClassInfo> GetOCInfo()
        {
            return GetOCInfo(FileObjectClassDef);
        }

        /// <summary>
        /// Creates command based on the commanf info, reading the calues from attributes
        /// </summary>
        /// <param name="cmdInfo">Command defition</param>
        /// <param name="attributes">Attribute values</param>
        /// <param name="config">Configuration object</param>
        /// <returns>
        /// Ready to execute Command
        /// </returns>
        /// <exception cref="ArgumentNullException">if some of the param is null</exception>
        internal static Command GetCommand(PSExchangeConnector.CommandInfo cmdInfo, ICollection<ConnectorAttribute> attributes, ExchangeConfiguration config)
        {
            Assertions.NullCheck(cmdInfo, "cmdInfo");
            Assertions.NullCheck(attributes, "attributes");

            Trace.TraceInformation("GetCommand: cmdInfo name = {0}", cmdInfo.Name);

            // create command
            Command cmd = new Command(cmdInfo.Name);

            // map name attribute, if mapping specified
            if (!string.IsNullOrEmpty(cmdInfo.NameParameter))
            {                
                object val = GetAttValue(Name.NAME, attributes);
                if (val != null)
                {
                    cmd.Parameters.Add(cmdInfo.NameParameter, val);
                }
            }

            bool emailAddressesPresent = GetAttValues(ExchangeConnector.AttEmailAddresses, attributes) != null;
            bool primarySmtpAddressPresent = GetAttValues(ExchangeConnector.AttPrimarySmtpAddress, attributes) != null;

            if (emailAddressesPresent && primarySmtpAddressPresent)
            {
                throw new ArgumentException(ExchangeConnector.AttEmailAddresses + " and " + ExchangeConnector.AttPrimarySmtpAddress + " cannot be both set.");
            }

            foreach (string attName in cmdInfo.Parameters)
            {
                object val = null;

                //Trace.TraceInformation("GetCommand: processing cmdInfo parameter {0}", attName);

                if (attName.Equals(ExchangeConnector.AttEmailAddresses))
                {
                    IList<object> vals = GetAttValues(attName, attributes);
                    if (vals != null)
                    {
                        List<string> addresses = new List<string>();
                        foreach (object addressAsObject in vals)
                        {
                            addresses.Add(addressAsObject.ToString());
                        }
                        val = addresses.ToArray(); 
                    }
                }
                else
                {
                    val = GetAttValue(attName, attributes);
                    if (val == null && attName.Equals("DomainController"))
                    {
                        // add domain controller if not provided
                        val = ActiveDirectoryUtils.GetDomainControllerName(config);
                    }
                }

                if (val != null)
                {
                    cmd.Parameters.Add(attName, val);
                }                  
            }

            Trace.TraceInformation("GetCommand exit: cmdInfo name = {0}", cmdInfo.Name);
            return cmd;
        }

        /// <summary>
        /// Helper method: Gets attribute value from the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attributes">collection of attribute</param>
        /// <returns>Attribute value as object, null if not found</returns>     
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static object GetAttValue(string attName, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");
            Assertions.NullCheck(attributes, "attributes");

            object value = null;
            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);

            if (attribute != null)
            {
                value = ConnectorAttributeUtil.GetSingleValue(attribute) ?? string.Empty;
            }

            return value;
        }

        /// <summary>
        /// Helper method: Sets attribute value in the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attValue">attribute value (if null, we will remove the attribute from the collection)</param>
        /// <param name="attributes">collection of attribute</param>
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static void SetAttValue(string attName, object attValue, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");
            Assertions.NullCheck(attributes, "attributes");

            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);
            if (attribute != null)
            {
                attributes.Remove(attribute);
            }
            if (attValue != null)
            {
                attributes.Add(ConnectorAttributeBuilder.Build(attName, new object[] { attValue }));
            }
        }

        /// <summary>
        /// Helper method: Gets attribute values from the attribute collection
        /// </summary>
        /// <param name="attName">attribute name</param>
        /// <param name="attributes">collection of attribute</param>
        /// <returns>Attribute value as collection of objects, null if not found</returns>     
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static IList<object> GetAttValues(string attName, ICollection<ConnectorAttribute> attributes)
        {
            Assertions.NullCheck(attName, "attName");
            Assertions.NullCheck(attributes, "attributes");

            ConnectorAttribute attribute = ConnectorAttributeUtil.Find(attName, attributes);

            if (attribute != null)
            {
                return attribute.Value;
            }
            else
            {
                return null;
            }
        }


        /// <summary>
        /// Helper method for filtering the specified attributes from collection of attributes
        /// </summary>
        /// <param name="attributes">Collection of attributes</param>
        /// <param name="names">Attribute names to be filtered out</param>
        /// <returns>Filtered collection of attributes</returns>
        internal static ICollection<ConnectorAttribute> FilterOut(ICollection<ConnectorAttribute> attributes, IList<string> names)
        {
            Assertions.NullCheck(attributes, "attributes");
            if (names == null || names.Count == 0)
            {
                return attributes;
            }
           
            ICollection<ConnectorAttribute> filtered = new List<ConnectorAttribute>();
            foreach (ConnectorAttribute attribute in attributes)
            {
                if (!names.Contains(attribute.Name))
                {
                    filtered.Add(attribute);
                }
            }
            return filtered;
        }

        /// <summary>
        /// Helper method - Replaces specified collection Items        
        /// </summary>        
        /// <param name="col">Input <see cref="ArrayList"/> to be searched for replacement</param>
        /// <param name="map">Replace mappings</param>
        /// <returns>Replaced <see cref="ArrayList"/></returns>        
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ICollection<string> FilterReplace(ICollection<string> col, IDictionary<string, string> map)
        {
            Assertions.NullCheck(col, "col");
            Assertions.NullCheck(map, "map");

            ICollection<string> newcol = CollectionUtil.NewList(col);
            foreach (KeyValuePair<string, string> pair in map)
            {
                if (newcol.Contains(pair.Key))
                {
                    newcol.Remove(pair.Key);
                    newcol.Add(pair.Value);
                }
            }            

            return newcol;
        }

        /// <summary>
        /// Finds the attributes in connector object and rename it according to input array of names, but only
        /// if the atribute name is in attributes to get
        /// </summary>
        /// <param name="cobject">ConnectorObject which attributes should be replaced</param>
        /// <param name="attsToGet">Attributes to get list</param>
        /// <param name="map">Replace mapping</param>
        /// <returns>ConnectorObject with replaced attributes</returns>        
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ConnectorObject ConvertAdAttributesToExchange(ConnectorObject cobject, ICollection<string> attsToGet)
        {
            Assertions.NullCheck(cobject, "cobject");

            var attributes = cobject.GetAttributes();
            var builder = new ConnectorObjectBuilder();

            bool emailAddressPolicyEnabled = true;
            foreach (ConnectorAttribute attribute in attributes)
            {
                string newName;
                if (attribute.Is(ExchangeConnector.AttMsExchPoliciesExcludedADName))
                {
                    if (attribute.Value != null && attribute.Value.Contains("{26491cfc-9e50-4857-861b-0cb8df22b5d7}"))
                    {
                        emailAddressPolicyEnabled = false;
                    }
                }
                else if (ExchangeConnector.AttMapFromAD.TryGetValue(attribute.Name, out newName))
                {
                    var newAttribute = RenameAttribute(attribute, newName);
                    builder.AddAttribute(newAttribute);
                }
                else
                {
                    builder.AddAttribute(attribute);
                }
            }

            builder.AddAttribute(ConnectorAttributeBuilder.Build(ExchangeConnector.AttEmailAddressPolicyEnabled, emailAddressPolicyEnabled));

            copyAttribute(builder, cobject, ExchangeConnector.AttPrimarySmtpAddressADName, ExchangeConnector.AttPrimarySmtpAddress);

            // derive recipient type
            long? recipientTypeDetails =
                ExchangeUtility.GetAttValue(ExchangeConnector.AttMsExchRecipientTypeDetailsADName, cobject.GetAttributes()) as long?;
            String recipientType = null;
            switch (recipientTypeDetails)
            { // see http://blogs.technet.com/b/benw/archive/2007/04/05/exchange-2007-and-recipient-type-details.aspx

                case 1: recipientType = ExchangeConnector.RcptTypeMailBox; break;
                case 128: recipientType = ExchangeConnector.RcptTypeMailUser; break;

                case null:          // we are dealing with user accounts, so we can assume that an account without Exchange information is an ordinary User
                case 65536: recipientType = ExchangeConnector.RcptTypeUser; break;
            }
            if (recipientType != null)
            {
                builder.AddAttribute(ConnectorAttributeBuilder.Build(ExchangeConnector.AttRecipientType, new string[] { recipientType }));
            }
            else
            {
                Trace.TraceInformation("Unknown recipientTypeDetails: {0} ({1})", recipientTypeDetails,
                    ExchangeUtility.GetAttValue(ExchangeConnector.AttMsExchRecipientTypeDetailsADName, cobject.GetAttributes()));
            }

            builder.ObjectClass = cobject.ObjectClass;
            builder.SetName(cobject.Name);
            builder.SetUid(cobject.Uid);
            return builder.Build();
        }

        private static void copyAttribute(ConnectorObjectBuilder builder, ConnectorObject cobject, string src, string dest)
        {
            ConnectorAttribute srcAttr = cobject.GetAttributeByName(src);
            if (srcAttr != null)
            {
                builder.AddAttribute(RenameAttribute(srcAttr, dest));
            }
        }

        /// <summary>
        /// Renames the connector attribute to new name
        /// </summary>
        /// <param name="cattribute">ConnectorAttribute to be renamed</param>
        /// <param name="newName">New attribute name</param>
        /// <returns>Renamed ConnectorAttribute</returns>
        /// <exception cref="ArgumentNullException">If some of the params is null</exception>
        internal static ConnectorAttribute RenameAttribute(ConnectorAttribute cattribute, string newName)
        {
            Assertions.NullCheck(cattribute, "cattribute");
            Assertions.NullCheck(newName, "newName");

            var attBuilder = new ConnectorAttributeBuilder();
            attBuilder.AddValue(cattribute.Value);
            attBuilder.Name = newName;
            return attBuilder.Build();
        }

        /// <summary>
        /// Localized null check
        /// </summary>
        /// <param name="obj">Object to be check for null</param>
        /// <param name="param">Parameter name to be used in exception message</param>
        /// <param name="config">Configuration used for localization purposes</param>
        /// <exception cref="ArgumentNullException">If the passed object is null</exception>
        internal static void NullCheck(object obj, string param, Configuration config)
        {
            if (obj == null)
            {
                throw new ArgumentNullException(config.ConnectorMessages.Format(
                            "ex_argument_null", "The Argument [{0}] can't be null", param));
            }
        }

        /// <summary>
        /// Builds new Operation options and add the specified attribute names as 
        /// AttributesToGet (add to existing AttributesToGet)
        /// </summary>
        /// <param name="options">Existing Operation Options</param>
        /// <param name="attNames">attribute names to be add to AttributeToGet</param>
        /// <returns>New Operation Options</returns>
        internal static OperationOptions AddAttributeToOptions(OperationOptions options, params string[] attNames)
        {
            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder(options);
            List<string> attsToGet = new List<string>();
            if (options.AttributesToGet != null)
            {
                attsToGet.AddRange(options.AttributesToGet);
            }

            foreach (string attName in attNames)
            {
                attsToGet.Add(attName);
            }

            optionsBuilder.AttributesToGet = attsToGet.ToArray();
            return optionsBuilder.Build();
        }
    }
}

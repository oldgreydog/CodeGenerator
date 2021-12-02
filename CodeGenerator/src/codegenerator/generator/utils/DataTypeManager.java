/*
	Copyright 2020 Wes Kaylor

	This file is part of CodeGenerator.

	CodeGenerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	CodeGenerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with CodeGenerator.  If not, see <http://www.gnu.org/licenses/>.
 */


package codegenerator.generator.utils;



import java.io.*;
import java.util.*;

import coreutil.config.*;
import coreutil.logging.*;



/**
	<p>This is used by the type conversion tags to load data type mapping files into memory and then retrieve values.</p>

	<p>Refer to {@link codegenerator.generator.tags.TypeConvert} for more discussion of the data type
	file.</p>

	<br><p>NOTE: Initializing this manager requires that one or more {@link codegenerator.generator.tags.TypeConvertLoadFile} tags
	be added to the template files, where appropriate, to load the desired type config files.</p>

 */
public class DataTypeManager {

	// Type map config names
	static private final String		CONFIG_NODE_DATA_TYPE_MAPS					= "dataTypeMaps";
	static private final String		CONFIG_NODE_TYPE_MAP						= "typeMap";
	static private final String		CONFIG_NODE_TYPE							= "type";

	static private final String		CONFIG_VALUE_TARGET_LANGUAGE				= "targetLanguage";
	static private final String		CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER	= "targetTypeFieldDelimiter";
	static private final String		CONFIG_VALUE_SOURCE_TYPE					= "sourceType";
	static private final String		CONFIG_VALUE_TARGET_TYPE					= "targetType";

	// Static members
	static private TreeMap<String, TreeMap<String, TreeMap<String, String>>>		s_typeMap	= new TreeMap<String, TreeMap<String, TreeMap<String, String>>>();


	//===========================================
	static public boolean LoadConfigFile(String p_configFilePathname) {
		try {
			File t_configFile = new File(p_configFilePathname);
			if (!t_configFile.exists() || !t_configFile.isFile()) {
				Logger.LogError("DataTypeManager.LoadConfigFile() failed to find the file [" + p_configFilePathname + "].");
				return false;
			}

			FileConfigValueSet t_newTypeFileSet = new FileConfigValueSet();
			if (!t_newTypeFileSet.Load(p_configFilePathname)) {
				Logger.LogError("DataTypeManager.LoadConfigFile() failed to load the config file [" + p_configFilePathname + "].");
				return false;
			}

			// Since this config file doesn't need to go into the ConfigManager so that it's visible to the rest of the code, we can just use it directly for the purposes of initializing this type map.
			ConfigNode t_dataTypeMaps = t_newTypeFileSet.GetNode(CONFIG_NODE_DATA_TYPE_MAPS);
			if (t_dataTypeMaps == null) {
				Logger.LogError("DataTypeManager.LoadConfigFile() failed to find the data type manager config node [" + CONFIG_NODE_DATA_TYPE_MAPS + "].");
				return false;
			}

			TreeMap<String, TreeMap<String, String>>	t_targetLanguageMap;
			TreeMap<String, String>						t_sourceTypeMap				= null;
			String										t_targetLanguageName;
			String										t_targetTypeFieldDelimiter;
			String										t_sourceType;
			String										t_targetTypeParts[];

			for (ConfigNode t_nextChildNode: t_dataTypeMaps.GetChildNodeList()) {
				if (!t_nextChildNode.IsValue()) {
					if (t_nextChildNode.GetName().equals(CONFIG_NODE_TYPE_MAP)) {
						t_targetLanguageName = t_nextChildNode.GetNodeValue(CONFIG_VALUE_TARGET_LANGUAGE);
						if ((t_targetLanguageName == null) || t_targetLanguageName.isEmpty()) {
							Logger.LogError("DataTypeManager.LoadConfigFile() did not find the config value [" + CONFIG_VALUE_TARGET_LANGUAGE + "].");
							return false;
						}

						t_targetLanguageMap = s_typeMap.get(t_targetLanguageName);
						if (t_targetLanguageMap != null) {
							Logger.LogError("DataTypeManager.LoadConfigFile() found a pre-existing instance of a source type map for the language [" + t_targetLanguageName + "].");
							return false;
						}

						t_targetLanguageMap = new TreeMap<String, TreeMap<String, String>>();
						s_typeMap.put(t_targetLanguageName, t_targetLanguageMap);

						t_targetTypeFieldDelimiter = t_nextChildNode.GetNodeValue(CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER);
						if ((t_targetTypeFieldDelimiter == null) || t_targetTypeFieldDelimiter.isEmpty()) {
							Logger.LogError("DataTypeManager.LoadConfigFile() did not find the config value [" + CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER + "].");
							return false;
						}

						for (ConfigNode t_nextSourceTypeNode: t_nextChildNode.GetChildNodeList()) {
							if (t_nextSourceTypeNode.IsValue()) {
								//Logger.LogWarning("DataTypeManager.LoadConfigFile() found an unknown config value [" + t_nextSourceTypeNode.GetName() + "].");
								continue;
							}
							else if (!t_nextSourceTypeNode.GetName().equalsIgnoreCase(CONFIG_NODE_TYPE)) {
								Logger.LogWarning("DataTypeManager.LoadConfigFile() found an unknown config node [" + t_nextSourceTypeNode.GetName() + "].");
								continue;
							}

							t_sourceType = null;
							for (ConfigNode t_nextTypeField: t_nextSourceTypeNode.GetChildNodeList()) {
								if (t_nextTypeField.GetName().equalsIgnoreCase(CONFIG_VALUE_SOURCE_TYPE)) {
									t_sourceType = ((ConfigValue)t_nextTypeField).GetValue();

									if (t_targetLanguageMap.containsKey(t_sourceType)) {
										Logger.LogError("DataTypeManager.LoadConfigFile() - the type map for target language [" + t_targetLanguageMap + "] already contains source type [" + t_sourceType + "].");
										return false;
									}

									t_sourceTypeMap = new TreeMap<String, String>();
									t_targetLanguageMap.put(t_sourceType, t_sourceTypeMap);
									continue;
								}
								else if (t_nextTypeField.GetName().equalsIgnoreCase(CONFIG_VALUE_TARGET_TYPE)) {
									if (t_sourceType == null) {
										Logger.LogError("DataTypeManager.LoadConfigFile() found a [" + CONFIG_VALUE_TARGET_TYPE + "] entry before a [" + CONFIG_VALUE_SOURCE_TYPE + "] entry was found.");
										return false;
									}

									t_targetTypeParts = ((ConfigValue)t_nextTypeField).GetValue().split(t_targetTypeFieldDelimiter);
									if (t_targetTypeParts.length < 2) {
										Logger.LogError("DataTypeManager.LoadConfigFile() - the target type value [" + t_targetTypeParts[0] + "] for source type [" + t_sourceType + "] must have at least two fields in it.");
										return false;
									}

									if (t_sourceTypeMap.containsKey(t_targetTypeParts[0])) {
										Logger.LogError("DataTypeManager.LoadConfigFile() - the source type [" + t_sourceType + "] already contains groupID entry [" + t_targetTypeParts[0] + "].");
										return false;
									}


									t_sourceTypeMap.put(t_targetTypeParts[0], t_targetTypeParts[1]);
									continue;
								}
							}
						}
					}
					else {
						Logger.LogError("DataTypeManager.LoadConfigFile() does handle config values named [" + t_nextChildNode.GetName() + "].");
						return false;
					}
				}
				else {
					Logger.LogError("DataTypeManager.LoadConfigFile() does handle config nodes named [" + t_nextChildNode.GetName() + "].");
					return false;
				}
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("DataTypeManager.LoadConfigFile() failed with error: ", t_error);
			return false;
		}
	}


	//===========================================
	static public String GetTypeConversion(String p_targetLanguage, String p_sourceType, String p_typeClass) {
		try {
			TreeMap<String, TreeMap<String, String>> t_targetLanguage = s_typeMap.get(p_targetLanguage);
			if (t_targetLanguage == null) {
				Logger.LogError("DataTypeManager.GetTypeConversion() failed to find a source type map for the language [" + p_targetLanguage + "].");
				return null;
			}

			TreeMap<String, String> t_sourceType = t_targetLanguage.get(p_sourceType);
			if (t_sourceType == null) {
				Logger.LogError("DataTypeManager.GetTypeConversion() failed to find a groupID map for the source type [" + p_sourceType + "].");
				return null;
			}

			return t_sourceType.get(p_typeClass);	// It's fine if this returns NULL if there is no p_typeClass for this source type.
		}
		catch (Throwable t_error) {
			Logger.LogException("DataTypeManager.GetTypeConversion() failed with error: ", t_error);
			return null;
		}
	}
}

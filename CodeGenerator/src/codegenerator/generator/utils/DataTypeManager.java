/*
	Copyright 2016 Wes Kaylor

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

	//ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
	static private class SourceType {

		// Data members:
		private String						m_name;
		private TreeMap<String, String>		m_targetTypes	= new TreeMap<>();

		//*****************************
		SourceType(String p_name) {
			m_name = p_name;
		}

		//*****************************
		String GetName() {
			return m_name;
		}

		//*****************************
		boolean SetTargetType(String p_targetType, String p_value) {
			if ((p_targetType == null) || p_targetType.isBlank()) {
				Logger.LogError("SourceType.SetTargetType() received an invalid target type [" + CONFIG_NODE_DATA_TYPE_MAPS + "].");
				return false;
			}

			if (m_targetTypes.containsKey(p_targetType)) {
				Logger.LogError("SourceType.SetTargetType(): source type [" + m_name + "] already contains the target type [" + p_targetType + "].");
				return false;
			}

			m_targetTypes.put(p_targetType, p_value);
			return true;
		}

		//*****************************
		String GetTargetTypeValue(String p_targetType) {
			if ((p_targetType == null) || p_targetType.isBlank()) {
				Logger.LogError("SourceType.GetTargetTypeValue() received an invalid target type [" + CONFIG_NODE_DATA_TYPE_MAPS + "].");
				return null;
			}

			return m_targetTypes.get(p_targetType);
		}
	}

	//ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
	static private class Language {

		// Data members:
		private TreeMap<String, SourceType>	m_sourceTypeMap		= new TreeMap<>();	// Capturing the length of the longest value in each target type lets us customize tab offsets to get the correct output alignment every(?) time.
		private TreeMap<String, Integer>	m_typeMaxSizeMap	= new TreeMap<>();	// Capturing the length of the longest value in each target type lets us customize tab offsets to get the correct output alignment every(?) time.

		//*****************************
		boolean AddSourceType(SourceType p_sourceType) {
			if (p_sourceType == null) {
				Logger.LogError("Language.AddSourceType() received a NULL source type.");
				return false;
			}

			if (m_sourceTypeMap.containsKey(p_sourceType.GetName())) {
				Logger.LogError("Language.AddSourceType() already has a source type with name [" + p_sourceType.GetName() + "].");
				return false;
			}

			m_sourceTypeMap.put(p_sourceType.GetName(), p_sourceType);
			return true;
		}

		//*****************************
		SourceType GetSourceType(String p_sourceTypeName) {
			if ((p_sourceTypeName == null) || p_sourceTypeName.isBlank()) {
				Logger.LogError("Language.AddSourceType() received a NULL source type.");
				return null;
			}

			return m_sourceTypeMap.get(p_sourceTypeName);
		}

		//*****************************
		boolean SetTargetTypeMaxSize(String p_targetTypeName, int p_targetValueSize) {
			if ((p_targetTypeName == null) || p_targetTypeName.isBlank()) {
				Logger.LogError("Language.SetTargetTypeMaxSize() received an invalid target type [" + p_targetTypeName + "].");
				return false;
			}

			if (m_typeMaxSizeMap.containsKey(p_targetTypeName)) {
				int t_maxValueLength = m_typeMaxSizeMap.get(p_targetTypeName);
				if (p_targetValueSize > t_maxValueLength)
					m_typeMaxSizeMap.put(p_targetTypeName, p_targetValueSize);
			}
			else
				m_typeMaxSizeMap.put(p_targetTypeName, p_targetValueSize);

			return true;
		}

		//*****************************
		int GetTargetTypeMaxSize(String p_targetTypeName) {
			if ((p_targetTypeName == null) || p_targetTypeName.isBlank()) {
				Logger.LogError("Language.GetTargetTypeValue() received an invalid target type [" + p_targetTypeName + "].");
				return 0;
			}

			return m_typeMaxSizeMap.get(p_targetTypeName);
		}
	}



	// Type map config names
	static private final String		CONFIG_NODE_DATA_TYPE_MAPS					= "dataTypeMaps";
	static private final String		CONFIG_NODE_TYPE_MAP						= "typeMap";
	static private final String		CONFIG_NODE_TYPE							= "type";

	static private final String		CONFIG_VALUE_TARGET_LANGUAGE				= "targetLanguage";
	static private final String		CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER	= "targetTypeFieldDelimiter";
	static private final String		CONFIG_VALUE_SOURCE_TYPE					= "sourceType";
	static private final String		CONFIG_VALUE_TARGET_TYPE					= "targetType";

	// Static members
	static private TreeMap<String, Language>	s_languageMap	= new TreeMap<>();


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

			Language		t_targetLanguageMap			= null;
			SourceType		t_sourceType				= null;
			String			t_targetLanguageName;
			String			t_targetTypeFieldDelimiter;
			String			t_sourceTypeName;
			String			t_targetTypeParts[];

			for (ConfigNode t_nextChildNode: t_dataTypeMaps.GetChildNodeList()) {
				if (t_nextChildNode.GetName().equals(CONFIG_NODE_TYPE_MAP)) {
					t_targetLanguageName = t_nextChildNode.GetValue(CONFIG_VALUE_TARGET_LANGUAGE).GetStringValue();
					if ((t_targetLanguageName == null) || t_targetLanguageName.isBlank()) {
						Logger.LogError("DataTypeManager.LoadConfigFile() did not find the config value [" + CONFIG_VALUE_TARGET_LANGUAGE + "].");
						return false;
					}

					t_targetLanguageMap = s_languageMap.get(t_targetLanguageName);
					if (t_targetLanguageMap == null) {	// I changed this to allow more than one file to be loaded for the same language.  I don't know why I did it the other way the first time.
						t_targetLanguageMap = new Language();
						s_languageMap.put(t_targetLanguageName, t_targetLanguageMap);
					}

					t_targetTypeFieldDelimiter = t_nextChildNode.GetValue(CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER).GetStringValue();
					if ((t_targetTypeFieldDelimiter == null) || t_targetTypeFieldDelimiter.isBlank()) {
						Logger.LogError("DataTypeManager.LoadConfigFile() did not find the config value [" + CONFIG_VALUE_TARGET_TYPE_FIELD_DELIMITER + "].");
						return false;
					}

					for (ConfigNode t_nextTypeNode: t_nextChildNode.GetChildNodeList()) {
						if (!t_nextTypeNode.GetName().equalsIgnoreCase(CONFIG_NODE_TYPE)) {
							Logger.LogWarning("DataTypeManager.LoadConfigFile() found an unknown config node [" + t_nextTypeNode.GetName() + "].");
							continue;
						}

						t_sourceType		= null;
						t_sourceTypeName	= null;
						for (ConfigValue t_nextTypeField: t_nextTypeNode.GetChildValueList()) {
							if (t_nextTypeField.GetName().equalsIgnoreCase(CONFIG_VALUE_SOURCE_TYPE)) {
								t_sourceTypeName = t_nextTypeField.GetStringValue();
								if ((t_sourceTypeName == null) || t_sourceTypeName.isBlank()) {
									Logger.LogError("DataTypeManager.LoadConfigFile() found a [" + CONFIG_VALUE_SOURCE_TYPE + "] entry that doesn't have a value.");
									return false;
								}

								if (t_sourceType == null)
									t_sourceType = new SourceType(t_sourceTypeName);

								if (!t_targetLanguageMap.AddSourceType(t_sourceType)) {
									Logger.LogError("DataTypeManager.LoadConfigFile() - the target language [" + t_targetLanguageName + "] failed to add source type [" + t_sourceTypeName + "].");
									return false;
								}

								continue;
							}
							else if (t_nextTypeField.GetName().equalsIgnoreCase(CONFIG_VALUE_TARGET_TYPE)) {
								if (t_sourceTypeName == null) {
									Logger.LogError("DataTypeManager.LoadConfigFile() found a [" + CONFIG_VALUE_TARGET_TYPE + "] entry before a [" + CONFIG_VALUE_SOURCE_TYPE + "] entry was found.");
									return false;
								}

								t_targetTypeParts = t_nextTypeField.GetStringValue().split(t_targetTypeFieldDelimiter);
								if (t_targetTypeParts.length < 2) {
									Logger.LogError("DataTypeManager.LoadConfigFile() - the target type value [" + t_targetTypeParts[0] + "] for source type [" + t_sourceTypeName + "] must have at least two fields in it.");
									return false;
								}

								if (!t_sourceType.SetTargetType(t_targetTypeParts[0], t_targetTypeParts[1])) {
									Logger.LogError("DataTypeManager.LoadConfigFile() - the source type [" + t_sourceTypeName + "] failed to add target type entry [" + t_targetTypeParts[0] + "].");
									return false;
								}

								if (!t_targetLanguageMap.SetTargetTypeMaxSize(t_targetTypeParts[0], t_targetTypeParts[1].length())) {
									Logger.LogError("DataTypeManager.LoadConfigFile() - the language [" + t_targetLanguageName + "] failed to set the max size for target type entry [" + t_targetTypeParts[0] + "].");
									return false;
								}

								continue;
							}
						}
					}
				}
				else {
					Logger.LogError("DataTypeManager.LoadConfigFile() does not handle config values named [" + t_nextChildNode.GetName() + "].");
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
	static public String GetTypeConversion(String p_targetLanguage, String p_sourceType, String p_targetType) {
		try {
			Language t_targetLanguage = s_languageMap.get(p_targetLanguage);
			if (t_targetLanguage == null) {
				Logger.LogError("DataTypeManager.GetTypeConversion() failed to find the language [" + p_targetLanguage + "].");
				return null;
			}

			SourceType t_sourceType = t_targetLanguage.GetSourceType(p_sourceType);
			if (t_sourceType == null) {
				Logger.LogError("DataTypeManager.GetTypeConversion() failed to find the source type [" + p_sourceType + "].");
				return null;
			}

			return t_sourceType.GetTargetTypeValue(p_targetType);	// It's fine if this returns NULL if there is no p_targetType for this source type.
		}
		catch (Throwable t_error) {
			Logger.LogException("DataTypeManager.GetTypeConversion() failed with error: ", t_error);
			return null;
		}
	}


	//===========================================
	static public int GetTypeMaxSize(String p_targetLanguage, String p_targetType) {
		try {
			Language t_targetLanguage = s_languageMap.get(p_targetLanguage);
			if (t_targetLanguage == null) {
				Logger.LogError("DataTypeManager.GetTypeMaxSize() failed to find the language [" + p_targetLanguage + "].");
				return 0;
			}

			// This is a String and not an int because the value will be be output as a string from the TypeMaxSize tag for comparison in a tag attribute.
			int t_maxLength = t_targetLanguage.GetTargetTypeMaxSize(p_targetType);
			if (t_maxLength == 0)
				return 0;		// If the target type doesn't exist, we'll just return "0".  It's not critical if the offset is screwed up since the template output will be invalid anyway without the type itself.

			return t_maxLength;
		}
		catch (Throwable t_error) {
			Logger.LogException("DataTypeManager.GetTypeMaxSize() failed with error: ", t_error);
			return 0;
		}
	}
}

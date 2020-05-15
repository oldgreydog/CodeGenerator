/*
Copyright 2019 Wes Kaylor

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


package codegenerator.generator.tags;



import java.io.*;

import codegenerator.generator.utils.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	Loads the type config file into the DataTypeManager so that it is available for any typeConversion
	tags in the template(s).  This will typically be used in the root template file.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%typeConvertLoadFile file = "DataType_Conversion_Java_to_Java.xml" %&gt;</code></pre>

 */
public class TypeConvertLoadFile extends TemplateBlock_Base {

	static public final String		BLOCK_NAME			= "typeConvertLoadFile";

	static public final String		ATTRIBUTE_FILE		= "file";


	// Data members
	private String m_filePath	= null;

	//*********************************
	public TypeConvertLoadFile() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TypeConvertLoadFile.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			// The target language should be a string constant.
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_FILE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeConvertLoadFile.Init() did not find the [" + ATTRIBUTE_FILE + "] attribute that is required for TypeConvertLoadFile tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_filePath = t_nodeAttribute.GetAttributeValueAsString();
			if (m_filePath == null) {
				Logger.LogError("TypeConvertLoadFile.Init() did not get the [" + ATTRIBUTE_FILE + "] value as a string from attribute at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}


			if (!DataTypeManager.LoadConfigFile(m_filePath)) {
				Logger.LogError("TypeConvertLoadFile.Init() failed to load the file [" + m_filePath +"] into the DataTypeManager at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeConvertLoadFile.Init() failed with error at line number [" + p_tagParser.GetLineNumber() + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new TypeConvertLoadFile();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name  :  " + m_name + "\n");

		t_dump.append(p_tabs + "\tFile path : " + ((m_filePath != null) ? m_filePath : "NULL") + "\n");

		return t_dump.toString();
	}
}
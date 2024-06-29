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



import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
<p>Loads the type conversion config file into the DataTypeManager so that it is available for any typeConversion
tags in the template(s).  This will typically be used in the root template file.</p>

<h3>Usage example</h3>

<pre><code><b>&lt;%typeConvertLoadFile file = "DataType_Conversion_Java_to_Java.xml" %&gt;</b></code></pre>

 <h3>Attribute descriptions</h3>

<p><code><b>file</b></code>:  the file path of the file to be loaded.</p>
*/
public class TypeConvertLoadFile extends Tag_Base {

	static public final String		TAG_NAME			= "typeConvertLoadFile";

	static private final String		ATTRIBUTE_FILE		= "file";


	// Data members
	private OptionalEvalValue	m_filePath		= null;
	private boolean				m_fileLoaded	= false;


	//*********************************
	public TypeConvertLoadFile() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TypeConvertLoadFile.Init() failed in the parent Init() at line number [" + m_lineNumber + "].");
				return false;
			}

			// The target language should be a string constant.
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_FILE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeConvertLoadFile.Init() did not find the [" + ATTRIBUTE_FILE + "] attribute that is required for TypeConvertLoadFile tags at line number [" + m_lineNumber + "].");
				return false;
			}

			GeneralBlock t_valueBlock = t_nodeAttribute.GetAttributeValue();
			if ((t_valueBlock == null) || !t_valueBlock.HasContentTags()) {
				Logger.LogError("TypeConvertLoadFile.Init() did not get the [" + ATTRIBUTE_FILE + "] value as a string from attribute at line number [" + m_lineNumber + "].");
				return false;
			}

			m_filePath = new OptionalEvalValue(t_valueBlock);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeConvertLoadFile.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TypeConvertLoadFile();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		if (!m_fileLoaded) {
			String t_filePath = m_filePath.Evaluate(p_evaluationContext);
			if ((t_filePath == null) || t_filePath.isBlank()) {
				Logger.LogError("TypeConvertLoadFile.Evaluate() failed to evaluate the destination directory path.");
				return false;
			}

			if (!DataTypeManager.LoadConfigFile(t_filePath)) {
				Logger.LogError("TypeConvertLoadFile.Evaluate() failed to load the file [" + m_filePath +"] into the DataTypeManager at line number [" + m_lineNumber + "].");
				return false;
			}

			m_fileLoaded = true;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name + "\n");

		t_dump.append(p_tabs + "\tFile path : " + ((m_filePath != null) ? m_filePath : "NULL") + "\n");

		return t_dump.toString();
	}
}

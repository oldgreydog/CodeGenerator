/*
Copyright 2025 Wes Kaylor

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
<p>Enables correct alignment of code where you are using values from <code><b>typeConvert</b></code> in the output and then
using <code><b>tabstop</b></code> in "marker" mode to align the following text.  Since the length of the values being returned
by <code><b>typeConvert</b></code> for a target type can vary wildly, you can't get perfect alignment with a normal <code><b>tabstop</b></code>
unless you make the offset so large that it will cover practically any value length.  But doing it that way results in huge
offsets all the time, even if the values output at a particular point are short ones.  Using <code><b>typeMaxSize</b></code>
lets the offset adjust to only be as large as the largest value in the particular target type.  It will still occasionally
produce over-sized offsets where there is a wide range of value sizes.  However, since the offset is based on the data instead
of a fixed size, it will almost always produce smaller offsets that look much better in the code.</p>

<h3>Usage example</h3>

<pre>	<code><b>&lt;%typeMaxSize targetLanguage = "java" targetType = "builtin" %&gt;</b></code></pre>

<p>This tag, while you may come up with some novel usage, only really makes sense as the value-side of the <code><b>tabstop</b></code>'s
<code><b>offset</b></code> attribute.  For example:</p>

<pre>	<code><b>&lt;%tabStop stopType = "marker" offset = &lt;%typeMaxSize targetLanguage = "java" targetType = "builtin" %&gt; %&gt;</b></code></pre>
 */
public class TypeMaxSize extends Tag_Base {

	static public final String		TAG_NAME						= "typeMaxSize";

	static private final String		ATTRIBUTE_TARGET_LANGUAGE		= "targetLanguage";
	static private final String		ATTRIBUTE_TARGET_TYPE			= "targetType";


	// Data members
	private	String		m_targetLanguage	= null;
	private	String		m_targetType		= null;


	//*********************************
	public TypeMaxSize() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TypeMaxSize.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			// The target language should be a string constant.
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TARGET_LANGUAGE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeMaxSize.Init() did not find the [" + ATTRIBUTE_TARGET_LANGUAGE + "] attribute that is required for TypeMaxSize tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_targetLanguage = t_nodeAttribute.GetAttributeValueAsString();
			if (m_targetLanguage == null) {
				Logger.LogError("TypeMaxSize.Init() did not get the value from attribute [" + ATTRIBUTE_TARGET_LANGUAGE + "] that is required for TypeMaxSize tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}


			// The type ID should be a string constant.
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TARGET_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeMaxSize.Init() did not find the [" + ATTRIBUTE_TARGET_TYPE + "] attribute that is required for TypeMaxSize tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_targetType = t_nodeAttribute.GetAttributeValueAsString();
			if (m_targetType == null) {
				Logger.LogError("TypeMaxSize.Init() did not get the value from attribute [" + ATTRIBUTE_TARGET_TYPE + "] that is required for TypeMaxSize tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeMaxSize.Init() failed with error at line number [" + p_tagParser.GetLineNumber() + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TypeMaxSize();
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
		try {
			if (m_targetLanguage == null) {
				Logger.LogError("TypeMaxSize.Evaluate() was not initialized.");
				return false;
			}

			int t_typeMaxSize = DataTypeManager.GetTypeMaxSize(m_targetLanguage, m_targetType);

			if ((t_typeMaxSize == 0)) {
				Logger.LogError("TypeMaxSize.Evaluate() failed for language [" + m_targetLanguage + "] and target type [" + m_targetType + "].");
				return false;
			}

			p_evaluationContext.GetCursor().Write(Integer.toString(t_typeMaxSize));
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeMaxSize.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name 	+ "\n");

//		if (m_sourceType != null) {
//			t_dump.append("\n\n");
//			t_dump.append(m_sourceType.Dump(p_tabs + "\t"));
//
//		}

		return t_dump.toString();
	}
}

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


package codegenerator.generator.tags;



import java.io.*;

import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
<p>This tag takes a value from the config that has internal delimiters such as "aa_Bb_cC", "AA_bB_Cc", "aa-Bb-cC" or "AA-bB-Cc" and
outputs it in camel case "AaBbCc" or like this "Aa Bb Cc" if you specify an optional output separator like a space.  The input
value can be any mix of upper and lower case.</p>

<p>For example, if you name a database table CONTACT_METHOD in the config, then you can use this tag to convert it to camel case to create a
class name such as "ContactMethod".  You can also use this tag's output as the input for another tag such as {@link FirstLetterToLowerCase}
tag and use it to create variable names such as "t_contactMethod" for instances of the same class.</p>

<h3>Usage example</h3>

<pre><code><b>&lt;%camelCase value = &lt;%className%&gt; optionalInputSeparator = "-" optionalOutputSeparator = " " %&gt;</b></code></pre>

<h3>Attribute descriptions</h3>

<p>Note that any attribute with starting with the word [optional] is just that: optional.</p>

<p><code><b>optionalInputSeparator</b></code>: [default: "_" (under-score)]  It lets you create camel-cased output with a space or other
characters between the words so that, for example, it can be used for a UI label such as "Contact Method".  Also note that if the
separator value contains whitespace(s), you must enclose it in double quotes.</p>

<p><code><b>optionalOutputSeparator</b></code>:  This lets you create camel-cased output with a space or other characters between the words so that, for example,
it can be used for a UI label such as "Contact Method".  Also note that if the separator value contains whitespace(s), you must enclose it
in double quotes.</p>

*/
public class CamelCase extends Tag_Base {

	static public final String		TAG_NAME								= "camelCase";

	static private final String		ATTRIBUTE_VALUE							= "value";
	static private final String		OLD_ATTRIBUTE_OPTIONAL_SEPARATOR		= "optionalSeparator";			// I'm leaving this attribute name in here so that I don't have to bump the major version number with a completely breaking change with the new attribute names.
	static private final String		ATTRIBUTE_OPTIONAL_INPUT_SEPARATOR		= "optionalInputSeparator";
	static private final String		ATTRIBUTE_OPTIONAL_OUTPUT_SEPARATOR		= "optionalOutputSeparator";


	// Data members
	private	Tag_Base		m_value						= null;
	private	String			m_inputSeparator			= "_";		// This is just a default value that is about 100% of my usage.
	private	String			m_optionalOutputSeparator	= null;


	//*********************************
	public CamelCase() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("CamelCase.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_VALUE);
			if (t_nodeAttribute == null) {
				Logger.LogError("CamelCase.Init() did not find the [" + ATTRIBUTE_VALUE + "] attribute that is required for CamelCase tags at line number [" + m_lineNumber + "].");
				return false;
			}

			m_value = t_nodeAttribute.GetAttributeValue();
			if (m_value == null) {
				Logger.LogError("CamelCase.Init() did not get the [" + ATTRIBUTE_VALUE + "] value from attribute that is required for CamelCase tags at line number [" + m_lineNumber + "].");
				return false;
			}

			boolean t_foundOldAttributeName = false;
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_INPUT_SEPARATOR);
			if (t_nodeAttribute == null)  {
				t_nodeAttribute = p_tagParser.GetNamedAttribute(OLD_ATTRIBUTE_OPTIONAL_SEPARATOR);
				if (t_nodeAttribute != null) {
					Logger.LogWarning("CamelCase.Init() found the old optional input attribute name [" + OLD_ATTRIBUTE_OPTIONAL_SEPARATOR + "] in the CamelCase tag at line number [" + m_lineNumber + "].  This will not affect the tag for now, but that attribute name should be replaced with the new name [" + ATTRIBUTE_OPTIONAL_INPUT_SEPARATOR + "] where possible.");
					t_foundOldAttributeName = true;
				}
			}

			// Since the input separator attribute is optional, we may get here without it being found so we have to check for it no matter what.
			if (t_nodeAttribute != null) {
				m_inputSeparator = t_nodeAttribute.GetAttributeValueAsString();
				if ((m_inputSeparator == null) || m_inputSeparator.isBlank()) {
					Logger.LogError("CamelCase.Init() did not get the value from the optional attribute [" + (t_foundOldAttributeName ? OLD_ATTRIBUTE_OPTIONAL_SEPARATOR : ATTRIBUTE_OPTIONAL_INPUT_SEPARATOR) + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_OUTPUT_SEPARATOR);
			if (t_nodeAttribute != null) {
				m_optionalOutputSeparator = t_nodeAttribute.GetAttributeValueAsString();
				if ((m_optionalOutputSeparator == null) || m_optionalOutputSeparator.isBlank()) {
					Logger.LogError("CamelCase.Init() did not get the value from the optional attribute [" + ATTRIBUTE_OPTIONAL_OUTPUT_SEPARATOR + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("CamelCase.Init() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new CamelCase();
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
			if (m_value == null) {
				Logger.LogError("CamelCase.Evaluate() was not initialized at line number [" + m_lineNumber + "].");
				return false;
			}

			StringWriter	t_valueWriter	= new StringWriter();
			Cursor			t_valueCursor	= new Cursor(t_valueWriter);
			p_evaluationContext.PushNewCursor(t_valueCursor);

			if (!m_value.Evaluate(p_evaluationContext)) {
				Logger.LogError("CamelCase.Evaluate() failed to evaluate the value at line number [" + m_lineNumber + "].");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
				return false;
			}

			p_evaluationContext.PopCurrentCursor();	// Get rid of the temp cursor because we can't leave it on the context when we leave.

			String t_result = CreateCamelCaseValue(t_valueWriter.toString());
			if ((t_result == null) || t_result.isBlank()) {
				Logger.LogError("CamelCase.Evaluate() failed to evaluate the value at line number [" + m_lineNumber + "].");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
				return false;
			}


			p_evaluationContext.GetCursor().Write(t_result);
		}
		catch (Throwable t_error) {
			Logger.LogException("CamelCase.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name  :  " + m_name 	+ "\n");

		if (m_value != null) {
			t_dump.append("\n\n");
			t_dump.append(m_value.Dump(p_tabs + "\t"));

		}

		return t_dump.toString();
	}


	//*********************************
	private String CreateCamelCaseValue(String p_value) {
		try {
			StringBuilder t_newValue = new StringBuilder();

			if ((p_value == null) || p_value.isBlank()) {
				Logger.LogError("CamelCase.CreateCamelCaseValue(): A NULL or empty value was passed in.");
				return null;
			}

			// If this name doesn't contain any separators, then we'll just lower-case the rest of the name, add it on and return it.
			if (!p_value.contains(m_inputSeparator)) {
				t_newValue.append(p_value.substring(0, 1).toUpperCase() + p_value.substring(1).toLowerCase());
				return t_newValue.toString();
			}

			// Otherwise, we need to create a camel-case of the name and remove the input separator.
			String	t_remainder		= p_value;
			boolean	t_firstAdded	= false;
			int		t_index;

			// This loop will only go to the last instance of the input separator, so there will be a (possibly) dangling final part of the string that will have to be handled by the code after this loop below.
			while (t_remainder.contains(m_inputSeparator)) {
				if (t_firstAdded) {
					if (m_optionalOutputSeparator != null)
						t_newValue.append(m_optionalOutputSeparator);
				}
				else
					t_firstAdded = true;

				t_index = t_remainder.indexOf(m_inputSeparator);
				t_newValue.append(t_remainder.substring(0, 1).toUpperCase() + t_remainder.substring(1, t_index).toLowerCase());
				t_remainder = t_remainder.substring(++t_index);
			}

			// Only add the optional separator if there has already been at least one append on the new value.
			if (t_firstAdded && (m_optionalOutputSeparator != null)) {
					t_newValue.append(m_optionalOutputSeparator);
			}

			t_newValue.append(t_remainder.substring(0, 1).toUpperCase());
			if (t_remainder.length() > 1)
				t_newValue.append(t_remainder.substring(1).toLowerCase());

			return t_newValue.toString();
		}
		catch (Throwable t_error) {
			Logger.LogException("CamelCase.CreateCamelCaseValue() failed with error: ", t_error);
			return null;
		}
	}
}

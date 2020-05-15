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
Takes any config value of the form "aa_bb_cc" or "aa-bb-cc" and outputs it in camel case "AaBbCc".
The input value can be any mix of upper and lower case.

<p>Example use of this tag:</p>

<pre><code>&lt;%camelCase value = &lt;%className%&gt; optionalSeparator = " " %&gt;</code></pre>

<p>Note that the attribute [optionalSeparator] is just that: optional.  It lets you create camel-cased output with a space or other characters between the words, for example, so that it can be used for a UI label.</p>

*/
public class CamelCase extends TemplateBlock_Base {

	static public final String		BLOCK_NAME						= "camelCase";

	static public final String		ATTRIBUTE_VALUE					= "value";
	static public final String		ATTRIBUTE_OPTIONAL_SEPARATOR	= "optionalSeparator";


	// Data members
	private	TemplateBlock_Base		m_value					= null;
	private	String					m_optionalSeparator		= null;


	//*********************************
	public CamelCase() {
		super(BLOCK_NAME);
		m_isSafeForTextBlock = true;
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

			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_SEPARATOR);
			if (t_nodeAttribute != null) {
				m_optionalSeparator = t_nodeAttribute.GetAttributeValueAsString();
				if (m_optionalSeparator == null) {
					Logger.LogError("CamelCase.Init() did not get the value from the optional attribute [" + ATTRIBUTE_OPTIONAL_SEPARATOR + "] at line number [" + m_lineNumber + "].");
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
	public TemplateBlock_Base GetInstance() {
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

			p_evaluationContext.GetCursor().Write(CreateCamelCaseName(t_valueWriter.toString()));
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

		t_dump.append(p_tabs + "Block type name  :  " + m_name 	+ "\n");

		if (m_value != null) {
			t_dump.append("\n\n");
			t_dump.append(m_value.Dump(p_tabs + "\t"));

		}

		return t_dump.toString();
	}


	//*********************************
	private String CreateCamelCaseName(String p_className) {
		StringBuilder t_newValue = new StringBuilder();

		if (p_className == null) {
			Logger.LogError("DDLParser.CreateCamelCaseName(): A NULL was passed in.");
			return null;
		}

		String	t_separator		= "_";
		char	t_separatorChar	= '_';
		if (p_className.contains("-")) {
			t_separator		= "-";
			t_separatorChar	= '-';
		}

		if (!p_className.contains(t_separator)) {	// If this name doesn't contain any separators, then we'll just lower-case the rest of the name, add it on and return it.
			t_newValue.append(p_className.substring(0, 1).toUpperCase() + p_className.substring(1).toLowerCase());
			return t_newValue.toString();
		}

		// Otherwise, we need to create a camel-case of the name and remove the underscores.
		String	t_remainder		= p_className;
		boolean	t_firstAdded	= false;
		int		t_index;
		while (t_remainder.contains(t_separator)) {
			if (t_firstAdded) {
				if (m_optionalSeparator != null)
					t_newValue.append(m_optionalSeparator);
			}
			else
				t_firstAdded = true;

			t_index = t_remainder.indexOf(t_separatorChar);
			t_newValue.append(t_remainder.substring(0, 1).toUpperCase() + t_remainder.substring(1, t_index).toLowerCase());
			t_remainder = t_remainder.substring(++t_index);
		}

		// Only add the optional separator if there has already been at least one append on the new value.
		if (t_firstAdded) {
			if (m_optionalSeparator != null)
				t_newValue.append(m_optionalSeparator);
		}

		t_newValue.append(t_remainder.substring(0, 1).toUpperCase());
		if (t_remainder.length() > 1)
			t_newValue.append(t_remainder.substring(1).toLowerCase());

		return t_newValue.toString();
	}
}
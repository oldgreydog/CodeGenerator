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
	Takes any config value and outputs it with the first character re-cast in lowercase.  This was
	created so that I could define a camel case class name config value with the first letter capitalized
	and re-use it for a variable name with a lowercase first letter.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%firstLetterToLowerCase value = &lt;%className%&gt;%&gt;</code></pre>

	<p>So a class name like <code>FooBar</code> could also be used to generate a local variable called
	<code>t_fooBar</code>.</p>
 */
public class FirstLetterToLowerCase extends TemplateBlock_Base {

	static public final String		BLOCK_NAME			= "firstLetterToLowerCase";

	static public final String		ATTRIBUTE_VALUE		= "value";


	// Data members
	protected	TemplateBlock_Base		m_value		= null;


	//*********************************
	public FirstLetterToLowerCase() {
		super(BLOCK_NAME);
		m_isSafeForTextBlock = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("FirstLetterToLowerCase.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_VALUE);
			if (t_nodeAttribute == null) {
				Logger.LogError("FirstLetterToLowerCase.Init() did not find the [" + ATTRIBUTE_VALUE + "] attribute that is required for FirstLetterToLowerCase tags.");
				return false;
			}

			m_value = t_nodeAttribute.GetAttributeValue();
			if (m_value == null) {
				Logger.LogError("FirstLetterToLowerCase.Init() did not get the [" + ATTRIBUTE_VALUE + "] value from attribute that is required for FirstLetterToLowerCase tags.");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstLetterToLowerCase.Init() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new FirstLetterToLowerCase();
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
			// If there are no child blocks, then this is a "leaf" node text object and we have to output its string.
			if (m_value == null) {
				Logger.LogError("FirstLetterToLowerCase.Evaluate() was not initialized.");
				return false;
			}

			StringWriter	t_valueWriter	= new StringWriter();
			Cursor			t_valueCursor	= new Cursor(t_valueWriter);

			p_evaluationContext.PushNewCursor(t_valueCursor);

			if (!m_value.Evaluate(p_evaluationContext)) {
				Logger.LogError("FirstLetterToLowerCase.Evaluate() failed to evaluate the value.");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
				return false;
			}

			p_evaluationContext.PopCurrentCursor();

			String			t_sourceValue	= t_valueWriter.toString();
			StringBuilder	t_newValue		= new StringBuilder();

			t_newValue.append(Character.toLowerCase(t_sourceValue.charAt(0)));
			t_newValue.append(t_sourceValue.subSequence(1, t_sourceValue.length()));

			p_evaluationContext.GetCursor().Write(t_newValue.toString());
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstLetterToLowerCase.Evaluate() failed with error: ", t_error);
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
}
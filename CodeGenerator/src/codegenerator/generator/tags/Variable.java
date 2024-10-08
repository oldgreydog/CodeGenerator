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



import coreutil.config.*;
import coreutil.logging.*;

import java.util.*;

import codegenerator.generator.utils.*;



/**
<p>Allows you to create reusable template blocks so that duplicate template sections can be cleaned
up and simplified.</p>

<h3>Usage example</h3>

<pre><code><b>&lt;%variable name = "primeNames" evalmode = "set" %&gt;
...
&lt;%endVariable%&gt;</b></code></pre>

<p>When used with <code><b>evalmode</b></code> set to "set", it will parse the contents to the <code><b>&lt;%endVariable%&gt;</b></code>
tag and save them for the variable name.</p>

<pre><code><b>&lt;%variable name = "primeNames" evalmode = "evaluate" optionalContextName = "outer1" %&gt;</b></code></pre>

<p>When used with <code><b>evalmode</b></code> set to <code><b>evaluate</b></code>, it will evaluate the template block set for that variable name at that location.
Obviously, all of the config value references inside the  must also be valid for that location.</p>

<h3>Attribute descriptions</h3>

<p><code><b>name</b></code>:  the name that identifies a particular <code><b>variable</b></code> instance.</p>

<p><code><b>evalmode</b></code>: [values: <code><b>set</b></code>|<code><b>evaluate</b></code>, no default: a value must be set]  </p>

<p><code><b>optionalContextName</b></code>:  this is an optional attribute.  Now that outer contexts exist, I had to add this so that
variables could be made to work even inside inner contexts when the variable's definition uses values from the outer context.

*/
public class Variable extends Tag_Base {

	static public final String		TAG_NAME							= "variable";
	static public final String		TAG_END_NAME						= "endVariable";

	static private final String		ATTRIBUTE_NAME						= "name";
	static private final String		ATTRIBUTE_EVAL_MODE					= "evalMode";
	static private final String		ATTRIBUTE_OPTIONAL_CONTEXT_NAME		= "optionalContextName";

	static private final String		EVAL_MODE_LABEL_SET					= "set";
	static private final String		EVAL_MODE_LABEL_EVALUATE			= "evaluate";

	static private final int		EVAL_MODE_VALUE_UNDEFINED			= -1;
	static private final int		EVAL_MODE_VALUE_SET					= 1;
	static private final int		EVAL_MODE_VALUE_EVALUATE			= 2;


	// Static members
	static protected final TreeMap<String, Tag_Base>		m_variableMap	= new TreeMap<>();


	// Data members
	private String		m_variableName	= null;
	private int			m_evalMode		= EVAL_MODE_VALUE_UNDEFINED;	// This is the name of the config node that will be the temporary "root" node for each iteration of the loop.  For example, if this is == "class", then when we enter Evaluate(), we will run through the loop once for each "class" child node we find on the passed-in p_currentNode.
	private	String		m_contextName	= null;					// The optional outer context in which to evaluate this variable.


	//*********************************
	public Variable() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public Variable GetInstance() {
		return new Variable();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("Variable.Init() failed in the parent Init().");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_NAME);
		if (t_nodeAttribute == null) {
			Logger.LogError("Variable.Init() did not find the [" + ATTRIBUTE_NAME + "] attribute that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_variableName = t_nodeAttribute.GetAttributeValueAsString();
		if ((m_variableName == null) || m_variableName.isBlank()) {
			Logger.LogError("Variable.Init() did not get the value for the [" + ATTRIBUTE_NAME + "] attribute that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
			return false;
		}



		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_EVAL_MODE);
		if (t_nodeAttribute == null) {
			Logger.LogError("Variable.Init() did not find the [" + ATTRIBUTE_EVAL_MODE + "] attribute that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
			return false;
		}

		String t_evalMode = t_nodeAttribute.GetAttributeValueAsString();
		if ((t_evalMode == null) || t_evalMode.isBlank()) {
			Logger.LogError("Variable.Init() did not get the value from attribute [" + ATTRIBUTE_EVAL_MODE + "] that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
			return false;
		}

		if (t_evalMode.equalsIgnoreCase(EVAL_MODE_LABEL_SET))
			m_evalMode = EVAL_MODE_VALUE_SET;
		else if (t_evalMode.equalsIgnoreCase(EVAL_MODE_LABEL_EVALUATE))
			m_evalMode = EVAL_MODE_VALUE_EVALUATE;
		else {
			Logger.LogError("Variable.Init() received and invalid evalmode value [" + t_evalMode + "].");
			return false;
		}


		// The "contextname" attribute is optional, so it's fine if it doesn't exist.
		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_CONTEXT_NAME);
		if (t_nodeAttribute != null) {
			m_contextName = t_nodeAttribute.GetAttributeValueAsString();
			if ((m_contextName == null) || m_contextName.isBlank()) {
				Logger.LogError("Variable.Init() did not get the value from attribute [" + ATTRIBUTE_OPTIONAL_CONTEXT_NAME + "] that is optional for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
				return false;
			}
		}


		return true;
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// We only parse the variable tag in "set" mode.  Otherwise, there is nothing after the opening tag.
			if (m_evalMode != EVAL_MODE_VALUE_SET)
				return true;

			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("Variable.Parse() general block parser failed in the block starting at [" + m_lineNumber + "].");
				return false;
			}

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase(TAG_END_NAME)) {
				Logger.LogError("Variable.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
				return false;
			}

			m_variableMap.put(m_variableName, t_generalBlock);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("Variable.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			if (m_evalMode == EVAL_MODE_VALUE_SET)		// Don't do anything for instances that are "set"s.  Those are never "evaluated".
				return true;
			else if (m_evalMode == EVAL_MODE_VALUE_EVALUATE) {
				Tag_Base t_evalBlock = m_variableMap.get(m_variableName);
				if (t_evalBlock == null) {
					Logger.LogError("Variable.Evaluate() did not find a evaluation tag for variable [" + m_variableName + "] at line [" + m_lineNumber + "].");
					return false;
				}

				// The addition of outer contexts means that if you use a variable inside an inner context, you may need to point it to the outer context to get the correct values in the evaluation of the variable.
				ConfigNode t_currentNode = p_evaluationContext.GetCurrentNode();
				if (m_contextName != null) {
					t_currentNode = p_evaluationContext.GetOuterContextManager().GetOuterContext(m_contextName);
					if (t_currentNode == null) {
						Logger.LogError("Variable.Evaluate() failed to find the outer context [" + m_contextName + "] for the evaluation mode at line [" + m_lineNumber + "].");
						return false;
					}
				}

				p_evaluationContext.PushNewCurrentNode(t_currentNode);	// This is unnecessarily redundant if we aren't changing the context above, but it simpler and cleaner, particularly if we error out in the if() below.

				if (!t_evalBlock.Evaluate(p_evaluationContext)) {
					p_evaluationContext.PopCurrentNode();
					return false;
				}

				p_evaluationContext.PopCurrentNode();
			}
			else {
				Logger.LogError("Variable.Evaluate() found an invalid value [" + m_evalMode + "] for the evaluation mode at line [" + m_lineNumber + "].");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("Variable.Evaluate() failed with error at line [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name        :  " + m_name			+ "\n");
		t_dump.append(p_tabs + "Variable name   :  " + m_variableName	+ "\n");
		t_dump.append(p_tabs + "Evaluation mode :  " + m_evalMode		+ "\n");

		t_dump.append("\n\n" + m_variableMap.get(m_variableName).Dump(p_tabs + "\t"));

		return t_dump.toString();
	}
}

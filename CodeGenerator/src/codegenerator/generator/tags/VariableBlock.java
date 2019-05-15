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

<p>When used with a variable name and the evalmode set to "set", it will parse the contents to the
<code>&lt;%endvariable%&gt;</code> tag and save them for the variable name.</p>

<pre><code>&lt;%variable name = "primeNames" evalmode = "set" %&gt;
...
&lt;%endvariable%&gt;</code></pre>

<p>When used with a variable name and the evalmode set to "evaluate", it will evaluate the template
block set for that variable name at that location.  Obviously, all of the config value references must
also be valid for that location.</p>

<pre><code>&lt;%variable name = "primeNames" evalmode = "evaluate" %&gt;</code></pre>

*/
public class VariableBlock extends TemplateBlock_Base {

	static public final String		BLOCK_NAME				= "variable";

	static public final String		ATTRIBUTE_NAME			= "name";
	static public final String		ATTRIBUTE_EVAL_MODE		= "evalmode";

	static public final int			EVAL_MODE_UNDEFINED		= -1;
	static public final int			EVAL_MODE_SET			= 1;
	static public final int			EVAL_MODE_EVALUATE		= 2;


	// Static members
	static protected final TreeMap<String, TemplateBlock_Base>		m_variableMap	= new TreeMap<>();


	// Data members
	protected String	m_variableName	= null;
	protected int		m_evalMode		= EVAL_MODE_UNDEFINED;	// This is the name of the config node that will be the temporary "root" node for each iteration of the loop.  For example, if this is == "class", then when we enter Evaluate(), we will run through the loop once for each "class" child node we find on the passed-in p_currentNode.


	//*********************************
	public VariableBlock() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public VariableBlock GetInstance() {
		return new VariableBlock();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_EVAL_MODE);
		if (t_nodeAttribute == null) {
			Logger.LogError("VariableBlock.Init() did not find the [" + ATTRIBUTE_EVAL_MODE + "] attribute that is required for variable tags.");
			return false;
		}

		String t_evalMode = t_nodeAttribute.GetValue().GetText();
		if (t_evalMode == null) {
			Logger.LogError("VariableBlock.Init() did not get the value from attribute that is required for variable tags.");
			return false;
		}

		if (t_evalMode.equalsIgnoreCase("set"))
			m_evalMode = EVAL_MODE_SET;
		else if (t_evalMode.equalsIgnoreCase("evaluate"))
			m_evalMode = EVAL_MODE_EVALUATE;
		else {
			Logger.LogError("VariableBlock.Init() received and invalid evalmode value [" + t_evalMode + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_NAME);
		if (t_nodeAttribute == null) {
			Logger.LogError("VariableBlock.Init() did not find the [" + ATTRIBUTE_NAME + "] attribute that is required for variable tags.");
			return false;
		}

		m_variableName = t_nodeAttribute.GetValue().GetText();
		if (m_variableName == null) {
			Logger.LogError("VariableBlock.Init() did not get the value from attribute that is required for variable tags.");
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// We only parse the variable block in "set" mode.  Otherwise, there is nothing after the opening tag.
			if (m_evalMode != EVAL_MODE_SET)
				return true;

			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("VariableBlock.Parse() general block parser failed.");
				return false;
			}

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase("endvariable")) {
				Logger.LogError("VariableBlock.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "].  The closing tag [endfor] was expected.");
				return false;
			}

			m_variableMap.put(m_variableName, t_generalBlock);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("VariableBlock.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			if (m_evalMode == EVAL_MODE_SET)		// Don't do anything for instances that are "set"s.  Those are never "evaluated".
				return true;
			else if (m_evalMode == EVAL_MODE_EVALUATE) {
				TemplateBlock_Base t_evalBlock = m_variableMap.get(m_variableName);
				if (t_evalBlock == null) {
					Logger.LogError("VariableBlock.Evaluate() did not find a evaluation block for variable [" + m_variableName + "].");
					return false;
				}

				if (!t_evalBlock.Evaluate(p_currentNode, p_rootNode, p_writer, p_iterationCounter))
					return false;
			}
			else {
				Logger.LogError("VariableBlock.Evaluate() found an invalid value [" + m_evalMode + "] for the evaluation mode.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("VariableBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name :  " + m_name			+ "\n");
		t_dump.append(p_tabs + "Variable name   :  " + m_variableName	+ "\n");
		t_dump.append(p_tabs + "Evaluation mode :  " + m_evalMode		+ "\n");

		t_dump.append("\n\n" + m_variableMap.get(m_variableName).Dump(p_tabs + "\t"));

		return t_dump.toString();
	}
}

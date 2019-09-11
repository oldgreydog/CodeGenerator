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
<p>Please see the documentation for {@link OuterContext} for more information about outer contexts and their usage.</p>

<p>This tag lets you pull in a value from the outer context to be used in the inner context.</p>

<p>Example use of this tag:</p>

<p><pre><code>&lt;%outercontexteval contextname = outer1 targetvalue = tableName %&gt;</code></pre></p>

<p></p>

*/
public class OuterContextEval extends TemplateBlock_Base {

	static public final String		BLOCK_NAME		= "outerContextEval";


	// Data members
	private	String		m_contextName	= null;
	private	String		m_valuePath		= null;


	//*********************************
	public OuterContextEval() {
		super(BLOCK_NAME);
		m_isSafeForTextBlock = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			m_lineNumber = p_tagParser.GetLineNumber();

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute("contextname");
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContextEval.Init() did not find the [contextname] attribute that is required for OuterContextEval tags at line number [" + m_lineNumber + "].");
				return false;
			}

			m_contextName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_contextName == null) {
				Logger.LogError("OuterContextEval.Init() did not get the value from the optional attribute [optionalSeparator] at line number [" + m_lineNumber + "].");
				return false;
			}


			t_nodeAttribute = p_tagParser.GetNamedAttribute("targetvalue");
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContextEval.Init() did not find the [targetvalue] attribute that is required for OuterContextEval tags at line number [" + m_lineNumber + "].");
				return false;
			}

			m_valuePath = t_nodeAttribute.GetAttributeValueAsString();
			if (m_valuePath == null) {
				Logger.LogError("OuterContextEval.Init() did not get the [targetvalue] attribute value from that is required for OuterContextEval at line number [" + m_lineNumber + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("OuterContextEval.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new OuterContextEval();
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
							LoopCounter		p_iterationCounter)	// There wasn't any good way to tell a OuterContextEval which iteration it was in that would be safe for arbitrary nesting, so I added this iteration counter to handle the problem.
	{
		try {
			if (m_valuePath == null) {
				Logger.LogError("OuterContextEval.Evaluate() was not initialized.");
				return false;
			}

			ConfigNode t_contextNode = OuterContextManager.GetOuterContext(m_contextName);
			if (t_contextNode == null) {
				Logger.LogError("OuterContextEval.Evaluate() failed to retreive an outer context with the name [" + m_contextName + "].");
				return false;
			}

			// We'll us a ConfigVariable to do the dirty work of getting the value out of the config.
			ConfigVariable t_targetValue = new ConfigVariable();
			t_targetValue.Init(m_valuePath, m_lineNumber);

			if (!t_targetValue.Evaluate(t_contextNode, p_rootNode, p_writer, p_iterationCounter)) {
				Logger.LogError("OuterContextEval.Evaluate() failed to evaluate the value.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("OuterContextEval.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name   :  " + m_name 		+ "\n");
		t_dump.append(p_tabs + "	context name  :  " + m_contextName 	+ "\n");

		if (m_valuePath != null) {
			t_dump.append(p_tabs + "	value name :\n" + m_valuePath + "\t");

		}

		return t_dump.toString();
	}
}

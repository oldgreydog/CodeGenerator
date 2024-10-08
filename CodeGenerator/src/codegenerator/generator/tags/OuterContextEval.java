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
import coreutil.config.*;
import coreutil.logging.*;



/**
<p>Provides access to values under the specified <code><b>outerContext</b></code> for use in nested tags whose context has
been changed in such a way that they no longer have direct parent access to that <code><b>outerContext</b></code>.</p>

<p>Please see the documentation for {@link OuterContext} for more information about outer contexts and their usage.</p>

<p>This tag lets you pull in a value from the outer context to be used in the inner context, usually in <code><b>if</b></code> tags or
to output directly to the generated file.</p>

<h3>Usage example</h3>

<p><pre><code><b>&lt;%outerContextEval contextname = outer1 targetvalue = tableName %&gt;</b></code></pre></p>

<h3>Attribute descriptions</h3>

<p><code><b>contextname</b></code>:  the name of the <code><b>outerContext</b></code> tag that this tag will find to get its context node.</p>

<p><code><b>targetvalue</b></code>:  once the target <code><b>outerContext</b></code> tag context is obtained, then this is the value under that
context that is to be retrieved and output as the "result" of this tag.</p>
*/
public class OuterContextEval extends Tag_Base {

	static public final String		TAG_NAME					= "outerContextEval";

	static private final String		ATTRIBUTE_CONTEXT_NAME		= "contextName";
	static private final String		ATTRIBUTE_TARGET_VALUE		= "targetValue";


	// Data members
	private	String		m_contextName	= null;
	private	String		m_valuePath		= null;


	//*********************************
	public OuterContextEval() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("OuterContextEval.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_CONTEXT_NAME);
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContextEval.Init() did not find the [" + ATTRIBUTE_CONTEXT_NAME + "] attribute that is required for OuterContextEval tags at line number [" + m_lineNumber + "].");
				return false;
			}

			m_contextName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_contextName == null) {
				Logger.LogError("OuterContextEval.Init() did not get the value from the attribute [" + ATTRIBUTE_CONTEXT_NAME + "] at line number [" + m_lineNumber + "].");
				return false;
			}


			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TARGET_VALUE);
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContextEval.Init() did not find the [" + ATTRIBUTE_TARGET_VALUE + "] attribute that is required for OuterContextEval tags at line number [" + m_lineNumber + "].");
				return false;
			}

			m_valuePath = t_nodeAttribute.GetAttributeValueAsString();
			if (m_valuePath == null) {
				Logger.LogError("OuterContextEval.Init() did not get the [" + ATTRIBUTE_TARGET_VALUE + "] attribute value from that is required for OuterContextEval at line number [" + m_lineNumber + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("OuterContextEval.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new OuterContextEval();
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
			if (m_valuePath == null) {
				Logger.LogError("OuterContextEval.Evaluate() at line number [" + m_lineNumber + "] was not initialized.");
				return false;
			}

			ConfigNode t_contextNode = p_evaluationContext.GetOuterContextManager().GetOuterContext(m_contextName);
			if (t_contextNode == null) {
				Logger.LogError("OuterContextEval.Evaluate() failed to retreive an outer context with the name [" + m_contextName + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			p_evaluationContext.PushNewCurrentNode(t_contextNode);

			// We'll us a ConfigValue to do the dirty work of getting the value out of the config.
			ConfigValue t_targetValue = new ConfigValue();
			t_targetValue.Init(m_valuePath, m_lineNumber);

			if (!t_targetValue.Evaluate(p_evaluationContext)) {
				Logger.LogError("OuterContextEval.Evaluate() failed to evaluate the value at line number [" + m_lineNumber + "].");
				p_evaluationContext.PopCurrentNode();
				return false;
			}

			p_evaluationContext.PopCurrentNode();
		}
		catch (Throwable t_error) {
			Logger.LogException("OuterContextEval.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name          :  " + m_name 		+ "\n");
		t_dump.append(p_tabs + "	context name  :  " + m_contextName 	+ "\n");

		if (m_valuePath != null) {
			t_dump.append(p_tabs + "	value name :\n" + m_valuePath + "\t");

		}

		return t_dump.toString();
	}
}

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



import coreutil.logging.*;
import codegenerator.generator.utils.*;



/**
	This lets you output the counter value.

	<pre><code>&lt;%counter  optionalCounterName = "loop1" %&gt;</code></pre>

	<p>The optionalCounterName attribute lets you specify using a named loop counter from a foreach block other than the
	one directly containing this counter block.</p>
*/
public class Counter extends TemplateBlock_Base {

	static public final String		BLOCK_NAME							= "counter";

	static public final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";

	// Data members
	private	String	m_optionalCounterName	= null;	// Providing a name for the loop counter lets you specify using a named loop counter from a foreach block other than the one directly containing this first block.


	//*********************************
	public Counter() {
		super("counter");
		m_isSafeForTextBlock = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("Counter.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		// The attribute "optionalCounterName" is, obviously, optional, so we need to handle it that way.
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_COUNTER_NAME);
		if (t_nodeAttribute != null) {
			m_optionalCounterName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_optionalCounterName == null) {
				Logger.LogError("Counter.Init() did not get the value from the [" + ATTRIBUTE_OPTIONAL_COUNTER_NAME + "] attribute.");
				return false;
			}
		}

		return true;
	}


	//*********************************
	@Override
	public Counter GetInstance() {
		return new Counter();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		// Nothing to do here.
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			LoopCounter t_iterationCounter = p_evaluationContext.GetLoopCounter();
			if (m_optionalCounterName != null)
				t_iterationCounter = t_iterationCounter.GetNamedCounter(m_optionalCounterName);

			if (t_iterationCounter == null) {
				Logger.LogError("Counter.Evaluate() failed to find a loop counter with name [" + m_optionalCounterName + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			p_evaluationContext.GetCursor().Write(Integer.toString(t_iterationCounter.GetCounter()));
		}
		catch (Throwable t_error) {
			Logger.LogException("Counter.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name :  counter\n");

		return t_dump.toString();
	}
}
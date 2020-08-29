/*
	Copyright 2020 Wes Kaylor

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
	This tag gives you more control over how counter values change.  It is required if you are using free-standing <code>counterVariable</code>
	block because that block doesn't auto-increment the way that a <code>forEach</code> counter does.

	<p>Look at the documentation for {@link CounterVariable} for an example of usage.  You can use this with <code>counterVariable</code>
	counters and named <code>forEach</code> counters.</p>
*/
public class CounterIncrement extends TemplateBlock_Base {

	static public final String		BLOCK_NAME							= "++counter";

	static public final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";

	// Data members
	private	String	m_optionalCounterName	= null;	// Providing a name for the loop counter lets you specify using a named loop counter from a forEach block other than the one directly containing this first block.


	//*********************************
	public CounterIncrement() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("CounterIncrement.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		// The attribute "optionalCounterName" is, obviously, optional, so we need to handle it that way.
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_COUNTER_NAME);
		if (t_nodeAttribute != null) {
			m_optionalCounterName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_optionalCounterName == null) {
				Logger.LogError("CounterIncrement.Init() did not get the value from the [" + ATTRIBUTE_OPTIONAL_COUNTER_NAME + "] attribute.");
				return false;
			}
		}

		return true;
	}


	//*********************************
	@Override
	public CounterIncrement GetInstance() {
		return new CounterIncrement();
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
			if (m_optionalCounterName != null) {
				t_iterationCounter = t_iterationCounter.GetNamedCounter(m_optionalCounterName);

				if (t_iterationCounter == null) {
					// If we don't find a loop counter by that name, then we need to check to see if there is a counter variable by that name.
					t_iterationCounter = p_evaluationContext.GetCounterVariable(m_optionalCounterName);
					if (t_iterationCounter == null) {
						Logger.LogError("CounterIncrement.Evaluate() failed to find a loop counter with name [" + m_optionalCounterName + "] at line number [" + m_lineNumber + "].");
						return false;
					}
				}
			}

			if (t_iterationCounter == null) {
				Logger.LogError("CounterIncrement.Evaluate() failed to find a default loop counter and no optional counter name was specified at line number [" + m_lineNumber + "].");
				return false;
			}

			t_iterationCounter.IncrementCounter();
		}
		catch (Throwable t_error) {
			Logger.LogException("CounterIncrement.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name :  ++counter\n");

		return t_dump.toString();
	}
}

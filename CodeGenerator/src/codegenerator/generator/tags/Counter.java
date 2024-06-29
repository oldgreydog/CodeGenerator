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
	<p>This lets you use a counter value in the text output.  By default, the value comes from the {@link ForEach ForEach} tag that
	immediately encloses this tag.  Or if you use the optional <code>optionalCounterName</code> attribute, you can name a different enclosing
	<code>foreach</code> tag or a <code>counterVariable</code> tag to get the value from.</p>

	<h3>Examples</h3>

	<p>We'll use the following template code for the examples:</p>


	<p><pre><code>&lt;%foreach node = table  optionalCounterName = tableCounter %&gt;
	&lt;%foreach node = column %&gt;

		&lt;%text%&gt;	Table &lt;%counter  optionalCounterName = tableCounter %&gt;		Column &lt;%counter%&gt;
&lt;%endtext%&gt;

	&lt;%endfor%&gt;
&lt;%endfor%&gt;</code></pre></p>

	<p>This would give you an output that looks like this for however many tables/columns you have in the config.</p>

	<pre><code>
	Table 1		Column 1
	Table 1		Column 2
	Table 1		Column 3
	...
	Table 2		Column 1
	Table 2		Column 2
	Table 2		Column 3
	...</code></pre>

	<p><B>However</B>, if you had ten tables and one of them was named USER, then you would only get <B>nine</B> table counts output!</p>
*/
public class Counter extends Tag_Base {

	static public final String		TAG_NAME							= "counter";

	static private final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";

	// Data members
	private	String	m_optionalCounterName	= null;	// Providing a name for the loop counter lets you specify using a named loop counter from a forEach tag other than the one directly containing this first tag.


	//*********************************
	public Counter() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
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
			LoopCounter t_iterationCounter = p_evaluationContext.GetLoopCounter();	// Get the default counter which is the most immediately enclosing ForEach tag.
			if (m_optionalCounterName != null) {
				// If the optional counter name is set, then get the referenced counter instead of the default.
				t_iterationCounter = p_evaluationContext.GetNamedCounter(m_optionalCounterName);
				if (t_iterationCounter == null) {
					Logger.LogError("Counter.Evaluate() failed to find a loop counter with name [" + m_optionalCounterName + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			if (t_iterationCounter == null) {
				Logger.LogError("Counter.Evaluate() failed to find a default loop counter and no optional counter name was specified at line number [" + m_lineNumber + "].");
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

		t_dump.append(p_tabs + "Tag name        :  " + m_name 	+ "\n");

		return t_dump.toString();
	}
}

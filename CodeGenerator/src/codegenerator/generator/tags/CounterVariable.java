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
	If you need to have a counter that guarantees that you only generate a piece of code only once inside
	a complicated set of nested contexts and/or <code>forEach</code> loops, then you can set up a CounterVariable
	tag that wraps those contexts/loops and use the new <code>++counter</code> ({@link CounterIncrement}) tag inside the
	if/else tag where the desired code is generated with the same name you put on the counterVariable tag.
	This will ensure that regardless of how the <code>forEach</code> loops traverse the config tree, that code will only get
	generated once.

	<p>Here's a long but (I think) illuminating real usage example from one of my templates:</p>
	<br>
	<pre><code>&lt;%counterVariable counterName = tableCounter %&gt;

	&lt;&lt;&lt; This section finds all foreign key references to this table and add the child parent nodes for each of them. &gt;&gt;&gt;
	&lt;%outerContext contextname = "parentTable" %&gt;

		&lt;%foreach node = "^table" %&gt;

			&lt;&lt;&lt; First we add this child node if this outer table has a foreign key that points back to the outer table.
			&lt;%foreach node = column %&gt;

				&lt;%foreach node = foreignKey %&gt;

					&lt;%if &lt;%parentTableName%&gt; = &lt;%outerContextEval contextname = "parentTable" targetvalue = sqlName %&gt; %&gt;

						&lt;%first optionalCounterName = tableCounter%&gt;

							&lt;&lt;&lt; We can use the "first" to add the opening authentication code only if there is at least one child found. &gt;&gt;&gt;
							&lt;%text%&gt;
	var t_authenticationAPI = new AuthenticationAPI();
	var t_permissionResults = t_authenticationAPI.GetPermissionValues("&lt;%endtext%&gt;

						&lt;%else%&gt;

							&lt;%text%&gt;,&lt;%endtext%&gt;

						&lt;%endfirst%&gt;

						&lt;%++counter optionalCounterName = tableCounter %&gt;

						&lt;%text%&gt;database.&lt;%^^sqlName%&gt;.view&lt;%endtext%&gt;

					&lt;%endif%&gt;

				&lt;%endfor%&gt;

			&lt;%endfor%&gt;

		&lt;%endfor%&gt;

	&lt;%endcontext%&gt;


	&lt;&lt;&lt; We'll use the tableCounter to only add this block of code if at least one child table was found. &gt;&gt;&gt;
	&lt;%first optionalCounterName = tableCounter%&gt;

	&lt;%else%&gt;

		&lt;%text%&gt;");
	if (t_permissionResults === null) {
		alert("Failed to get the permissions required to create the &lt;%className%&gt; display.");
		return null;
	}
	else {
		var t_permissions = t_permissionResults.permissions;

&lt;%endtext%&gt;

	&lt;%endfirst%&gt;


&lt;%endCounter%&gt;
</code></pre>
	<br>
	<p>If you look at the example closely, you'll see that where I used <code>++counter</code>, I only get one pass through that
	<code>first</code> tag.  But I also use the same counter at the bottom of the example in the <code>first</code> tag to only
	generate a matching block of code in the <code>else</code> if the first <code>first</code> tag was executed at least once.</p>
*/
public class CounterVariable extends Tag_Base {

static public final String		TAG_NAME										= "counterVariable";
static public final String		TAG_END_NAME									= "endCounter";

	static private final String		ATTRIBUTE_COUNTER_NAME		= "counterName";


// Data members
private String			m_counterName				= null;
private GeneralBlock	m_contentBlock				= null;


//*********************************
public CounterVariable() {
	super(TAG_NAME);
}


//*********************************
@Override
public boolean Init(TagParser p_tagParser) {
	try {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("CounterVariable.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_COUNTER_NAME);
		if (t_nodeAttribute == null) {
			Logger.LogError("CounterVariable.Init() failed to find the required attribute [" + ATTRIBUTE_COUNTER_NAME + "] at line number [" + m_lineNumber + "].");
			return false;
		}

		m_counterName = t_nodeAttribute.GetAttributeValueAsString();

		return true;
	}
	catch (Throwable t_error) {
		Logger.LogException("CounterVariable.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
		return false;
	}
}


//*********************************
@Override
public CounterVariable GetInstance() {
	return new CounterVariable();
}


//*********************************
@Override
public boolean Parse(TemplateTokenizer p_tokenizer) {
	try {
		// The GeneralBlock handles parsing of this tag's contents.
		GeneralBlock t_generalBlock	= new GeneralBlock();
		if (!t_generalBlock.Parse(p_tokenizer)) {
			Logger.LogError("CounterVariable.Parse() general block parser failed in the block starting at line number [" + m_lineNumber + "].");
			return false;
		}

		m_contentBlock = t_generalBlock;

		String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
		if (!t_endingTagName.equalsIgnoreCase("endCounter")) {
			Logger.LogError("CounterVariable.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
			return false;
		}
	}
	catch (Throwable t_error) {
		Logger.LogException("CounterVariable.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "]: ", t_error);
		return false;
	}

	return true;
}


//*********************************
@Override
public boolean Evaluate(EvaluationContext p_evaluationContext)
{
	try {
		LoopCounter	t_counter = new LoopCounter();

		p_evaluationContext.AddCounterVariable(m_counterName, t_counter);

		if (!m_contentBlock.Evaluate(p_evaluationContext)) {
			Logger.LogError("CounterVariable.Evaluate() failed to evaluate its content block.");
			p_evaluationContext.PopCurrentNode();
			return false;
		}

		p_evaluationContext.RemoveCounterVariable(m_counterName);
	}
	catch (Throwable t_error) {
		Logger.LogException("CounterVariable.Evaluate() failed with error: ", t_error);
		return false;
	}

	return true;
}
}

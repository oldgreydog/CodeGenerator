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
<p>Once I started using outer contexts, a need arose for some way to control first/else blocks across more than one forEach
where there was no outer enclosing forEach block, hence the new counterVariable block and ++counter (CounterIncrement) tags.</p>

<br><br><pre><code>		&lt;%counterVariable counterName = specialCounter1 %&gt;
		...
		&lt;%forEach%&gt;
			...
			&lt;%if exists = foreignKey%&gt;
				...
				&lt;%++counter optionalCounterName = specialCounter1 %&gt;
				...
			&lt;%endIf%&gt;</code></pre>
			...
		&lt;%endFor%&gt;</code></pre>
		...
		...
		&lt;%forEach%&gt;
			...
			&lt;%first optionalCounterName = specialCounter1 %&gt;
				...
			&lt;%endFirst%&gt;</code></pre>
			...
			&lt;%++counter optionalCounterName = specialCounter1 %&gt;
			...
		&lt;%endFor%&gt;</code></pre>
		...
	&lt;%endCounter%&gt;</code></pre>
*/
public class CounterVariable extends TemplateBlock_Base {

static public final String		BLOCK_NAME										= "counterVariable";
static public final String		BLOCK_END_NAME									= "endCounter";

static public final String		ATTRIBUTE_COUNTER_NAME							= "counterName";


// Data members
private String			m_counterName				= null;
private GeneralBlock	m_contentBlock				= null;


//*********************************
public CounterVariable() {
	super(BLOCK_NAME);
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
		// Get the general block of tags for the <if> tag.
		GeneralBlock t_generalBlock	= new GeneralBlock();
		if (!t_generalBlock.Parse(p_tokenizer)) {
			Logger.LogError("CounterVariable.Parse() general block parser failed in the block starting at line number [" + m_lineNumber + "].");
			return false;
		}

		m_contentBlock = t_generalBlock;

		String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
		if (!t_endingTagName.equalsIgnoreCase("endCounter")) {
			Logger.LogError("CounterVariable.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].  The closing tag [" + BLOCK_END_NAME + "] was expected.");
			return false;
		}
	}
	catch (Throwable t_error) {
		Logger.LogException("CounterVariable.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "]: ", t_error);
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

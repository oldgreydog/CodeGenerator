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
	Allows a template to do something different on the first pass through a loop than what is done
	on every subsequent pass.

	<p>There are numerous places in code where you will want to generate things like parameter lists
	so you need to control when, in that example, you insert commas.  The <code>first</code>-<code>else</code>
	tags let you do that.  For example:</p>

	<pre><code>&lt;%foreach node = member  optionalCounterName = "loop1" %&gt;
	&lt;%first%&gt;
		&lt;%text%&gt;&lt;%endtext%&gt;
	&lt;%else%&gt;
		&lt;%text%&gt;,
&lt;%endtext%&gt;
	&lt;%endfirst%&gt;

	&lt;%text%&gt;       &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; class = "object" %&gt; p_&lt;%firstLetterToLowerCase member = &lt;%name%&gt;%&gt;&lt;%endtext%&gt;
&lt;%endfor%&gt;
</code></pre>

	<p>The first pass through this loop that is iterating over "member" nodes, the loop will add an
	empty text block in front of the parameter definition.  But for every iteration after that, the
	<code>else</code> block will add a comma and a new-line in front of the parameter definition.</p>

	<p>The optionalCounterName attribute lets you specify using a named loop counter from a foreach block other than the
	one directly containing this first block.</p>
 */
public class FirstElseBlock extends TemplateBlock_Base {

	static public final String		BLOCK_NAME							= "first";

	static public final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";

	private GeneralBlock		m_firstBlock			= null;
	private GeneralBlock		m_elseBlock				= null;
	private	String				m_optionalCounterName	= null;	// Providing a name for the loop counter lets you specify using a named loop counter from a foreach block other than the one directly containing this first block.

//	private ArrayList<Integer>	m_parentIterationCountList	= null;
	private final TreeMap<String, LoopCounter>		m_counterIDMap	= new TreeMap<>();


	//*********************************
	public FirstElseBlock() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("FirstElseBlock.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		// The attribute "optionalCounterName" is, obviously, optional, so we need to handle it that way.
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_COUNTER_NAME);
		if (t_nodeAttribute != null) {
			m_optionalCounterName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_optionalCounterName == null) {
				Logger.LogError("FirstElseBlock.Init() did not get the value from the [" + ATTRIBUTE_OPTIONAL_COUNTER_NAME + "] attribute.");
				return false;
			}
		}

		return true;
	}


	//*********************************
	@Override
	public FirstElseBlock GetInstance() {
		return new FirstElseBlock();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// Get the general block of tags for the <if> tag.
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("FirstElseBlock.Parse() general block parser failed in the block starting at [" + m_lineNumber + "].");
				return false;
			}

			m_firstBlock = t_generalBlock;


			// If the first block is followed by any elseif blocks, then consume them.
			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (t_endingTagName.equalsIgnoreCase("else")) {
				t_generalBlock	= new GeneralBlock();
				if (!t_generalBlock.Parse(p_tokenizer)) {
					Logger.LogError("FirstElseBlock.Parse() general block parser failed in elseif block in the block starting at [" + t_generalBlock.m_lineNumber + "].");
					return false;
				}

				m_elseBlock = t_generalBlock;

				t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			}

			if (!t_endingTagName.equalsIgnoreCase("endfirst")) {
				Logger.LogError("FirstElseBlock.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + t_generalBlock.m_lineNumber + "].  The closing tag [endif] was expected.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstElseBlock.Parse() failed with error in the block starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			LoopCounter t_iterationCounter = p_iterationCounter;
			if (m_optionalCounterName != null) {
				t_iterationCounter = p_iterationCounter.GetNamedCounter(m_optionalCounterName);

				if (t_iterationCounter == null) {
					Logger.LogError("FirstElseBlock.Evaluate() failed to find a loop counter with name [" + m_optionalCounterName + "] at line number [" + m_lineNumber + "].");
					return false;
				}

				// Since optionally "named" counters ID's don't change each iteration of an outer loop re-evaluates the foreach, we have to check to see if the named loop counter is a new instance reference and, if it is, count it as a new start of the loop.
				LoopCounter t_existingLoopCounter = m_counterIDMap.get(t_iterationCounter.GetCounterID());
				if (t_existingLoopCounter != t_iterationCounter)	// Since optionally "named" counters ID's don't change each iteration of an outer loop re-evaluates the foreach, we have to check to see if the named loop counter is a new instance reference and, if it is, count it as a new start of the loop.
					m_counterIDMap.remove(t_iterationCounter.GetCounterID());
			}

			// I think (!hope!) that using a tree map to keep the internal counter for each counter ID that we see will let us use variable blocks inside various levels of nested <foreach> loops and <if> blocks without having to worry about looking up the stack of counters to figure out if a particular evaluation is the first one or not for that particular counter.
			LoopCounter t_internalCounter = m_counterIDMap.get(t_iterationCounter.GetCounterID());
			boolean		t_firstTimeThrough	= false;
			if (t_internalCounter == null)
			{
				m_counterIDMap.put(t_iterationCounter.GetCounterID(), t_iterationCounter);
				t_firstTimeThrough	= true;
			}

			if (t_firstTimeThrough) {
				m_firstBlock.Evaluate(p_currentNode, p_rootNode, p_writer, t_iterationCounter);
			}
			else if (m_elseBlock != null) {
				m_elseBlock.Evaluate(p_currentNode, p_rootNode, p_writer, t_iterationCounter);
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstElseBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}
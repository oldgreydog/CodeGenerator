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

import java.util.*;

import codegenerator.generator.utils.*;



/**
<p>Allows a template to do something different on the first pass through a loop than what is done on every subsequent pass.</p>

<p>There are numerous places in code where you will want to generate things like parameter lists
so you need to control when, in that example, you insert commas.  The <code><b>first</b></code>-<code>else</code>
tags let you do that.</p>

<h3>Usage example</h3>

	<pre><code><b>&lt;%forEach node = member  optionalCounterName = "loop1" %&gt;

	&lt;%first%&gt;

		&lt;&lt;&lt; comment: note that it is possible to leave this block empty so that the first pass is essentially a NULLOP. &gt;&gt;&gt;

	&lt;%else%&gt;

		&lt;%text%&gt;,
&lt;%endtext%&gt;

	&lt;%endFirst%&gt;

	&lt;%text%&gt;       &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; class = "object" %&gt; p_&lt;%firstLetterToLowerCase member = &lt;%name%&gt;%&gt;&lt;%endtext%&gt;

&lt;%endFor%&gt;</b></code></pre>

<p>On the first pass through this loop, the <code><b>first</b></code> tag will do nothing.  But
for every iteration after that, the <code>else</code> tag will add a comma and a new-line in front of the
parameter definition.</p>

<h3>Attribute descriptions</h3>

<p>The optionalCounterName attribute lets you specify using a named loop counter from a {@link ForEach} tag or {@link CounterVariable} other than the
{@link ForEach} directly containing this <code><b>first</b></code> tag.  See {@link CounterVariable} for an example of this usage.  Since a <code><b>first</b></code>
can contain any other tags such as <code><b>forEach</b></code> or <code>if</code>, this can let you create rather complex logic.</p>
 */
public class FirstElse extends Tag_Base {

	static public final String		TAG_NAME							= "first";
	static public final String		TAG_ELSE_NAME						= "else";
	static public final String		TAG_END_NAME						= "endFirst";

	static private final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";

	private LoopCounter			m_currentCounter		= null;
	private int					m_lastCounterValue		= 0;
	private GeneralBlock		m_firstBlock			= null;
	private GeneralBlock		m_elseBlock				= null;
	private	String				m_optionalCounterName	= null;	// Providing a name for the loop counter lets you specify using a named loop counter from a forEach tag other than the one directly containing this first tag.


	//*********************************
	public FirstElse() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("FirstElse.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		// The attribute "optionalCounterName" is, obviously, optional, so we need to handle it that way.
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_COUNTER_NAME);
		if (t_nodeAttribute != null) {
			m_optionalCounterName = t_nodeAttribute.GetAttributeValueAsString();
			if (m_optionalCounterName == null) {
				Logger.LogError("FirstElse.Init() did not get the value from the [" + ATTRIBUTE_OPTIONAL_COUNTER_NAME + "] attribute.");
				return false;
			}
		}

		return true;
	}


	//*********************************
	@Override
	public FirstElse GetInstance() {
		return new FirstElse();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// Get the general block of tags for the <if> tag.
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("FirstElse.Parse() general block parser failed in the block starting at [" + m_lineNumber + "].");
				return false;
			}

			m_firstBlock = t_generalBlock;


			// If the first tag is followed by an else tag, then consume it.
			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (t_endingTagName.equalsIgnoreCase(TAG_ELSE_NAME)) {
				t_generalBlock	= new GeneralBlock();
				if (!t_generalBlock.Parse(p_tokenizer)) {
					Logger.LogError("FirstElse.Parse() general block parser failed in the [" + TAG_ELSE_NAME + "] tag in the tag starting at [" + t_generalBlock.m_lineNumber + "].");
					return false;
				}

				m_elseBlock = t_generalBlock;

				t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			}

			if (!t_endingTagName.equalsIgnoreCase(TAG_END_NAME)) {
				Logger.LogError("FirstElse.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + t_generalBlock.m_lineNumber + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstElse.Parse() failed with error in the tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			LoopCounter t_iterationCounter = p_evaluationContext.GetLoopCounter();
			if (m_optionalCounterName != null) {
				t_iterationCounter = p_evaluationContext.GetNamedCounter(m_optionalCounterName);
				if (t_iterationCounter == null) {
					Logger.LogError("FirstElse.Evaluate() failed to find a counter with name [" + m_optionalCounterName + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			if (t_iterationCounter == null) {
				Logger.LogError("FirstElse.Evaluate() failed to find a default loop counter and no optional counter name was specified at line number [" + m_lineNumber + "].");
				return false;
			}

			// Since an instance of First will exist across any number of passes for different config nodes and even output files, each time we come into a particular instance of First we have to check to see if the counter we are working with has changed and, if it has, reset this instance as if it has never seen the counter.
			if (m_currentCounter != null) {
				if (m_currentCounter != t_iterationCounter) {
					m_currentCounter	= t_iterationCounter;
					m_lastCounterValue	= 0;
				}
			}
			else
				m_currentCounter = t_iterationCounter;


			boolean	t_firstTimeThrough	= false;
			int		t_nextCounterValue	= t_iterationCounter.GetCounter();
			if (t_nextCounterValue == 0)
				return true;					// Until the counter is > 0, we can't even do the "first" pass, much less the "else", so we'll short-circuit out here.
			else if (m_lastCounterValue == 0)
				t_firstTimeThrough	= true;
			else if (m_lastCounterValue == t_nextCounterValue)
				return true;					// I don't think this can happen, but just in case, if the value hasn't changed, then we need to exit this loop.

			m_lastCounterValue	= t_nextCounterValue;

			if (t_firstTimeThrough) {
				LinkedList<Tag_Base> t_contents = m_firstBlock.GetChildTagList();
				if ((t_contents != null) && !t_contents.isEmpty()) {
					if (!m_firstBlock.Evaluate(p_evaluationContext)) {
						Logger.LogError("FirstElse.Evaluate() failed to evaluate the [first] tag at line number [" + m_lineNumber + "].");
						return false;
					}
				}
			}
			else if (m_elseBlock != null) {
				LinkedList<Tag_Base> t_contents = m_elseBlock.GetChildTagList();
				if ((t_contents != null) && !t_contents.isEmpty()) {
					if (!m_elseBlock.Evaluate(p_evaluationContext)) {
						Logger.LogError("FirstElse.Evaluate() failed to evaluate the [else] tag for [first] tag at line number [" + m_lineNumber + "].");
						return false;
					}
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FirstElse.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}
}

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

	<pre><code>&lt;%foreach node=member%&gt;
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
 */
public class FirstElseBlock extends TemplateBlock_Base {

	private GeneralBlock		m_firstBlock	= null;
	private GeneralBlock		m_elseBlock		= null;

//	private ArrayList<Integer>	m_parentIterationCountList	= null;
	private final TreeMap<Integer, Integer>		m_counterIDMap	= new TreeMap<>();


	//*********************************
	public FirstElseBlock() {
		super("first");
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		// No action needs to be taken here.
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
				Logger.LogError("FirstElseBlock.Parse() general block parser failed.");
				return false;
			}

			m_firstBlock = t_generalBlock;


			// If the first block is followed by any elseif blocks, then consume them.
			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (t_endingTagName.equalsIgnoreCase("else")) {
				t_generalBlock	= new GeneralBlock();
				if (!t_generalBlock.Parse(p_tokenizer)) {
					Logger.LogError("FirstElseBlock.Parse() general block parser failed in elseif block.");
					return false;
				}

				m_elseBlock = t_generalBlock;

				t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			}

			if (!t_endingTagName.equalsIgnoreCase("endfirst")) {
				Logger.LogError("FirstElseBlock.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "].  The closing tag [endif] was expected.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("FirstElseBlock.Parse() failed with error: ", t_error);
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
			// I think (!hope!) that using a tree map to keep the internal counter for each counter ID that we see will let us use variable blocks inside various levels of nested <foreach> loops and <if> blocks without having to worry about looking up the stack of counters to figure out if a particular evaluation is the first one or not for that particular counter.
			Integer t_internalCounter = m_counterIDMap.get(p_iterationCounter.GetCounterID());
			if (t_internalCounter == null) {
				m_counterIDMap.put(p_iterationCounter.GetCounterID(), 1);
				t_internalCounter	= 1;
			}
			else
				m_counterIDMap.put(p_iterationCounter.GetCounterID(), ++t_internalCounter);

			if (t_internalCounter == 1) {
				m_firstBlock.Evaluate(p_currentNode, p_rootNode, p_writer, p_iterationCounter);
			}
			else if (m_elseBlock != null) {
				m_elseBlock.Evaluate(p_currentNode, p_rootNode, p_writer, p_iterationCounter);
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("FirstElseBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

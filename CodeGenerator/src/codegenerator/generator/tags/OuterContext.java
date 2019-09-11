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



import coreutil.config.*;
import coreutil.logging.*;

import java.util.*;

import codegenerator.generator.utils.*;



/**
	<p>The outer context tags are such special-case tags that you may never find a use for them.  I have a certain type of
	relationship between database tables that I define in my config value files and I wanted to use that relationship info
	to generate deletions in my cached DAO's.  But to do that, I needed to be able to jump a <code>forEach</code> to a node ABOVE the current
	one the template was pointing at in the config values tree.  And I needed to be able to reference values from both contexts in the
	template code inside the <code>forEach</code> that was pointed outside the parent <code>forEach</code>.  That's where the outer context tags come in.</p>

	<p>There are two tags.  The first is "outerContext".  This one is used to basically set a virtual maker in the loops to grab the
	node context wherever it is placed in the template.</p>

	<p>The second tag is "outerContextEval".  This one is used inside the inner <code>forEach</code> loop(s) so that you can access a value that's
	in the outer node context held by the "OuterContext" tag.</p>

	<p>To take advantage of this new functionality, the <code>forEach</code> tag's "node" attribute can now take the parent reference characters ("^")
	in its value.  Here's an abbreviated example of the usage of these tags (the 1., 2., 3. and 4. are for reference in the explanation below):</p>

	<code>&lt;!-- At this point in the template, we are inside a <code>&lt;%foreach node = "table" %&gt;</code> tag, so the current
		node context is pointing to a particular "table" node --&gt;</code>

<br><br><pre><code>		&lt;%outerContext contextname = parentTable %&gt;
	1.		&lt;%foreach node = ^table  optionalCounterName = innerTable %&gt;
	2.			&lt;%foreach node = column %&gt;
					&lt;%foreach node = foreignKey %&gt;
	3.					&lt;%if &lt;%and &lt;%parentTableName%&gt; = &lt;%outerContextEval contextname = parentTable targetvalue = sqlName %&gt;
									&lt;%not &lt;%parentTableName%&gt; = &lt;%^^sqlName%&gt; %&gt; == true %&gt; == true %&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
						&lt;%endif%&gt;
					&lt;%endfor%&gt;
				&lt;%endfor%&gt;

				&lt;%foreach node = tableRelationship %&gt;
					&lt;%if &lt;%and &lt;%parentTableName%&gt; = &lt;%outerContextEval contextname = parentTable targetvalue = sqlName %&gt;
								&lt;%not &lt;%parentTableName%&gt; = &lt;%^sqlName%&gt; %&gt; == true %&gt; == true %&gt;
						&lt;%first  optionalCounterName = innerTable %&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
						&lt;%endfirst%&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
					&lt;%endif%&gt;
				&lt;%endfor%&gt;
			&lt;%endfor%&gt;
	4.	&lt;%endcontext%&gt;</code></pre>

	<p>Once the outerContext tag is hit, you can then jump the node pointer to a different context with a forEach as shown at 1.
	Since that forEach's node attribute value is "^table", that tells the forEach to jump up ("^") one parent (which is the "root"
	node in my database config values file) and start iterating over the "table" nodes under that parent.</p>

	<p>!!It is super important to note that now that we are inside the new parent context at 1., all node and value references inside
	that forEach (i.e. 2.) are in that context, so you don't have to use the "^" with them!!</p>

	<p>3. shows how to use outerContextEval to access a value in the outer context from inside the new inner context.</p>

	<p>At 4., the "endcontext" end tag closes the designated outer context and that context no longer exists from there on.</p>
 */
public class OuterContext extends TemplateBlock_Base {

	static public final String	BLOCK_NAME		= "outerContext";


	// Data members
	private String			m_contextName		= null;
	private GeneralBlock	m_contentBlock		= null;


	//*********************************
	public OuterContext() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			m_lineNumber = p_tagParser.GetLineNumber();

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute("contextname");
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContext.Init() failed to find the required attribute [contextname] at line number [" + m_lineNumber + "].");
				return false;
			}

			m_contextName	= t_nodeAttribute.GetAttributeValueAsString();

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("OuterContext.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public OuterContext GetInstance() {
		return new OuterContext();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// Get the general block of tags for the <if> tag.
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("OuterContext.Parse() general block parser failed.");
				return false;
			}

			m_contentBlock = t_generalBlock;

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase("endcontext")) {
				Logger.LogError("OuterContext.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "].  The closing tag [endif] was expected.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("OuterContext.Parse() failed with error: ", t_error);
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
			OuterContextManager.SetOuterContext(m_contextName, p_currentNode);

			if (!m_contentBlock.Evaluate(p_currentNode, p_rootNode, p_writer, p_iterationCounter)) {
				Logger.LogError("OuterContext.Evaluate() failed to evaluate its content block.");
				return false;
			}

			OuterContextManager.RemoveOuterContext(m_contextName);
		}
		catch (Throwable t_error) {
			Logger.LogError("OuterContext.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

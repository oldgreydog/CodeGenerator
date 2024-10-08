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

import codegenerator.generator.utils.*;



/**
<p>Captures the current config context node at that point in the template so that nested tags can access it no matter how their context is changed.</p>

<p>The outer context tags are such special-case tags that you may never find a use for them.  I have a foreign-key
relationship between database tables that I define in my config value files and I wanted to use that relationship info
to generate deletions in my cached DAO's.  But to do that, I needed to be able to jump a <code><b>forEach</b></code> tag to a node ABOVE the current
one the template was pointing at in the config values tree.  And I needed to be able to reference values from both contexts in the
template code inside the <code><b>forEach</b></code> that was pointed outside the parent <code><b>forEach</b></code>.  That's where the outer context tags come in.</p>

<p>There are two tags.  The first is <code><b>outerContext</b></code>.  This one is used to basically set a virtual maker in the loops to grab the
node context wherever it is placed in the template.</p>

<pre><code><b>&lt;%outerContext contextName = parentTable  optionalJumpToParentContext = "^^" %&gt;</b></code></pre>

<p>The second tag is {@link OuterContextEval}.  This one is used inside the inner <code><b>forEach</b></code> loop(s) so that you can access a value that's
in the outer node context held by the <code><b>outerContext</b></code> tag.</p>

<pre><code><b>&lt;%outerContextEval contextName = parentTable targetValue = sqlName %&gt;</b></code></pre>

<p>There are two ways to take advantage of this functionality.  The <code><b>forEach</b></code> tag's <code><b>node</b></code> attribute can now take the parent reference characters ("^")
in its value.  That will jump the context up one parent node per carret and then execute the <code><b>forEach</b></code> loop from that context.  The other way is to use
the optionalJumpToParentContext attribute on the <code><b>outerContext</b></code> tag to jump the context up.  That option is discussed below.</p>

<h3>Usage example</h3>

<p>Here's an abbreviated example of the usage of these tags (the 1., 2., 3. and 4. are for reference in the explanation below):</p>

<p>At this point in the template, we are inside a <code><b>&lt;%forEach node = "table" %&gt;</b></code> tag, so the current
	node context is pointing to a particular "table" node.</p>

<pre>		<code><b>&lt;%outerContext contextName = parentTable %&gt;
	1.		&lt;%forEach node = ^table  optionalCounterName = innerTable %&gt;
	2.			&lt;%forEach node = column %&gt;
					&lt;%forEach node = foreignKey %&gt;
	3.					&lt;%if &lt;%and &lt;%parentTableName%&gt; = &lt;%outerContextEval contextName = parentTable targetValue = sqlName %&gt;
									&lt;%not &lt;%parentTableName%&gt; = &lt;%^^sqlName%&gt; %&gt; == true %&gt; == true %&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
						&lt;%endIf%&gt;
					&lt;%endFor%&gt;
				&lt;%endFor%&gt;

				&lt;%forEach node = tableRelationship %&gt;
					&lt;%if &lt;%and &lt;%parentTableName%&gt; = &lt;%outerContextEval contextName = parentTable targetValue = sqlName %&gt;
								&lt;%not &lt;%parentTableName%&gt; = &lt;%^sqlName%&gt; %&gt; == true %&gt; == true %&gt;
						&lt;%first  optionalCounterName = innerTable %&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
						&lt;%endfirst%&gt;
							&lt;%text%&gt;...&lt;%endtext%&gt;
					&lt;%endIf%&gt;
				&lt;%endFor%&gt;
			&lt;%endFor%&gt;
	4.	&lt;%endcontext%&gt;</b></code></pre>

<p>Once the  <code><b>outerContext</b></code>  tag is hit, you can then jump the node pointer to a different context with a <code><b>forEach</b></code> as shown at 1.
Since that forEach's node attribute value is "^table", that tells the <code><b>forEach</b></code> to jump up ("^") one parent (which is the "root"
node in my database config values file) and start iterating over the "table" nodes under that parent.</p>

<p>!!It is super important to note that now that we are inside the new parent context at 1., all node and value references inside
that <code><b>forEach</b></code> (i.e. 2.) are in that context, so you don't have to use the "^" with them!!</p>

<p>3. shows how to use <code><b>outerContextEval</b></code> to access a value in the outer context from inside the new inner context.</p>

<p>At 4., the <code><b>endcontext</b></code> end tag closes the designated outer context and that context no longer exists from there on.</p>

<br><p>Now let's go back to the <code><b>optionalJumpToParentContext</b></code> attribute on the <code><b>outerContext</b></code> tag.  The driver
for adding this attribute was that I had one or more files that I only wanted to generate once based on a flag value used on my
API config.  To do that, I needed to be able to jump the parent context up some number of levels without being forced to use a
<code><b>forEach</b></code> loop.  An inner <code><b>forEach</b></code> would make it impossible to generate the file since the file template needs
to do the <code><b>forEach</b></code> internally to generate its content but you would be in the wrong context to do that.</p>

<p>Here's an example of this usage:</p>

<pre>	<code><b>&lt;%forEach node=manager%&gt;
		&lt;%file template=manager.template						filename="&lt;%className%&gt;Manager.java"				destDir="&lt;%root.global.outputPath%&gt;/&lt;%firstLetterToLowerCase value = &lt;%className%&gt; %&gt;" %&gt;

		&lt;%forEach node=api%&gt;
			&lt;%file template=manager_net_client.template		filename="&lt;%apiName%&gt;_NET.java"						destDir="&lt;%root.global.outputPath%&gt;/&lt;%firstLetterToLowerCase value = &lt;%^className%&gt; %&gt;" %&gt;
			&lt;%file template=manager_net_server.template		filename="&lt;%apiName%&gt;_NET_Server.java"				destDir="&lt;%root.global.outputPath%&gt;/&lt;%firstLetterToLowerCase value = &lt;%^className%&gt; %&gt;" %&gt;
		&lt;%endFor%&gt;

		&lt;%if &lt;%accessType%&gt; = "transparent" %&gt;
			&lt;%file template=manager_interface.template		filename = "&lt;%className%&gt;Manager_Interface.java"		destDir = "&lt;%root.global.outputPath%&gt;/&lt;%firstLetterToLowerCase value = &lt;%className%&gt; %&gt;" %&gt;

			&lt;%first%&gt;
				&lt;%outerContext contextName = root  optionalJumpToParentContext = "^" %&gt;
					&lt;%file template=manager_factory.template				filename = "&lt;%root.global.serviceGroupName%&gt;ManagerFactory.java"		destDir = "&lt;%root.global.outputPath%&gt;/factory" %&gt;
					&lt;%file template=manager_factory_config.template		filename = "&lt;%root.global.serviceGroupName%&gt;ManagerFactoryConfig.xml"	destDir = "&lt;%root.global.outputPath%&gt;/factory" %&gt;
				&lt;%endcontext%&gt;
			&lt;%endFirst%&gt;
		&lt;%endIf%&gt;
	&lt;%endFor%&gt;</b></code></pre>
 */
public class OuterContext extends Tag_Base {

	static public final String		TAG_NAME										= "outerContext";
	static public final String		TAG_END_NAME									= "endContext";

	static private final String		ATTRIBUTE_CONTEXT_NAME							= "contextName";
	static private final String		ATTRIBUTE_OPTIONAL_JUMP_TO_PARENT_CONTEXT		= "optionalJumpToParentContext";


	// Data members
	private String			m_contextName				= null;
	private int				m_jumpToParentContextCount	= 0;	// This will let you jump to a parent context without using a forEach loop.
	private GeneralBlock	m_contents					= null;


	//*********************************
	public OuterContext() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("OuterContext.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_CONTEXT_NAME);
			if (t_nodeAttribute == null) {
				Logger.LogError("OuterContext.Init() failed to find the required attribute [" + ATTRIBUTE_CONTEXT_NAME + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			m_contextName = t_nodeAttribute.GetAttributeValueAsString();

			// Handle the optional attribute optionalJumpToParentContext.
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_JUMP_TO_PARENT_CONTEXT);
			if (t_nodeAttribute != null) {
				String t_contextJump = t_nodeAttribute.GetAttributeValueAsString();
				if (t_contextJump == null) {
					Logger.LogError("OuterContext.Init() failed to get the string value of attribute [" + ATTRIBUTE_OPTIONAL_JUMP_TO_PARENT_CONTEXT + "] at line number [" + m_lineNumber + "].");
					return false;
				}

				for (int i = 0; i < t_contextJump.length(); ++i) {
					if (t_contextJump.charAt(i) != '^') {
						Logger.LogError("OuterContext.Init() found an invalid character at index [" + i + "] in the value [" + t_contextJump + "] of attribute [optionalJumpToParentContext] at line number [" + m_lineNumber + "].  This attribute only accepts carrets (^) in its value.");
						return false;
					}

					++m_jumpToParentContextCount;
				}
			}


			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("OuterContext.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
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
				Logger.LogError("OuterContext.Parse() general block parser failed in the block starting at line number [" + m_lineNumber + "].");
				return false;
			}

			m_contents = t_generalBlock;

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase(TAG_END_NAME)) {
				Logger.LogError("OuterContext.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("OuterContext.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			p_evaluationContext.GetOuterContextManager().SetOuterContext(m_contextName, p_evaluationContext.GetCurrentNode());

			ConfigNode t_context = p_evaluationContext.GetCurrentNode();
			for (int i = 0; i < m_jumpToParentContextCount; ++i) {
				t_context = t_context.GetParentNode();
				if (t_context == null) {
					Logger.LogError("OuterContext.Evaluate() was configured with a parent context jump count of [" + m_jumpToParentContextCount + "], but that goes past the root node.");
					return false;
				}
			}

			p_evaluationContext.PushNewCurrentNode(t_context);

			if (!m_contents.Evaluate(p_evaluationContext)) {
				Logger.LogError("OuterContext.Evaluate() failed to evaluate its content tag.");
				p_evaluationContext.PopCurrentNode();
				return false;
			}

			p_evaluationContext.PopCurrentNode();

			p_evaluationContext.GetOuterContextManager().RemoveOuterContext(m_contextName);
		}
		catch (Throwable t_error) {
			Logger.LogException("OuterContext.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

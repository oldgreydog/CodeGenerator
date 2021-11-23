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



import coreutil.config.*;
import coreutil.logging.*;

import java.util.*;

import codegenerator.generator.utils.*;



/**
	Provides the <code>if</code> and <code>elseIf</code> tag parsing and evaluation.

	<p>Here is a sample of the if-else tags that are parsed by this code:</p>

	<pre><code>&lt;%if  &lt;%type%&gt; = Integer %&gt;
	...
&lt;%elseIf  &lt;%type%&gt; = Long %&gt;
	...
&lt;%else%&gt;
	...
&lt;%endIf%&gt;</code></pre>

	<p>Where &lt;%type%&gt; is a {@link ConfigVariable} tag object that converts a config value of
	the name "type" into the value it has in the config file under the current parent node.</p>

	<p>The test condition for the <code>if</code> and <code>elseIf</code> tags is more generically
	defined like this:</p>

	<p><code>&lt;%if  [any tag that evaluates to a string] = [string constant] %&gt;</code></p>

	<p>So the other common instance of the <code>if</code> and <code>elseIf</code> tags you'll see
	in the example templates uses <code>typeConvert</code> because it also evaluates to a string:</p>

	<p><code>&lt;%if  &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; class = "object" %&gt; = Integer %&gt;</code></p>

	<p>The condition uses "=" instead of "==" because this relies on the default tag parsing done by
	{@link TagParser} which uses {@link TagAttributeParser}	to parse the attributes.  Using the tag
	attribute format meant no extra effort to make this work whereas going to "==" would have required
	custom parse code which I had no interest in doing.</p>

	<p>However, I have added the ability to use this if/else logic to test for the existence of a child
	node type.  Thus it is now possible to have if-tags like this:</p>

	<pre><code>&lt;%if  exists = parameter %&gt;
	...
&lt;%elseIf  exists = returnType %&gt;
	...
&lt;%else%&gt;
	...
&lt;%endIf%&gt;</code></pre>

	<p>And you can mix conditions if it makes sense:</p>

	<pre><code>&lt;%if  exists = parameter %&gt;
	...
&lt;%elseIf  &lt;%type%&gt; = Long %&gt;
	...
&lt;%else%&gt;
	...
&lt;%endIf%&gt;</code></pre>

	<p>I finally added boolean logic because I needed it.  You now have simplistic <code>and</code> and <code>or</code> functionality
	to use in your <code>if</code> and <code>elseIf</code> tags.  As with all of the other tags, <code>and</code> and <code>or</code>
	use the same XML tag layout for simplicity of parsing.</p>

	<p><code>&lt;%[and/or]  [some tag that evaluates to a string] = [some string const] [...] %&gt;</code></p>

	<p>Since <code>and/or</code> are simple tags that evaluate to a string (<code>true/false</code>),
	then they can be used as the right side of <code>if</code> attributes, such as:</p>

	<p><code>&lt;%if  &lt;%[and/or]  [some tag that evaluates to a string] = [some string const] [...] %&gt; = [true/false] %&gt;</code></p>

	<p>It's critical to notice that there is an <code>= [true/false]</code> after the <code>and/or</code> tag.
	That is required to make the <code>and/or</code> a complete attribute in the <code>if</code> tag, but it also lets you
	do a crude !and or !or by setting it <code>= false</code> instead of <code>= true</code>.</p>

	<p>An <code>if</code> tag only accepts one test condition attribute, but obviously <code>and/or</code> will take any number
	of test condition attributes.  They also do short-circuit evaluation such that <code>and</code> returns <code>false</code>
	on the first test condition attribute that is <code>false</code> and <code>or</code> returns <code>true</code> on the first test condition
	attribute that is <code>true</code>.</p>

	<p>Here is an example of an <code>if</code>/<code>elseIf</code> tag using and <code>or</code> and <code>and</code> tags:</p>

	<pre><code>&lt;%if  &lt;%or  &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = String
		&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = Calendar
		&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = byte[] %&gt; = "true" %&gt;
	...
&lt;%elseIf  &lt;%and	&lt;%name%&gt; = "Company"  &lt;%isNullable%&gt; = "true" %&gt; = "true" %&gt;
	...
&lt;%else%&gt;
	...
&lt;%endIf%&gt;</code></pre>

	<p><code>and</code> and <code>or</code> should also nest inside each other so that you can make
	fairly complicated conditionals if you need them.</p>

*/
public class IfElse extends Tag_Base {

	/*
	 * This is a simple helper class.
	 */
	static protected class IfCondition extends Tag_Base {

		static public final String		ATTRIBUTE_EXISTS	= "exists";


		private boolean		m_testExists			= false;
		private Tag_Base	m_attributeName			= null;
		private Tag_Base	m_attributeValue		= null;
		private int			m_lineNumber			= -1;


		//*********************************
		public IfCondition() {}


		//*********************************
		@Override
		public boolean Init(TagParser p_tagParser) {
			try {
				if (!super.Init(p_tagParser)) {
					Logger.LogError("IfCondition.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
					return false;
				}

				// I've added the special-case attribute "exists" so that we can use the if/else tag to test for the existence of a child node type and then execute different tag blogs depending on that test.
				TagAttributeParser t_conditionAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_EXISTS);
				if (t_conditionAttribute != null) {
					m_testExists = true;
				}
				else {
					// This is a really bu-tugly way to get this value, but it was the only way to do with a generic tag parser.
					Vector<TagAttributeParser> t_attributes = p_tagParser.GetTagAttributes();
					if (t_attributes.isEmpty()) {
						Logger.LogError("IfCondition.Init() the tag attribute parser did not contain any config variable attributes at line number [" + m_lineNumber + "].  One is required for a IfElse.");
						return false;
					}

					if (t_attributes.size() > 1) {
						Logger.LogError("IfCondition.Init() the tag attribute parser contained [" + t_attributes.size() + "] config variable attributes at line number [" + m_lineNumber + "].  Only one is required for a IfElse.");
						return false;
					}

					t_conditionAttribute = t_attributes.get(0);
					if (t_conditionAttribute == null) {	// This should never happen but Vector appears to allow NULL values, so just in case...
						Logger.LogError("IfCondition.Init() did not find the [template] attribute that is required for IfElse tags at line number [" + m_lineNumber + "].");
						return false;
					}

					m_attributeName	= t_conditionAttribute.GetAttributeName();
				}

				m_attributeValue	= t_conditionAttribute.GetAttributeValue();
				m_lineNumber		= p_tagParser.GetLineNumber();

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogException("IfCondition.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return false;
			}
		}


		//*********************************
		/**
		 * This is only used by If_Boolean subclasses to initialize their child IfConditions.
		 * @param p_tagAttributeParser
		 * @return
		 */
		public boolean Init(TagAttributeParser p_tagAttributeParser) {
			try {
				String t_attributeName = p_tagAttributeParser.GetAttributeNameAsString();
				if ((t_attributeName != null) && t_attributeName.equalsIgnoreCase(ATTRIBUTE_EXISTS)) {
					m_testExists = true;
				}
				else {
					m_attributeName = p_tagAttributeParser.GetAttributeName();
					if (m_attributeName == null) {
						Logger.LogError("IfCondition.Init(TagAttributeParser) did not get a left-hand argument from attribute that is required for IfElse tags at line number [" + p_tagAttributeParser.GetLineNumber() + "].");
						return false;
					}
				}

				m_attributeValue	= p_tagAttributeParser.GetAttributeValue();
				m_lineNumber	= p_tagAttributeParser.GetLineNumber();

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogException("IfCondition.Init(TagAttributeParser) failed with error: ", t_error);
				return false;
			}
		}


		//*********************************
		@Override
		public Tag_Base GetInstance() {
			return null;	// This should never be called for this class.
		}


		//*********************************
		public Boolean Test(EvaluationContext p_evaluationContext)
		{
			// An else will not test for existence of a child node nor will it have a m_configVariable or m_compareValue so it is always TRUE;
			if (!m_testExists &&
				((m_attributeName == null) ||
				 (m_attributeValue   == null)))
			{
				return true;
			}

			String t_righthandValue = Tag_Base.EvaluateToString(m_attributeValue, p_evaluationContext);
			if (t_righthandValue == null) {
				Logger.LogError("IfCondition.Test(TagAttributeParser) failed to evaluate the righthand value of its attribute at line number [" + m_lineNumber + "].");
				return null;
			}

			// If we are testing for the existence of a child node type, then we have a completely different test to do.
			if (m_testExists) {
				try {
					ConfigNode t_nextConfigNode	= p_evaluationContext.GetCurrentNode();
					if (t_righthandValue.startsWith("root.")) {
						t_nextConfigNode = p_evaluationContext.GetRootNode();
						t_righthandValue = t_righthandValue.replace("root.", "");	// Remove the "root." from the reference so that it will work correctly below.
					}
					else if (t_righthandValue.contains("^")) {
						// This allows us to use the "parent" reference character '^' to check for the existence of nodes in any level of parent.
						int			t_currentIndex	= -1;
						while ((t_currentIndex = t_righthandValue.indexOf('^', t_currentIndex + 1)) >= 0)
							t_nextConfigNode = t_nextConfigNode.GetParentNode();

						// Remove the carrets ("^") from the value so that it will work correctly below.
						t_righthandValue = t_righthandValue.replace("^", "");
					}

					if (t_nextConfigNode.GetNode(t_righthandValue) != null)
						return true;

					return false;
				}
				catch (Throwable t_error) {
					Logger.LogException("IfExistsBlock.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
					return null;
				}
			}
			else {	// Otherwise, do the default test.
				String t_lefthandValue = Tag_Base.EvaluateToString(m_attributeName, p_evaluationContext);
				if (t_lefthandValue == null) {
					Logger.LogError("IfCondition.Test(TagAttributeParser) failed to evaluate the lefthand value of its attribute at line number [" + m_lineNumber + "].");
					return null;
				}

				return t_lefthandValue.equalsIgnoreCase(t_righthandValue);
			}
		}


		//*********************************
		@Override
		public boolean Parse(TemplateTokenizer p_tokenizer) {
			return false;
		}


		//*********************************
		@Override
		public boolean Evaluate(EvaluationContext p_evaluationContext)
		{
			try {
				// Now that I've changed Tag_Base.Evaluate() to fail on an empty m_tagList, Any child tags that want to allow empty content blocks have to override Evaluate() and handle that empty case to avoid the error.
				if ((m_tagList != null) && !m_tagList.isEmpty()) {
					if (!super.Evaluate(p_evaluationContext)) {
						Logger.LogError("IfCondition.Evaluate() failed to evaluate an IF condition's contents at line [" + m_lineNumber + "].");
						return false;
					}
				}
			}
			catch (Throwable t_error) {
				Logger.LogException("IfCondition.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return false;
			}

			return true;
		}


		//*********************************
		@Override
		public String Dump(String p_tabs) {
			StringBuilder t_dump = new StringBuilder();

			t_dump.append(p_tabs + "Tag name        :  " + m_name			+ "\n");
			t_dump.append(p_tabs + "Compare Value   :  " + m_attributeValue	+ "\n");


			if (m_tagList != null) {
				for (Tag_Base t_nextTag: m_tagList) {
					t_dump.append("\n\n");
					t_dump.append(t_nextTag.Dump(p_tabs + "\t"));
				}
			}

			return t_dump.toString();
		}
	}



	//===========================================

	static public final String		TAG_NAME				= "if";
	static public final String		BLOCK_ELSE_IF_NAME		= "elseIf";
	static public final String		BLOCK_ELSE_NAME			= "else";
	static public final String		TAG_END_NAME			= "endIf";


	//*********************************
	public IfElse() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("IfElse.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			IfCondition t_ifCondition = new IfCondition();
			if (!t_ifCondition.Init(p_tagParser)) {
				Logger.LogError("IfElse.Init() failed to initialize the first IfCondition block at line number [" + m_lineNumber + "].");
				return false;
			}

			AddChildNode(t_ifCondition);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("IfElse.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public IfElse GetInstance() {
		return new IfElse();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			if (m_tagList == null) {
				Logger.LogError("IfElse.Parse() didn't find the condition attribute for the IF tag at line [" + m_lineNumber + "].");
				return false;
			}

			IfCondition	t_nextCondition = (IfCondition)m_tagList.getFirst();	// The first IfCondition should have been created in Init() from the tag parser data.

			// Get the general block of tags for the <if> tag.
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("IfElse.Parse() general block parser failed in the block starting at [" + m_lineNumber + "].");
				return false;
			}

			t_nextCondition.AddChildNode(t_generalBlock);

			// If the first tag is followed by any elseIf tags, then consume them.
			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			while (t_endingTagName.equalsIgnoreCase(BLOCK_ELSE_IF_NAME)) {
				t_nextCondition = new IfCondition();
				if (!t_nextCondition.Init(t_generalBlock.GetUnknownTag())) {
					Logger.LogError("IfElse.Parse() failed to init an [" + BLOCK_ELSE_IF_NAME + "] tag at line [" + p_tokenizer.GetLineCount() + "].");
					return false;
				}

				AddChildNode(t_nextCondition);

				t_generalBlock	= new GeneralBlock();
				if (!t_generalBlock.Parse(p_tokenizer)) {
					Logger.LogError("IfElse.Parse() general block parser failed in [" + BLOCK_ELSE_IF_NAME + "] tag starting at line [" + t_nextCondition.m_lineNumber + "].");
					return false;
				}

				t_nextCondition.AddChildNode(t_generalBlock);

				t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			}

			// If there is an else tag, then consume it.
			if (t_endingTagName.equalsIgnoreCase(BLOCK_ELSE_NAME)) {
				t_nextCondition = new IfCondition();	// Else tags don't have attributes so we can not call Init() here.  All we need is to create the object and add it to m_tagList.
				AddChildNode(t_nextCondition);

				t_generalBlock	= new GeneralBlock();
				if (!t_generalBlock.Parse(p_tokenizer)) {
					Logger.LogError("IfElse.Parse() general block parser failed in else block at line [" + t_nextCondition.m_lineNumber + "].");
					return false;
				}

				t_nextCondition.AddChildNode(t_generalBlock);

				t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			}

			if (!t_endingTagName.equalsIgnoreCase(TAG_END_NAME)) {
				Logger.LogError("IfElse.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("IfElse.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)

	{
		try {
			if (m_tagList == null) {
				Logger.LogError("IfElse.Evaluate() doesn't have an IF condition attribute at line [" + m_lineNumber + "].");
				return false;
			}

			ListIterator<Tag_Base> t_tagIterator = m_tagList.listIterator();
			IfCondition t_nextCondition;
			Boolean t_result;
			while (t_tagIterator.hasNext()) {
				t_nextCondition = (IfCondition)t_tagIterator.next();

				// We only evaluate the first condition that returns TRUE.
				t_result = t_nextCondition.Test(p_evaluationContext);
				if (t_result == null)
					return false;
				else if (t_result) {
					LinkedList<Tag_Base> t_contents = t_nextCondition.GetChildNodeList();
					if ((t_contents != null) && !t_contents.isEmpty()) {
						if (!t_nextCondition.Evaluate(p_evaluationContext)) {
							Logger.LogError("IfElse.Evaluate() failed to evaluate an IF condition's contents at line [" + m_lineNumber + "].");
							return false;
						}
					}

					return true;
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("IfElse.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;	// We could get here if there is no else tag to evaluate as the default.
	}
}

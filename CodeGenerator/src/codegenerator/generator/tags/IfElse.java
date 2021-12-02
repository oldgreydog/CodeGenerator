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
	Provides <code>if-elseIf-else</code> conditional evaluation.

	<p>Here is an example of the complete <code>if-elseIf-else</code> tag structure:</p>

	<pre>	<code>&lt;%if [one top-level boolean attribute] %&gt;
		...
	&lt;%elseIf [one top-level boolean attribute] %&gt;
		...
	&lt;%else%&gt;
		...
	&lt;%endIf%&gt;</code></pre>

	<p>The <code><B>if</B></code> and <code><B>elseIf</B></code> tags only accept one test condition attribute.</p>

	<p>As with typical programming language if-statements, you can use any combination of these tags that you need.
	The test condition for the <code><B>if</B></code> and <code><B>elseIf</B></code> tags is more generically
	defined like this:</p>

	<p><code>&lt;%if [something that evaluates to a string] = [something that evaluates to a string] %&gt;</code></p>

	<p>Where other tags have specific named attributes, whether required or optional, the <code><B>if</B></code> tag
	and all of the other boolean tags treat their attributes as string comparisons.  So the attribute "name" and "value"
	can be anything that evaluates to a string, be it a tag or a string constant.</p>

	<p>The condition uses "=" instead of "==" because this relies on the default tag parsing done by
	{@link TagParser} which uses {@link TagAttributeParser}	to parse the attributes.  Using the tag
	attribute format meant no extra effort to make this work whereas going to "==" would have required
	custom parse code.  And using the standard attribute format means that the code can, with no special code,
	correctly recurse down through whatever complex, nested boolean conditions you construct.</p>

	<p>All of that means that you have a pretty wide range of options when it comes to boolean operations you can use.
	First, there are three traditional boolean tags:</p>

	<pre>	<code><B>and</B>
	<B>or</B>
	<B>not</B></code></pre>

	<p>There is also a special-case attribute that you can use to test for the existence of a config node or value:</p>

	<pre>	<code><B>exists</B></code></pre>

	<p>You can use many of the simple tags (tags that only have one tag and don't, therefore, contain
	child tags).  Since the tag must evaluate to a string, that includes tags like <code><B>camelcase</B></code>
	but excludes tags like <code><B>++counter</B></code></p>

	<pre>	<code><B>camelcase</B>
	<B>counter</B>
	<B>firstLetterToLowerCase</B>
	<B>typeConvert</B>
	<B>&lt;%DataType%&gt;</B> (config values)</code></pre>

	<p>And finally, you can use string constants.</p>


	<h4>Boolean tags</h4>

	<p>The boolean tags let you construct complex boolean tests with any number and combination of child boolean tags and/or
	simple tags and string constants.</p>

	<p>The <code><B>and</B></code> and <code><B>or</B></code> take any number attributes that can be a combination of child
	boolean tags and/or simple tags and string constants:</p>

	<pre>	<code>&lt;%[and/or]  [[something that evaluates to a string] = [something that evaluates to a string]] <B>[...]</B> %&gt;</code></pre>

	<p>The <code><B>not</B></code> only allows one attribute which can be a child boolean tag or a simple tag and string constant:</p>

	<pre>	<code>&lt;%not  [[something that evaluates to a string] = [something that evaluates to a string]] %&gt;</code></pre>

	<p><code><B>and</B></code>, <code><B>or</B></code> and <code><B>not</B></code> evaluate to only one of the string values
	<code><B>true</B></code> or <code><B>false</B></code>, so they must be used as an attribute in the following form:</p>

	<pre>	<code>&lt;%[and/or]  [something that evaluates to a string] = [something that evaluates to a string] <B>[...]</B> %&gt; = [true/false]</code>
	<code>&lt;%not  [[something that evaluates to a string] = [something that evaluates to a string]] %&gt; = [true/false]</code></pre>

	<p>It's critical to notice that there is an <code><B>= [true/false]</B></code> after the <code><B>and/or/not</B></code> tags.
	That is required to make the <code><B>and/or/not</B></code> a complete attribute, but it also lets you
	do a crude !and or !or by setting it <code><B>= false</B></code> instead of <code><B>= true</B></code>.  But for a <code><B>not</B></code>
	tag, you are as likely to use <code><B>= false</B></code> as much as you use <code><B>= true</B></code> because you are testing
	for the "not" of the enclosed boolean.  Given that the <code><B>not</B></code> only takes one attribute, you will likely only
	really need it when the one attribute is a simple-tag-and-string-const test.</p>

	<p>The <code><B>and/or</B></code> tags do in-order short-circuit evaluation such that <code><B>and</B></code> returns <code><B>false</B></code>
	on the first test condition attribute that is <code><B>false</B></code> and <code><B>or</B></code> returns <code><B>true</B></code> on the
	first test condition attribute that is <code><B>true</B></code>.</p>

	<p>Here is a simple example of an <code><B>if</B></code>/<code><B>elseIf</B></code> tag using and <code><B>or</B></code> and <code><B>and</B></code> tags:</p>

	<pre>	<code>&lt;%if  &lt;%or  &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = String
				&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = Calendar
				&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "object" %&gt; = byte[]	%&gt; = "true" %&gt;
		...
	&lt;%elseIf  &lt;%and &lt;%name%&gt; = "Company"
					&lt;%isNullable%&gt; = "true"	%&gt; = "true" %&gt;
		...
	&lt;%else%&gt;
		...
	&lt;%endIf%&gt;</code></pre>

	<p><code><B>and</B></code>, <code><B>or</B></code> and <code><B>not</B></code> can be nested inside each other so that you can make
	fairly complicated conditionals if you need them.</p>


	<h4>Exists</h4>

	<p>The <code><B>exists</B></code> attribute tests for the existence of a child
	node type.  For example:</p>

	<pre>	<code>&lt;%if  exists = parameter %&gt;
		...[Use the parameter node]...
	&lt;%elseIf  exists = returnType %&gt;
		...[Use the returnType value]...
	&lt;%else%&gt;
		...
	&lt;%endIf%&gt;</code></pre>


	<h4>Simple Tags</h4>

	<p>In the end, the boolean tags are only for grouping comparisons of simple tags and string constants like this example:</p>

	<p><code>&lt;%if  &lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; class = "object" %&gt; = Integer %&gt;</code></p>

	<p>Where &lt;%type%&gt; is a {@link ConfigVariable} tag that inputs a config value of the name "type" into the {@link TypeConvert} tag
	which, in this case, maps it to an equivalent Java object type as the tag output.  Therefore the example is checking to see if the type is <code><B>Integer</B></code>.</p>



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

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

import java.io.*;

import codegenerator.generator.utils.*;



/**
<p>Generates a special pair of start and end comments between which the user can insert their custom
code.  This guarantees that on subsequent re-generations over existing files, the custom code will be saved and
re-inserted in the correct place in the newly generated version of the file.  An example would
look like this:</p>

<pre><code> // StartCustomCode:ExtraMembers
private String m_name;
// EndCustomCode:ExtraMembers</code></pre>

<p>This tag is meant to be used by itself on an otherwise empty line inside a <code>text</code> tag.  The indentation before this tag
will be used to indent both the start and end comments the same amount in the output code.</p>

<h3>Usage examples</h3>

<p>Here are a few samples of this tag's usage (the first example would generate the comments shown above):</p>

<pre><code><b>&lt;%customCode key=ExtraMembers openingCommentCharacters=// %&gt;</b></code></pre>

<pre><code><b>&lt;%customCode key=LoadAll&lt;%className%&gt;CacheCode openingCommentCharacters=// %&gt;</b></code></pre>

<pre><code><b>&lt;%customCode key=XmlCustomValues openingCommentCharacters="&lt;!--" optionalClosingCommentCharacters="--&gt;" %&gt;</b></code></pre>

<h3>Attribute descriptions</h3>

<p><b><code>key</code></b>: this is the unique name that will identify the start/end tags.</p>

<p><b><code>openingCommentCharacters</code></b>: this is either the single-line comment characters or the opening comment characters that are used by whatever language is being generated.  In this example, it's "//" for java, c++, etc..</p>

<p><b><code>optionalClosingCommentCharacters</code></b>: this optional attribute is used for the closing comment characters for languages or file types that require them.  In this example, it's "-->" that close the "&lt;!--" opening comment for xml.</p>

<p>The key is critical.  It has to be unique in the file.  If the code where this tag is used is
outside a <code><b>forEach</b></code> tag, then you can optionally give it a key that is fixed, such
as <code>StaticMembers</code>.  However, if this tag is used inside a <code><b>forEach</b></code> tag,
then you must define a key that uses a raw or type-converted config value to make it unique within
the context. The <code>LoadAll&lt;%className%&gt;CacheCode</code> used in the example above is a
perfect example.  The config variable <code><b>&lt;%className%&gt;</b></code> will cause a unique tag to
be generated at that location in every pass through the <code><b>forEach</b></code> tag.</p>

<p><b>If you don't make the key unique, it will probably generate the first time you run the corrupted template, but
from that point on, you will get an error when you run the generate and you will have to delete the file or fix it by
renaming it and then re-merging the custom code after you've generated the new version of the file.  This is why you
should generate to a separate directory from the one you have your working code tree in, particularly if you are
creating/editing a template.</b></p>

<p>So if you are inside nested <code><b>forEach</b></code> tags, then you will probably need
to add a config value for each context, using the parent reference caret (^) where necessary.  So,
for example, if you were inside a <code><b>forEach</b></code> tag iterating over the "member" nodes under
an outer <code><b>forEach</b></code> tag that's iterating over "class" nodes, then you might create a tag like:</p>

<p><code><b>&lt;%customCode key=Validate&lt;%memberName%&gt;Of&lt;%^className%&gt; openingCommentCharacters=// %&gt;</b></code></p>

<p>The caret (^) in front of "className" tells {@link ConfigValue} to go up one config node before trying
to find the value named "className".</p>
 */
public class CustomCode extends Tag_Base {

	static public final String		TAG_NAME									= "customCode";

	static private final String		ATTRIBUTE_KEY								= "key";
	static private final String		ATTRIBUTE_OPENING_COMMENT_CHARS				= "openingCommentCharacters";
	static private final String		ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS	= "optionalClosingCommentCharacters";


	// Data members
	private	Tag_Base	m_key							= null;
	private	String		m_openingCommentCharacters		= null;
	private	String		m_closingCommentCharacters		= null;


	//*********************************
	public CustomCode() {
		super(TAG_NAME);
		m_isSafeForText = true;
	}


	//*********************************
	@Override
	public CustomCode GetInstance() {
		return new CustomCode();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("CustomCode.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_KEY);
		if (t_nodeAttribute == null) {
			Logger.LogError("CustomCode.Init() did not find the [" + ATTRIBUTE_KEY + "] attribute that is required for CustomCode tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_key = t_nodeAttribute.GetAttributeValue();
		if (m_key == null) {
			Logger.LogError("CustomCode.Init() did not get the [" + ATTRIBUTE_KEY + "] string from attribute that is required for CustomCode tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPENING_COMMENT_CHARS);
		if (t_nodeAttribute == null) {
			Logger.LogError("CustomCode.Init() did not find the [" + ATTRIBUTE_OPENING_COMMENT_CHARS + "] attribute that is required for CustomCode tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_openingCommentCharacters = t_nodeAttribute.GetAttributeValueAsString();
		if (m_openingCommentCharacters == null) {
			Logger.LogError("CustomCode.Init() did not get the [" + ATTRIBUTE_OPENING_COMMENT_CHARS + "] value from attribute that is required for CustomCode tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS);
		if (t_nodeAttribute != null) {
			m_closingCommentCharacters = t_nodeAttribute.GetAttributeValueAsString();
			if (m_closingCommentCharacters == null) {
				Logger.LogError("CustomCode.Init() did not get the [" + ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS + "] value from attribute that is required for CustomCode tags at line number [" + m_lineNumber + "].");
				return false;
			}
		}


		return true;
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
			// Get the key for this custom code block.  The vast majority of the time, the key will have one or more tags embedded in it, so we must evaluate it to get the correct final value.
			StringWriter	t_keyWriter = new StringWriter();
			Cursor			t_keyCursor	= new Cursor(t_keyWriter);

			p_evaluationContext.PushNewCursor(t_keyCursor);

			if (!m_key.Evaluate(p_evaluationContext)) {
				Logger.LogError("CustomCode.Evaluate() failed to evaluate the custom code block key at line number [" + m_lineNumber + "].");
				return false;
			}

			p_evaluationContext.PopCurrentCursor();


			// Now begin with the opening comment line.
			Cursor t_writer				= p_evaluationContext.GetCursor();
			String t_leadingWhiteSpace	= t_writer.GetCurrentLineContents();		// Grab the current contents of the line because we want to use the same whitespace offset for the closing comment line below as is before this tag in the template so that the two comments line up at the same indention.

			t_writer.Write(m_openingCommentCharacters + "	" + CustomCodeManager.START_CUSTOM_CODE + ":" + t_keyWriter.toString());

			if (m_closingCommentCharacters != null)
				t_writer.Write("	" + m_closingCommentCharacters);

			t_writer.Write("\n");

			// Insert any custom code that may have been found when the existing file was scanned.
			String				t_key				= t_keyWriter.toString();
			CustomCodeManager	t_customCodeManager = p_evaluationContext.GetCustomCodeManager();
			if (t_customCodeManager.IsDuplicate(t_key)) {
				Logger.LogError("CustomCode.Evaluate() found a duplicate custom code block key [" + t_key + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_customCode = t_customCodeManager.GetCodeSegment(t_key);
			if (t_customCode != null)
				t_writer.Write(t_customCode);

			// Finish by adding the closing comment line.
			t_writer.Write(t_leadingWhiteSpace + m_openingCommentCharacters + "	" + CustomCodeManager.END_CUSTOM_CODE + ":" + t_keyWriter.toString());	// This doesn't have "\n" on the end because it's used in text tags and the text tag will keep the newline after the tag itself and add it here so we don't have to.

			if (m_closingCommentCharacters != null)
				t_writer.Write("	" + m_closingCommentCharacters);	// This doesn't have "\n" on the end because it's used in text tags and the text tag will keep the newline after the tag itself and add it here so we don't have to.
		}
		catch (Throwable t_error) {
			Logger.LogException("CustomCode.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name           :  " + m_name 				+ "\n");
		t_dump.append(p_tabs + "Comment Characters :  " + m_openingCommentCharacters	+ "\n");
		t_dump.append(p_tabs + "Key                :\n");

		t_dump.append(m_key.Dump(p_tabs + "\t"));

		return t_dump.toString();
	}
}

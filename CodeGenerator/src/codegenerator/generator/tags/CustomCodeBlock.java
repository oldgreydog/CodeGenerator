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
 * Generates a special pair of start and end comments between which the user can insert their custom
 * code so that on subsequent re-generations over existing files, the custom code will be saved and
 * re-inserted in the correct place in the newly generated version of the file.  An example would
 * look like this:
 *
 * <pre><code> // StartCustomCode:ExtraMembers
 * private String m_name;
 * // EndCustomCode:ExtraMembers</code></pre>
 *
 * <p>The original version of this tag required that you passed in the amount of whitespace that you
 * wanted inserted before the end-comment because it had no way of knowing where the start-tag was on
 * the current line.  I added the {@link codegenerator.generator.utils.Cursor} to fix that.  Now this
 * code can grab the whitespace in the current line before it adds the start-comment and then write
 * that whitespace on the next line before the end-comment.  That ensures that the two comments start
 * with the same, and correct, indentation in the output code.</p>
 *
 * <p>This is a sample of the tag that represents this block:</p>
 *
 * <pre><code>&lt;%customCode key=LoadAll&lt;%className%&gt;CacheCode commentCharacters=//%&gt;</code></pre>
 *
 * <p><b><code>key</code></b> - this is the unique name that will be generated for the start/end tags.</p>
 *
 * <p><b><code>commentCharacters</code></b> - this is the set of single-line comment characters that is used by whatever language is being generated.  In this example, it's "//" for java.</p>
 *
 * <p>The key is critical.  It has to be unique in the file.  If the code where this tag is used is
 * outside a <code>forEach</code> block, then you can optionally give it a key that is fixed, such
 * as <code>StaticMembers</code>.  However, if this tag is used inside a <code>forEach</code> block,
 * then you must define a key that uses a raw or type-converted config value to make it unique within
 * the context. The <code>LoadAll&lt;%className%&gt;CacheCode</code> used in the example above is a
 * perfect example.  The config variable <code>&lt;%className%&gt;</code> will cause a unique tag to
 * be generated at that location in every pass through the <code>forEach</code> block.</p>
 *
 * <p><b>If you don't make the key unique, you will loose custom code the next time you regenerate
 * on top of existing files!!</b></p>
 *
 * <p>To that end, if you are inside nested <code>forEach</code> blocks, then you will probably need
 * to add a config value for each context, using the parent reference caret (^) where necessary.  So,
 * for example, if you were inside a <code>forEach</code> block iterating over the "member" nodes under
 * an outer <code>forEach</code> block that's iterating over "class" nodes, then you might create a tag like:</p>
 *
 * <p><code>&lt;%customCode key=Validate&lt;%memberName%&gt;Of&lt;%^className%&gt; commentCharacters=//%&gt;</code></p>
 *
 * <p>The caret (^) in front of "className" tells {@link ConfigVariable} to go up one node before trying
 * to find the value named "className".  Since that parent node will be the one that the outer loop is
 * currently pointed at, you will get the correct value for the context.</p>
 */
public class CustomCodeBlock extends TemplateBlock_Base {

	static public final String		BLOCK_NAME									= "customCode";

	static public final String		ATTRIBUTE_KEY								= "key";
	static public final String		ATTRIBUTE_OPENING_COMMENT_CHARS				= "openingCommentCharacters";
	static public final String		ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS	= "optionalClosingCommentCharacters";


	// Data members
	protected	TemplateBlock_Base	m_key							= null;
	protected	String				m_openingCommentCharacters		= null;
	protected	String				m_closingCommentCharacters		= null;


	//*********************************
	public CustomCodeBlock() {
		super(BLOCK_NAME);
		m_isSafeForTextBlock = true;
	}


	//*********************************
	@Override
	public CustomCodeBlock GetInstance() {
		return new CustomCodeBlock();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("CustomCodeBlock.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_KEY);
		if (t_nodeAttribute == null) {
			Logger.LogError("CustomCodeBlock.Init() did not find the [" + ATTRIBUTE_KEY + "] attribute that is required for CustomCodeBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_key = t_nodeAttribute.GetAttributeValue();
		if (m_key == null) {
			Logger.LogError("CustomCodeBlock.Init() did not get the [" + ATTRIBUTE_KEY + "] string from attribute that is required for CustomCodeBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPENING_COMMENT_CHARS);
		if (t_nodeAttribute == null) {
			Logger.LogError("CustomCodeBlock.Init() did not find the [" + ATTRIBUTE_OPENING_COMMENT_CHARS + "] attribute that is required for CustomCodeBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_openingCommentCharacters = t_nodeAttribute.GetAttributeValueAsString();
		if (m_openingCommentCharacters == null) {
			Logger.LogError("CustomCodeBlock.Init() did not get the [" + ATTRIBUTE_OPENING_COMMENT_CHARS + "] value from attribute that is required for CustomCodeBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS);
		if (t_nodeAttribute != null) {
			m_closingCommentCharacters = t_nodeAttribute.GetAttributeValueAsString();
			if (m_closingCommentCharacters == null) {
				Logger.LogError("CustomCodeBlock.Init() did not get the [" + ATTRIBUTE_OPTIONAL_CLOSING_COMMENT_CHARS + "] value from attribute that is required for CustomCodeBlock tags at line number [" + m_lineNumber + "].");
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
			StringWriter	t_keyWriter = new StringWriter();
			Cursor			t_keyCursor	= new Cursor(t_keyWriter);

			p_evaluationContext.PushNewCursor(t_keyCursor);

			m_key.Evaluate(p_evaluationContext);

			p_evaluationContext.PopCurrentCursor();


			Cursor t_writer = p_evaluationContext.GetCursor();
			String t_leadingWhiteSpace = t_writer.GetCurrentLineContents();		// Grab the current contents of the line because we want to use the same whitespace offset for the closing comment line below as is before this tag in the template so that the two comments line up at the same indention.

			t_writer.Write(m_openingCommentCharacters + "	" + CustomCodeManager.START_CUSTOM_CODE + ":" + t_keyWriter.toString());

			if (m_closingCommentCharacters != null)
				t_writer.Write("	" + m_closingCommentCharacters);

			t_writer.Write("\n");

			String t_customCode = p_evaluationContext.GetCustomCodeManager().GetCodeSegment(t_keyWriter.toString());
			if (t_customCode != null)
				t_writer.Write(t_customCode);

			t_writer.Write(t_leadingWhiteSpace + m_openingCommentCharacters + "	" + CustomCodeManager.END_CUSTOM_CODE + ":" + t_keyWriter.toString());	// This doesn't have "\n" on the end because it's used in text blocks and the text block will keep the newline after the tag itself and add it here so we don't have to.

			if (m_closingCommentCharacters != null)
				t_writer.Write("	" + m_closingCommentCharacters);	// This doesn't have "\n" on the end because it's used in text blocks and the text block will keep the newline after the tag itself and add it here so we don't have to.
		}
		catch (Throwable t_error) {
			Logger.LogException("CustomCodeBlock.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name    :  " + m_name 				+ "\n");
		t_dump.append(p_tabs + "Comment Characters :  " + m_openingCommentCharacters	+ "\n");
		t_dump.append(p_tabs + "Key                :\n");

		t_dump.append(m_key.Dump(p_tabs + "\t"));

		return t_dump.toString();
	}
}

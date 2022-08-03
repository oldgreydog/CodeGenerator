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



import java.io.*;
import java.util.*;

import codegenerator.generator.utils.*;
import codegenerator.generator.utils.TemplateTokenizer.*;
import coreutil.logging.*;



/**
	<p>This is the only tag that can contain text that will be written to the output.</p>

	<pre>	<code>&lt;%text%&gt;
	...
	&lt;%endText%&gt;</code></pre>

	<p>Note that all text and white space from the end of the closing delimiter for the <code><B>text</B></code>
	tag and the beginning of the opening delimiter of the <code><B>endText</B></code> tag is significant.
	The only exception is any embedded tags.  They will be replaced by their evaluated text during file
	generation.  This means that you can indent the opening <code><B>text</B></code> tag as much as you like
	to make the template more readable, but text inside the tag will have to be indented in relation to
	the left edge because it will be output with the formatting as written, not in relation to where
	the <code><B>text</B></code> tag was placed.</p>

	<p>Text that is not in a <code><B>text</B></code> tag will be ignored by the parser.  This is an easy
	mistake to make because you will see your text is in the template but it may not be immediately
	obvious that it isn't inside a <code><B>text</B></code> tag and that's why it isn't getting written
	out to your files.  Of course, that also means that it is possible to comment your templates
	with any text that is outside of a text tag.  I've been using "<code>&lt;&lt;&lt;</code>" and
	"<code>&gt;&gt;&gt;</code>" as opening and closing delimiters for my template comments because they
	help the comments stand out, but you can do your comments any way you like.</p>

	<p>The <code><B>text</B></code> tag is not allowed to have any other tags inside it except for:</p>

	<pre>	a config value (such as <code><B>&lt;%className%&gt;</B></code>)
	<code><B>camelCase
	counter
	customCode
	firstLetterToLowerCase
	outerContextEval
	tabMarker
	tabStop
	typeConvert
	variable</B></code></pre>

	<p>Note that one of the defining characteristics of all of these tags is that they are single, free-standing tags.
	In other words, they don't have any intermediate or ending tags and, therefore, do not have child tags as content.
	Any other tag type will cause the parse to fail.  This is enforced by the code because every tag class marks itself
	as being safe for use in text or not.</p>
 */
public class Text extends Tag_Base {

	static public final String	TAG_NAME		= "text";
	static public final String	END_TAG_NAME	= "endText";

	// Data members
	private boolean		m_parsingTagElement		= false;
	private boolean		m_expectClosingQuotes	= false;
	private boolean		m_equalsIsAToken		= false;

	private String		m_text					= null;		// This will only have a value when this text tag doesn't itself contain any other tags.


	//*********************************
	public Text() {
		super("text");
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("Text.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new Text();
	}


	//*********************************
	public void SetText(String t_text) {
		m_text = t_text;
	}


	//*********************************
	public String GetText() {
		return m_text;
	}


	//*********************************
	public boolean IsEmpty() {
		if ((m_text == null) && ((m_tagList == null) || m_tagList.isEmpty()))
			return true;

		return false;
	}


	//*********************************
	public boolean ParseTagElement(TemplateTokenizer p_tokenizer, boolean p_equalsIsAToken) {
		m_parsingTagElement		= true;
		m_equalsIsAToken		= p_equalsIsAToken;

		return Parse(p_tokenizer);
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// A general block will parse child tags until it finds a tag that isn't a command.  That tag should be the closing tag for the parent tag.
			StringBuilder		t_collectedText		= new StringBuilder();
			Token				t_nextToken;
			TagParser			t_tagParser;
			Tag_Base	t_newTag;

			while ((t_nextToken = p_tokenizer.GetNextToken()) != null) {
				switch (t_nextToken.m_tokenType) {
					case Token.TOKEN_TYPE_OPENING_DELIMITER:
						t_tagParser = new TagParser();
						if (!t_tagParser.Parse(p_tokenizer)) {
							Logger.LogError("Text.Parse() failed to parse the tag at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
							return false;
						}

						// If the tag is the end tag for this block, then all we have to do it save any remaining text and return.
						if (t_tagParser.GetTagName().equalsIgnoreCase(END_TAG_NAME)) {
							SaveRemainingTextOnExit(t_collectedText);
							return true;
						}


						t_newTag = TagFactory.GetTag(t_tagParser.GetTagName());
						if (t_newTag == null) {
							// This should be variable tag embedded in the text.
							ConfigValue t_configValue = new ConfigValue();
							if (!t_configValue.Init(t_tagParser, p_tokenizer.GetLineCount())) {
								Logger.LogError("Text.Parse() failed to initialize the config variable at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
								return false;
							}

							SaveCollectedTextBeforeNewTag(t_collectedText);
							AddChildNode(t_configValue);
						}
						else {
							// Other than ConfigValues, these are the only tag types that can appear inside of a Text.  This forces you to keep text tags simpler which will keep templates simpler (hopefully).
							if (t_newTag.IsSafeForText())
							{
								if (!t_newTag.Init(t_tagParser)) {
									Logger.LogError("Text.Parse() failed to initialize the tag [" + t_newTag.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
									return false;
								}

								if (!t_newTag.Parse(p_tokenizer)) {
									Logger.LogError("Text.Parse() failed to parse the tag [" + t_newTag.GetName() + "] in the tag starting at [" + m_lineNumber + "].");
									return false;
								}

								SaveCollectedTextBeforeNewTag(t_collectedText);
								AddChildNode(t_newTag);
							}
							else {
								Logger.LogError("Text.Parse() found the tag [" + t_newTag.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] which is not allowed inside a text tag that started at [" + m_lineNumber + "].");
								return false;
							}
						}

						break;

					case Token.TOKEN_TYPE_CLOSING_DELIMITER:
						if (!m_parsingTagElement) {
							Logger.LogError("Text.Parse() is parsing a text tag and found an unexpected CLOSING_DELIMITER at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
							return false;
						}
						else if (m_expectClosingQuotes) {
							Logger.LogError("Text.Parse() is parsing a tag element and found an unexpected CLOSING_DELIMITER when it should found a DOUBLE_QUOTE at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
							return false;
						}

						// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
						SaveRemainingTextOnExit(t_collectedText);

						p_tokenizer.PushBackToken(t_nextToken);	// We are parsing a tag element so we need to push the closing delimiter back so that the tag parser can find it.
						return true;

					case Token.TOKEN_TYPE_EQUALS:
						if (m_equalsIsAToken) {
							// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
							SaveRemainingTextOnExit(t_collectedText);

							p_tokenizer.PushBackToken(t_nextToken);		// We have to push the equals back on the tokenizer so that the tag parser can find it.

							return true;
						}

						t_collectedText.append(t_nextToken.m_tokenValue);
						break;

					case Token.TOKEN_TYPE_WHITE_SPACE:
						if (m_parsingTagElement) {
							// If we are parsing a tag element and quotes are to be treated as the end of the text, then white spaces are part of the text and have to be added to the local text.
							if (m_expectClosingQuotes) {
								t_collectedText.append(t_nextToken.m_tokenValue);
								break;
							}

							// Otherwise, a space is the closing token for the text and we are done.
							// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
							SaveRemainingTextOnExit(t_collectedText);

							return true;
						}

						// Otherwise, white space is always part of the text block so we need to capture it.
						t_collectedText.append(t_nextToken.m_tokenValue);
						break;

					case Token.TOKEN_TYPE_WORD:
						t_collectedText.append(t_nextToken.m_tokenValue);	// We always have to capture words as part of the text block.
						break;

					case Token.TOKEN_TYPE_DOUBLE_QUOTE:
						if (m_parsingTagElement) {
							// If we are parsing a tag element and quotes are to be treated as the end of the text, then we are done.
							if (m_expectClosingQuotes) {
								// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
								SaveRemainingTextOnExit(t_collectedText);

								return true;
							}
							else {
								// If the quotes are the first token we've found, then we need to turn on the expectation that we will find closing quotes.
								if (((m_tagList == null) || m_tagList.isEmpty()) &&
									(t_collectedText.length() == 0))
								{
									m_expectClosingQuotes	= true;
									m_text					= "";		// And we need to set this to the empty string so that if that is what is also represented in the template then this will not evaluate to NULL.
									break;
								}
							}
						}

						// Otherwise, quotes are always part of the text block so we need to capture them.
						t_collectedText.append(t_nextToken.m_tokenValue);
						break;

					default:
						Logger.LogError("Text.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the attribute name at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
						return false;
				}
			}


			Logger.LogError("Text.Parse() appears to have hit the end of the file without finding the closing tag of the parent tag that started at [" + m_lineNumber + "].");
			return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("Text.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the text tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	private void SaveCollectedTextBeforeNewTag(StringBuilder p_collectedText) {
		// If there is parsed text that hasn't been saved, the we need to add it to m_tagList before we add the new tag that has been found.
		if (p_collectedText.length() != 0) {
			Text t_newText = new Text();
			t_newText.SetText(p_collectedText.toString());
			AddChildNode(t_newText);

			p_collectedText.setLength(0);	// Now that we've saved the local text, we need to reset it for any future text.
		}
	}


	//*********************************
	private void SaveRemainingTextOnExit(StringBuilder p_collectedText) {
		// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
		if (p_collectedText.length() != 0) {
			if ((m_tagList != null) && !m_tagList.isEmpty()) {	// If this block contained tags, then there will be objects in the m_tagList.  In that case, we need to create a new Text, put the local text into it and add it to the tag list.
				Text t_newText = new Text();
				t_newText.SetText(p_collectedText.toString());
				AddChildNode(t_newText);
			}
			else
				m_text = p_collectedText.toString();	// Otherwise, if what we just parsed is a strictly text-only string (there were no embedded tags), then we need to set the m_text member of this object with the parsed text.

			p_collectedText.setLength(0);	// Now that we've saved the local text, we need to reset it for any future text.
		}
	}


	//*********************************
	/**
	 * This will probably only ever be used where there are attribute values that contain variable tags that need to be evaluated.
	 * @param p_currentNode
	 * @param p_rootNode
	 * @param p_iterationCount
	 * @return
	 */
	public String EvaluateToString(EvaluationContext p_evaluationContext)
	{
		StringWriter	t_stringWriter	= new StringWriter();
		Cursor			t_stringCursor	= new Cursor(t_stringWriter);

		p_evaluationContext.PushNewCursor(t_stringCursor);

		if (!Evaluate(p_evaluationContext)) {
			Logger.LogError("Text.EvaluateToString() failed to evaluate.");
			p_evaluationContext.PopCurrentCursor();
			return null;
		}

		p_evaluationContext.PopCurrentCursor();

		return t_stringWriter.toString();
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			// If there are no child tags, then this is a "leaf" node text object and we have to output its string.
			if ((m_tagList == null) || m_tagList.isEmpty()) {
				if (m_text != null)
					p_evaluationContext.GetCursor().Write(m_text);

				return true;
			}

			// Otherwise, this is just a parent instance that contains the list of child text and text-safe tags that make up the whole "outer" text tag.
			for (Tag_Base t_nextTag: m_tagList) {
				if (!t_nextTag.Evaluate(p_evaluationContext)) {
					return false;
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("Text.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name 	+ "\n");

		if ((m_text != null))
			t_dump.append(p_tabs + "Text             :  " + m_text	+ "\n");

		if (m_tagList == null) {
			ListIterator<Tag_Base> t_tagIterator = m_tagList.listIterator();
			Tag_Base t_nextTag;
			while (t_tagIterator.hasNext()) {
				t_nextTag = t_tagIterator.next();

				t_dump.append("\n\n");
				t_dump.append(t_nextTag.Dump(p_tabs + "\t"));
			}
		}

		return t_dump.toString();
	}
}

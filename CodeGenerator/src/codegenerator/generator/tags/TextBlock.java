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



import java.io.*;
import java.util.*;

import codegenerator.generator.utils.*;
import codegenerator.generator.utils.TemplateTokenizer.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	This is the only tag that can contain text that will be written to the output.

	<pre><code>&lt;%text%&gt;
...
&lt;%endtext%&gt;</code></pre>

	Text that is not in a <code>text</code> tag should be ignored by the parser.  This is an easy
	mistake to make because you will see your text is in the template but it may not be immediately
	obvious that it isn't inside a <code>text</code> block and that's why it isn't getting written
	out to your files.

	<p>The <code>text</code> block is not allowed to have any other tags inside it except for:</p>

	<pre>- a config value (such as <code>&lt;%className%&gt;</code>)
- <code>customCode</code>
- <code>camelCase</code>
- <code>firstLetterToLowerCase</code>
- <code>tagMarker</code>
- <code>tagStop</code>
- <code>typeConvert</code></pre>

	<p>Any other tag type will cause the parse to fail.</p>

	<p>When I first created the code generator, I thought of the templates as being code with markup
	tags added in.  Unfortunately, this made parsing the templates way more complicated and it also
	made certain forms of code impossible to generate (such as a comma delimited sequence like ?,?,?,?
	that was driven by the number of columns in a table).</p>

	<p>Once I realized I could create an explicit <code>text</code> block and only output text that
	was inside those blocks, it made parsing and complex output much easier and more flexible.</p>
 */
public class TextBlock extends TemplateBlock_Base {

	static public final String	BLOCK_NAME		= "text";

	// Data members
	boolean		m_parsingTagElement		= false;
	boolean		m_expectClosingQuotes	= false;
	boolean		m_equalsIsAToken		= false;

	String		m_text					= null;		// This will only have a value when this text block doesn't itself contain any other tags.


	//*********************************
	public TextBlock() {
		super("text");
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("TextBlock.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new TextBlock();
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
		if ((m_text == null) && m_blockList.isEmpty())
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
			// A general block will parse child tags until it finds a tag that isn't a command.  That tag should be the closing tag for the parent block.
			StringBuilder		t_collectedText		= new StringBuilder();
			Token				t_nextToken;
			TagParser			t_tagParser;
			TemplateBlock_Base	t_newBlock;

			while ((t_nextToken = p_tokenizer.GetNextToken()) != null) {
				switch (t_nextToken.m_tokenType) {
					case Token.TOKEN_TYPE_OPENING_DELIMITER:
						t_tagParser = new TagParser();
						if (!t_tagParser.Parse(p_tokenizer)) {
							Logger.LogError("TextBlock.Parse() failed to parse the tag at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
							return false;
						}

						// If the tag is the end tag for this block, then all we have to do it save any remaining text and return.
						if (t_tagParser.GetTagName().equalsIgnoreCase("endtext")) {
							SaveRemainingTextOnExit(t_collectedText);
							return true;
						}


						t_newBlock = BlockFactory.GetBlock(t_tagParser.GetTagName());
						if (t_newBlock == null) {
							// This should be variable tag embedded in the text.
							ConfigVariable t_configVariable = new ConfigVariable();
							if (!t_configVariable.Init(t_tagParser, p_tokenizer.GetLineCount())) {
								Logger.LogError("TextBlock.Parse() failed to initialize the config variable at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
								return false;
							}

							SaveCollectedTextBeforeNewTag(t_collectedText);
							m_blockList.add(t_configVariable);
						}
						else {
							// Other than ConfigVariables, these are the only tag types that can appear inside of a TextBlock.  This forces you to keep text blocks simpler which will keep templates simpler (hopefully).
							if (t_newBlock.IsSafeForTextBlock())
							{
								if (!t_newBlock.Init(t_tagParser)) {
									Logger.LogError("TextBlock.Parse() failed to initialize the block [" + t_newBlock.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
									return false;
								}

								if (!t_newBlock.Parse(p_tokenizer)) {
									Logger.LogError("TextBlock.Parse() failed to parse the tag [" + t_newBlock.GetName() + "] in the block starting at [" + m_lineNumber + "].");
									return false;
								}

								SaveCollectedTextBeforeNewTag(t_collectedText);
								m_blockList.add(t_newBlock);
							}
							else {
								Logger.LogError("TextBlock.Parse() found the tag [" + t_newBlock.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] which is not allowed inside a text block that started at [" + m_lineNumber + "].");
								return false;
							}
						}

						break;

					case Token.TOKEN_TYPE_CLOSING_DELIMITER:
						if (!m_parsingTagElement) {
							Logger.LogError("TextBlock.Parse() is parsing a text block and found an unexpected CLOSING_DELIMITER at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
							return false;
						}
						else if (m_expectClosingQuotes) {
							Logger.LogError("TextBlock.Parse() is parsing a tag element and found an unexpected CLOSING_DELIMITER when it should found a DOUBLE_QUOTE at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
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
								if (m_blockList.isEmpty() && (t_collectedText.length() == 0)) {
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
						Logger.LogError("TextBlock.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the attribute name at line [" + p_tokenizer.GetLineCount() + "] in the block starting at [" + m_lineNumber + "].");
						return false;
				}
			}


			Logger.LogError("TextBlock.Parse() appears to have hit the end of the file without finding the closing tag of the parent block that started at [" + m_lineNumber + "].");
			return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("TextBlock.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the text block starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	private void SaveCollectedTextBeforeNewTag(StringBuilder p_collectedText) {
		// If there is parsed text that hasn't been saved, the we need to add it to m_blockList before we add the new tag block that has been found.
		if (p_collectedText.length() != 0) {
			TextBlock t_newText = new TextBlock();
			t_newText.SetText(p_collectedText.toString());
			m_blockList.add(t_newText);

			p_collectedText.setLength(0);	// Now that we've saved the local text, we need to reset it for any future text.
		}
	}


	//*********************************
	private void SaveRemainingTextOnExit(StringBuilder p_collectedText) {
		// If there is parsed text that hasn't been saved, the we need to figure out where to put it.
		if (p_collectedText.length() != 0) {
			if (!m_blockList.isEmpty()) {	// If this block contained tags, then there will be objects in the m_blockList.  In that case, we need to create a new TextBlock, put the local text into it and add it to the block list.
				TextBlock t_newText = new TextBlock();
				t_newText.SetText(p_collectedText.toString());
				m_blockList.add(t_newText);
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
	public String EvaluateToString(ConfigNode		p_currentNode,
								   ConfigNode		p_rootNode,
								   LoopCounter		p_iterationCounter)
	{
		StringWriter	t_stringWriter	= new StringWriter();
		Cursor			t_stringCursor	= new Cursor(t_stringWriter);
		if (!Evaluate(p_currentNode, p_rootNode, t_stringCursor, p_iterationCounter)) {
			Logger.LogError("TextBlock.EvaluateToString() failed to evaluate.");
			return null;
		}

		return t_stringWriter.toString();
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)	// There wasn't any good way to tell a TextBlock which iteration it was in that would be safe for arbitrary nesting, so I added this iteration counter to handle the problem.
	{
		try {
			// If there are no child blocks, then this is a "leaf" node text object and we have to output its string.
			if (m_blockList.isEmpty()) {
				if (m_text != null)
					p_writer.Write(m_text);

				return true;
			}

			// Otherwise, this is just a parent instance that contains the list of child text/variable blocks that make up the whole "outer" text block.
			for (TemplateBlock_Base t_nextBlock: m_blockList) {
				if (!t_nextBlock.Evaluate(p_currentNode, p_rootNode, p_writer, p_iterationCounter)) {
					return false;
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("TextBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name  :  " + m_name 	+ "\n");

		if ((m_text != null))
			t_dump.append(p_tabs + "Text             :  " + m_text	+ "\n");

		ListIterator<TemplateBlock_Base> t_blockIterator = m_blockList.listIterator();
		TemplateBlock_Base t_nextBlock;
		while (t_blockIterator.hasNext()) {
			t_nextBlock = t_blockIterator.next();

			t_dump.append("\n\n");
			t_dump.append(t_nextBlock.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}
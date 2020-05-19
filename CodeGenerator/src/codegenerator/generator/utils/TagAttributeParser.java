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


package codegenerator.generator.utils;



import coreutil.logging.*;

import codegenerator.generator.tags.*;
import codegenerator.generator.utils.TemplateTokenizer.*;



/**
	This class expects to parse attributes of the forms:
	<p>name=value</p>
	<p>name="value with white spaces bound by double quotes"</p>
	<p>It will stop parsing when it finds the closing delimiter for the tag. It will accept those forms with white space between any or all of the elements and between the value and the closing delimiter.</p>
 */
public class TagAttributeParser {

	// Data members
	private TemplateBlock_Base		m_attributeName;
	private TemplateBlock_Base		m_value;
	private int						m_lineNumber		= -1;


	//*********************************
	public TemplateBlock_Base GetAttributeName() {
		return m_attributeName;
	}


	//*********************************
	/**
	 * Converting the parser to have more flexible name and value typing of TemplateBlock_Base instead of TextBlock lead to
	 * adding this helper function that will let most classes that only need plain text attributes to get those values without
	 * having to do the conversions themselves.  Of course, that means that if the either of the attributes values is changed to
	 * be a more complex type, then they have to handle a NULL return from this and then check to see if a more complex type is available.
	 *
	 * @return Can be NULL.
	 */
	public String GetAttributeNameAsString() {
		try {
			if ((m_attributeName != null) && (m_attributeName.GetName() == TextBlock.BLOCK_NAME))
				return  ((TextBlock)m_attributeName).GetText();

			return null;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagAttributeParser.GetAttributeValueAsString() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public TemplateBlock_Base GetAttributeValue() {
		return m_value;
	}


	//*********************************
	/**
	 * Converting the parser to have more flexible name and value typing of TemplateBlock_Base instead of TextBlock lead to
	 * adding this helper function that will let most classes that only need plain text attributes to get those values without
	 * having to do the conversions themselves.  Of course, that means that if the either of the attributes values is changed to
	 * be a more complex type, then they have to handle a NULL return from this and then check to see if a more complex type is available.
	 *
	 * @return Can be NULL if the object type is not TextBlock.
	 */
	public String GetAttributeValueAsString() {
		try {
			if ((m_value != null) && (m_value.GetName() == TextBlock.BLOCK_NAME))
				return  ((TextBlock)m_value).GetText();

			return null;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagAttributeParser.GetAttributeValueAsString() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public int GetLineNumber() {
		return m_lineNumber;
	}


	//*********************************
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			m_lineNumber = p_tokenizer.GetLineCount();

			// A general block will parse child tags until it finds a tag that isn't a command.  That tag should be the closing tag for the parent block.
			Token				t_nextToken;
			TagParser			t_tagParser;
			TemplateBlock_Base	t_newBlock;
			TextBlock			t_textBlock;

			while ((t_nextToken = p_tokenizer.GetNextToken()) != null) {
				switch (t_nextToken.m_tokenType) {
					case Token.TOKEN_TYPE_OPENING_DELIMITER:
						t_tagParser = new TagParser();
						if (!t_tagParser.Parse(p_tokenizer)) {
							Logger.LogError("TextBlock.Parse() failed to parse the tag at line [" + p_tokenizer.GetLineCount() + "].");
							return false;
						}

						t_newBlock = BlockFactory.GetBlock(t_tagParser.GetTagName());
						if (t_newBlock == null) {
							// This should be variable tag embedded in the text.
							ConfigVariable t_configVariable = new ConfigVariable();
							if (!t_configVariable.Init(t_tagParser, p_tokenizer.GetLineCount())) {
								Logger.LogError("TagAttributeParser.Parse() failed to initialize the config variable at line [" + p_tokenizer.GetLineCount() + "].");
								return false;
							}

							if (m_attributeName == null)
								m_attributeName = t_configVariable;
							else {
								m_value = t_configVariable;
								return true;
							}

							break;	// We should get an "=" next.
						}
						else {
							// Other than ConfigVariables, these are the only tag types that can appear inside of a TextBlock.  This forces you to keep text blocks simpler which will keep templates simpler (hopefully).
							if (t_newBlock.IsSafeForTextBlock())
							{
								if (!t_newBlock.Init(t_tagParser)) {
									Logger.LogError("TagAttributeParser.Parse() failed to initialize the block [" + t_newBlock.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "].");
									return false;
								}

								if (!t_newBlock.Parse(p_tokenizer)) {
									Logger.LogError("TagAttributeParser.Parse() failed to parse the tag [" + t_newBlock.GetName() + "].");
									return false;
								}

								if (m_attributeName == null)
									m_attributeName = t_newBlock;
								else {
									m_value = t_newBlock;
									return true;
								}
							}
							else {
								Logger.LogError("TagAttributeParser.Parse() found the tag [" + t_newBlock.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] which is not allowed inside a text block.");
								return false;
							}
						}

						break;

					case Token.TOKEN_TYPE_CLOSING_DELIMITER:
						p_tokenizer.PushBackToken(t_nextToken);	// We are parsing a tag element so we need to push the closing delimiter back so that the tag parser can find it.

						// If either of the name or value members didn't get initialized, then we've run off of the end of the attributes and found the closing delimiter for the tag and we need to return false.
						if ((m_attributeName == null) ||
							(m_value == null))
							return false;

						return true;

					case Token.TOKEN_TYPE_EQUALS:
						break;

					case Token.TOKEN_TYPE_WHITE_SPACE:
						break;

					case Token.TOKEN_TYPE_WORD:
						// I think that if we get this outside of tag delimiters, then it's safe to assume that we are getting a single-word constant value and we need to wrap it in a TextBlock and move on.
						t_textBlock = new TextBlock();
						t_textBlock.SetText(t_nextToken.m_tokenValue);

						if (m_attributeName == null)
							m_attributeName = t_textBlock;
						else {
							m_value = t_textBlock;
							return true;
						}

						break;

					case Token.TOKEN_TYPE_DOUBLE_QUOTE:
						// We'll use the TextBlock to parse this string constant.
						p_tokenizer.PushBackToken(t_nextToken);

						t_textBlock = new TextBlock();
						t_textBlock.ParseTagElement(p_tokenizer, true);

						if (m_attributeName == null)
							m_attributeName = t_textBlock;
						else {
							m_value = t_textBlock;
							return true;
						}

						break;

					default:
						Logger.LogError("TagAttributeParser.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the attribute name at line [" + p_tokenizer.GetLineCount() + "].");
						return false;
				}
			}


			Logger.LogError("TagAttributeParser.Parse() appears to have hit the end of the file without finding the closing tag of the parent block.");
			return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagAttributeParser.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}
}
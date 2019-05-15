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
	private TextBlock		m_attributeName;
	private boolean			m_attributeNameIsVariable		= false;
	private TextBlock		m_value;


	//*********************************
	public TextBlock GetAttributeName() {
		return m_attributeName;
	}


	//*********************************
	public boolean IsAttributeNameAVariable() {
		return m_attributeNameIsVariable;
	}


	//*********************************
	public TextBlock GetValue() {
		return m_value;
	}


	//*********************************
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// EatWhiteSpace() will be used as needed because the white space in a tag is not significant (i.e. required) in its results.
			p_tokenizer.EatWhiteSpace();

			// Get the left side of the attribute.  This can be any type of tag that evaluates to a string.
			TextBlock t_textBlock = new TextBlock();
			if (!t_textBlock.ParseTagElement(p_tokenizer, true)) {
				Logger.LogError("TagAttributeParser.Parse() failed to parse the attribute name at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			// If the text block is empty, then there are no remaining attributes to parse.
			if (t_textBlock.IsEmpty())
				return false;

			m_attributeName = t_textBlock;

			// Get the equals token.
			Token t_nextToken = p_tokenizer.GetNextToken();
			if (t_nextToken == null) {
				Logger.LogError("TagAttributeParser.Parse() failed to get the EQUALS from the tokenizer at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			if (t_nextToken.m_tokenType != Token.TOKEN_TYPE_EQUALS) {
				Logger.LogError("TagAttributeParser.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting an EQUALS for the attribute at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			// EatWhiteSpace() will be used as needed because the white space in a tag is not significant (i.e. required) in its results.
			p_tokenizer.EatWhiteSpace();

			// Get the attribute value.
			t_textBlock = new TextBlock();
			if (!t_textBlock.ParseTagElement(p_tokenizer, false)) {
				Logger.LogError("TagAttributeParser.Parse() failed to parse the attribute value at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			m_value = t_textBlock;

			// Eat any white space that might be between the value and the closing delimiter or then next attribute.
			p_tokenizer.EatWhiteSpace();


			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("TagAttributeParser.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}
}

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


package codegenerator.generator.utils;



import java.util.*;

import coreutil.logging.*;
import codegenerator.generator.utils.TemplateTokenizer.*;



public class TagParser {

	// Data members
	private String									m_tagName;
	private TreeMap<String, TagAttributeParser>		m_namedAttributes	= new TreeMap<>();	// This gives us fast access to simple named attributes that a tag uses for initializing known attributes that is uses.
	private Vector<TagAttributeParser>				m_tagAttributes		= new Vector<>();	// This holds all of the attributes, including those that have complex, "mixed" attribute names (i.e. those that include one or more tags in their values).
	private int										m_lineNumber		= -1;


	//*********************************
	public String GetTagName() {
		return m_tagName;
	}


	//*********************************
	public TagAttributeParser GetNamedAttribute(String p_attributeName) {
		return m_namedAttributes.get(p_attributeName.toLowerCase());
	}


	//*********************************
	public Vector<TagAttributeParser> GetTagAttributes() {
		return m_tagAttributes;
	}


	//*********************************
	public int GetLineNumber() {
		return m_lineNumber;
	}


	//*********************************
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			m_lineNumber = p_tokenizer.GetLineCount();

//			if (p_tokenizer.GetLineCount() == 235)
//				Logger.LogVerbose("Pause");

//			if (p_tokenizer.GetCurrentLine().contains("<%if <%and exists=\"isNullable\""))
//				Logger.LogVerbose("Pause");


			// EatWhiteSpace() will be used as needed because the white space in a tag is not significant (i.e. required) in its results.
			p_tokenizer.EatWhiteSpace();

			// Get the tag name token.
			Token t_nextToken = p_tokenizer.GetNextToken();
			if (t_nextToken == null) {
				Logger.LogError("TagParser.Parse() failed to get the tag name from the tokenizer at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			if (t_nextToken.m_tokenType != Token.TOKEN_TYPE_WORD) {
				Logger.LogError("TagParser.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the tag name at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			m_tagName = t_nextToken.m_tokenValue;

//			if (m_tagName.equals(TypeConvert.TAG_NAME))
//				Logger.LogVerbose("Pause");


			// Use TagAttributeParser to find any and all attributes that might be on this tag.
			TagAttributeParser	t_nextAttribute		= new TagAttributeParser();
			String				t_attributeName;
			while (t_nextAttribute.Parse(p_tokenizer)) {
				// If GetAttributeNameAsString() returns NULL, then it contains a "complex" value that includes one or more tags and can therefore only be Evaluate()'d so it can't be put in the attribute map.
				t_attributeName = t_nextAttribute.GetAttributeNameAsString();
				if (t_attributeName != null)
					m_namedAttributes.put(t_attributeName.toLowerCase(), t_nextAttribute);

				m_tagAttributes.add(t_nextAttribute);

				// The last thing to do should be to eat the closing delimiter.
				t_nextToken = p_tokenizer.GetNextToken();
				if (t_nextToken == null) {
					Logger.LogError("TagParser.Parse() failed to get the closing delimiter for the tag [" + m_tagName + "] at line [" + p_tokenizer.GetLineCount() + "].");
					return false;
				}

				if (t_nextToken.m_tokenType == Token.TOKEN_TYPE_CLOSING_DELIMITER)
					return true;

				p_tokenizer.PushBackToken(t_nextToken);	// We didn't find the closing delimiter for the tag so we must have another attribute and we need to go around again.

				t_nextAttribute = new TagAttributeParser();
			}


			// The last thing to do should be to eat the closing delimiter.
			t_nextToken = p_tokenizer.GetNextToken();
			if (t_nextToken == null) {
				Logger.LogError("TagParser.Parse() failed to get the closing delimiter for the tag [" + m_tagName + "] at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			if (t_nextToken.m_tokenType != Token.TOKEN_TYPE_CLOSING_DELIMITER) {
				Logger.LogError("TagParser.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a CLOSING_DELIMITER for the tag name at line [" + p_tokenizer.GetLineCount() + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagParser.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}

}

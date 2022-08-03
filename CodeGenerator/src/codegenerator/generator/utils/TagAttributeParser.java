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



import coreutil.logging.*;

import java.util.*;

import codegenerator.generator.tags.*;
import codegenerator.generator.utils.TemplateTokenizer.*;



/**
	<p>This class parses a single attribute name/value pair.  The {@link TagParser} uses new instances to parse all of a
	tag's attributes until it finds the closing delimiter for tag.  Here is a compressed list of examples of valid attributes:</p>

	<pre>	<code>name=value
	name = value
	name = "value with white spaces bound by double quotes"
	nameWith&lt;%embeddedTags%&gt; = valueWith&lt;%embeddedTags%&gt;</code></pre>

	<p>This list is "compressed" in that it tries to show examples of all of the rules below without showing every possible combination.
	Here are the general rules for what the parser accepts for valid attributes:</p>

	<pre>	- white space before or after all elements: attribute name, the equals, the value
	- or no white space between the elements
	- embedded tags in the name and/or the value
	- double quotes around the name and/or the value.  This makes it possible to have white space embedded in either one.
		- NOTE: The double quotes are only required for names/values that include white space.  They are optional in all other cases.</pre>

	<p>Here is the current list of allowed embedded tags:</p>

	<pre>	<code><B>camelCase
	&lt;%DataType%&gt; </B>(i.e. config values)<B>
	counter
	firstLetterToLowerCase
	and/or/not	</B>(i.e. for if statements)<B>
	outerContextEval
	typeConvert
	variable</B></code></pre>
 */
public class TagAttributeParser {

	// Data members
	private enum ParseState {START_NAME,
							 IN_NAME,
							 EXPECT_EQUALS,
							 START_VALUE,
							 IN_VALUE
							 }

	private GeneralBlock		m_attributeName;
	private GeneralBlock		m_value;
	private int					m_lineNumber		= -1;
	private boolean				m_failedWithError	= false;


	//*********************************
	public GeneralBlock GetAttributeName() {
		return m_attributeName;
	}


	//*********************************
	/**
	 * Converting the parser to have more flexible name and value typing of GeneralBlock instead of Text lead to
	 * adding this helper function that will let most classes that only need plain text attributes to get those values without
	 * having to do the conversions themselves.  Of course, that means that if the either of the attributes values is changed to
	 * be a more complex type, then they have to handle a NULL return from this and then check to see if a more complex type is available.
	 *
	 * @return Can be NULL if the value is missing or if the value contains tags that can only have a value during the evaluation phase of execution.
	 */
	public String GetAttributeNameAsString() {
		try {
			if (m_attributeName == null)
				return null;

			LinkedList<Tag_Base> t_attributeTags = m_attributeName.GetChildNodeList();
			if ((t_attributeTags == null) || (t_attributeTags.isEmpty() || (t_attributeTags.size() > 1)))
				return null;

			Tag_Base t_onlyTag = t_attributeTags.getFirst();
			if (t_onlyTag.GetName() == Text.TAG_NAME)
				return  ((Text)t_onlyTag).GetText();

			return null;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagAttributeParser.GetAttributeValueAsString() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public GeneralBlock GetAttributeValue() {
		return m_value;
	}


	//*********************************
	/**
	 * Converting the parser to have more flexible name and value typing of GeneralBlock instead of Text lead to
	 * adding this helper function that will let most classes that only need plain text attributes to get those values without
	 * having to do the conversions themselves.  Of course, that means that if the either of the attributes values is changed to
	 * be a more complex type, then they have to handle a NULL return from this and then check to see if a more complex type is available.
	 *
	 * @return Can be NULL if the value is missing or if the value contains tags that can only have a value during the evaluation phase of execution.
	 */
	public String GetAttributeValueAsString() {
		try {
			if (m_value == null)
				return null;

			LinkedList<Tag_Base> t_attributeTags = m_value.GetChildNodeList();
			if ((t_attributeTags == null) || (t_attributeTags.isEmpty() || (t_attributeTags.size() > 1)))
				return null;

			Tag_Base t_onlyTag = t_attributeTags.getFirst();
			if (t_onlyTag.GetName() == Text.TAG_NAME)
				return  ((Text)t_onlyTag).GetText();

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
	public boolean FailedWithError() {
		return m_failedWithError;
	}


	//*********************************
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			m_lineNumber = p_tokenizer.GetLineCount();

			Token		t_nextToken;
			TagParser	t_tagParser;
			Tag_Base	t_newTag;
			Text		t_text;
			boolean		t_expectDoubleQuote		= false;
			ParseState	t_parseState			= ParseState.START_NAME;

			while ((t_nextToken = p_tokenizer.GetNextToken()) != null) {
				switch (t_nextToken.m_tokenType) {
					case Token.TOKEN_TYPE_OPENING_DELIMITER:
						t_tagParser = new TagParser();
						if (!t_tagParser.Parse(p_tokenizer)) {
							Logger.LogError("TagAttributeParser.Parse() failed to parse the tag at line [" + p_tokenizer.GetLineCount() + "].");
							return false;
						}

						t_newTag = TagFactory.GetTag(t_tagParser.GetTagName());
						if (t_newTag == null) {
							// This should be a variable tag embedded in the text.
							ConfigValue t_configValue = new ConfigValue();
							if (!t_configValue.Init(t_tagParser, p_tokenizer.GetLineCount())) {
								Logger.LogError("TagAttributeParser.Parse() failed to initialize the config variable at line [" + p_tokenizer.GetLineCount() + "].");
								m_failedWithError = true;
								return false;
							}

							switch (t_parseState) {
								case START_NAME:
									t_parseState = ParseState.IN_NAME;	// Set the new state and fall through to the next case to add the tag to the child node list.
								case IN_NAME:
									if (m_attributeName == null)
										m_attributeName = new GeneralBlock();

									m_attributeName.AddChildNode(t_configValue);
									break;

								case EXPECT_EQUALS:
									Logger.LogError("TagAttributeParser.Parse() is expecting the equals sign but found config variable at line [" + p_tokenizer.GetLineCount() + "].");
									m_failedWithError = true;
									return false;

								case START_VALUE:
									t_parseState = ParseState.IN_VALUE;	// Set the new state and fall through to the next case to add the tag to the child node list.
								case IN_VALUE:
									if (m_value == null)
										m_value = new GeneralBlock();

									m_value.AddChildNode(t_configValue);
									break;
							}

							break;
						}
						else {
							// Other than ConfigValues, these are the only tag types that can appear inside of a Text tag.  This forces you to keep text blocks simpler which will keep templates simpler (hopefully).
							if (t_newTag.IsSafeForAttributes())
							{
								if (!t_newTag.Init(t_tagParser)) {
									Logger.LogError("TagAttributeParser.Parse() failed to initialize the tag [" + t_newTag.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "].");
									m_failedWithError = true;
									return false;
								}

								if (!t_newTag.Parse(p_tokenizer)) {
									Logger.LogError("TagAttributeParser.Parse() failed to parse the tag [" + t_newTag.GetName() + "].");
									m_failedWithError = true;
									return false;
								}

								switch (t_parseState) {
									case START_NAME:
										t_parseState = ParseState.IN_NAME;	// Set the new state and fall through to the next case to add the tag to the child node list.
									case IN_NAME:
										if (m_attributeName == null)
											m_attributeName = new GeneralBlock();

										m_attributeName.AddChildNode(t_newTag);
										break;

									case EXPECT_EQUALS:
										Logger.LogError("TagAttributeParser.Parse() is expecting the tag [" + t_newTag.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "].");
										m_failedWithError = true;
										return false;

									case START_VALUE:
										t_parseState = ParseState.IN_VALUE;	// Set the new state and fall through to the next case to add the tag to the child node list.
									case IN_VALUE:
										if (m_value == null)
											m_value = new GeneralBlock();

										m_value.AddChildNode(t_newTag);
										break;
								}
							}
							else {
								Logger.LogError("TagAttributeParser.Parse() found the tag [" + t_newTag.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "] which is not allowed inside a text tag.");
								m_failedWithError = true;
								return false;
							}
						}

						break;

					case Token.TOKEN_TYPE_CLOSING_DELIMITER:
						p_tokenizer.PushBackToken(t_nextToken);	// We are parsing a tag element so we need to push the closing delimiter back so that the tag parser can find it.

						// If either of the name or value members didn't get initialized, then we've run off of the end of the attributes and found the closing delimiter for the tag and we need to return false.
						if ((m_attributeName == null) ||
							(m_value == null))
							return false;	// This is a valid FALSE since we have run out of attributes to parse and this is how that is signaled to the calling code.

						return true;

					case Token.TOKEN_TYPE_EQUALS:
						switch (t_parseState) {
							case START_NAME:
								Logger.LogError("TagAttributeParser.Parse() found an equals sign at line [" + p_tokenizer.GetLineCount() + "] but it was expecting to find the attribute name.");
								m_failedWithError = true;
								return false;

							case IN_NAME:
							case EXPECT_EQUALS:
								t_parseState = ParseState.START_VALUE;
								break;

							case START_VALUE:
							case IN_VALUE:
								Logger.LogError("TagAttributeParser.Parse() found an equals sign at line [" + p_tokenizer.GetLineCount() + "] but it was expecting to find the attribute value.");
								m_failedWithError = true;
								return false;
						}

						break;

					case Token.TOKEN_TYPE_WHITE_SPACE:

						switch (t_parseState) {
							case START_NAME:
								break;	// Just eat any white space before the name.
							case IN_NAME:
								// If we are inside double quotes, then we need to keep the white space, not ignore it.
								if (t_expectDoubleQuote) {
									t_text = new Text();
									t_text.SetText(t_nextToken.m_tokenValue);

									if (m_attributeName == null)
										m_attributeName = new GeneralBlock();

									m_attributeName.AddChildNode(t_text);
								}
								else {	// Otherwise, we're done with the attribute name and we need to look for the equals.
									t_parseState = ParseState.EXPECT_EQUALS;
								}

								break;

							case EXPECT_EQUALS:
								break;	// We can eat any white space between the name and the equals sign.

							case START_VALUE:
								break;	// Just eat any white space between the equals and the value.
							case IN_VALUE:
								// If we are inside double quotes, then we need to keep the white space, not ignore it.
								if (t_expectDoubleQuote) {
									t_text = new Text();
									t_text.SetText(t_nextToken.m_tokenValue);

									if (m_value == null)
										m_value = new GeneralBlock();

									m_value.AddChildNode(t_text);
								}
								else {	// Otherwise, we're done with the attribute name and we need to look for the equals.
									return true;	// We're done with the attribute value and, therefore, the attribute, so it's time to return.
								}
						}

						break;

					case Token.TOKEN_TYPE_WORD:
						// I think that if we get this outside of tag delimiters, then it's safe to assume that we are getting a single-word constant value and we need to wrap it in a Text object and move on.
						t_text = new Text();
						t_text.SetText(t_nextToken.m_tokenValue);

						switch (t_parseState) {
							case START_NAME:
								t_parseState = ParseState.IN_NAME;
							case IN_NAME:
								if (m_attributeName == null)
									m_attributeName = new GeneralBlock();

								m_attributeName.AddChildNode(t_text);

								// We can't change the state to EXPECT_EQUALS here because this may be a mixed value (i.e. text + tags).
								break;

							case EXPECT_EQUALS:
								break;	// We can eat any white space between the name and the equals sign.

							case START_VALUE:
								t_parseState = ParseState.IN_VALUE;
							case IN_VALUE:
								if (m_value == null)
									m_value = new GeneralBlock();

								m_value.AddChildNode(t_text);

								// We can't return here because this may be a mixed value (i.e. text + tags).
								break;
						}

						break;

					case Token.TOKEN_TYPE_DOUBLE_QUOTE:
						switch (t_parseState) {
							case START_NAME:
								if (m_attributeName == null)
									m_attributeName = new GeneralBlock();

								t_expectDoubleQuote	= true;
								t_parseState		= ParseState.IN_NAME;
								break;

							case IN_NAME:
								if (!t_expectDoubleQuote) {
									Logger.LogError("TagAttributeParser.Parse() found an unexpected double-quote in the attribute name that was not preceded by an matching opening double-quote at line [" + p_tokenizer.GetLineCount() + "] but .");
									m_failedWithError = true;
									return false;
								}

								t_expectDoubleQuote	= false;					// Now that we've found the closing quote, we need to clear this flag in case we need it for the value parsing.
								t_parseState		= ParseState.EXPECT_EQUALS;	// Since this was a double-quoted value, we are done with the name and now expect the equals sign.
								break;

							case EXPECT_EQUALS:
								Logger.LogError("TagAttributeParser.Parse() is expecting the equals sign at line [" + p_tokenizer.GetLineCount() + "] but found an unexpected double-quote.");
								m_failedWithError = true;
								return false;

							case START_VALUE:
								if (m_value == null)
									m_value = new GeneralBlock();

								t_expectDoubleQuote	= true;
								t_parseState = ParseState.IN_VALUE;
								break;


							case IN_VALUE:
								if (!t_expectDoubleQuote) {
									Logger.LogError("TagAttributeParser.Parse() found an unexpected double-quote in the attribute value that was not preceded by an matching opening double-quote at line [" + p_tokenizer.GetLineCount() + "] but .");
									m_failedWithError = true;
									return false;
								}

								return true;	// Since this was a double-quoted value, we are done with the attribute value and can return.
						}

						break;

					default:
						Logger.LogError("TagAttributeParser.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the attribute name at line [" + p_tokenizer.GetLineCount() + "].");
						m_failedWithError = true;
						return false;
				}
			}


			Logger.LogError("TagAttributeParser.Parse() appears to have hit the end of the file without finding the closing tag of the parent tag.");
			m_failedWithError = true;
			return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("TagAttributeParser.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			m_failedWithError = true;
			return false;
		}
	}
}

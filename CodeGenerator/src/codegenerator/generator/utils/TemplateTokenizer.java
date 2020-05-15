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



import java.io.*;
import java.util.*;

import coreutil.logging.*;



public class TemplateTokenizer {

	static public class Token {
		static public final int		TOKEN_TYPE_NOT_TOKEN			= -1;
		static public final int		TOKEN_TYPE_OPENING_DELIMITER	= 0;
		static public final int		TOKEN_TYPE_CLOSING_DELIMITER	= 1;
		static public final int		TOKEN_TYPE_EQUALS				= 2;
		static public final int		TOKEN_TYPE_WHITE_SPACE			= 3;
		static public final int		TOKEN_TYPE_WORD					= 4;
		static public final int		TOKEN_TYPE_DOUBLE_QUOTE			= 5;

		public	int		m_tokenType;
		public	String	m_tokenValue;

		public Token(int p_type, String p_value) {
			m_tokenType = p_type;
			m_tokenValue = p_value;
		}

		public String GetTokenTypeName() {
			switch(m_tokenType) {
				case TOKEN_TYPE_OPENING_DELIMITER:
					return "Opening delim";
				case TOKEN_TYPE_CLOSING_DELIMITER:
					return "Closing delim";
				case TOKEN_TYPE_EQUALS:
					return "Equals       ";
				case TOKEN_TYPE_WHITE_SPACE:
					return "White space  ";
				case TOKEN_TYPE_WORD:
					return "Word         ";
				case TOKEN_TYPE_DOUBLE_QUOTE:
					return "Double quote ";
			};

			return "Not defined";
		}
	}

	static private class SpecialSymbol {
		private String	m_symbol;
		private int		m_tokenType;

		public SpecialSymbol(String p_symbol, int p_tokenType) {
			m_symbol	= p_symbol;
			m_tokenType	= p_tokenType;
		}

		public int GetSymbolLength() {
			return m_symbol.length();
		}

		public String GetSymbol() {
			return m_symbol;
		}

		public int GetTokenType() {
			return m_tokenType;
		}
	}

    protected	File 						m_sourceTempateFile;
    protected	BufferedReader				m_fileReader;
	protected	LinkedList<SpecialSymbol>	m_symbolList		= new LinkedList<TemplateTokenizer.SpecialSymbol>();

	protected	String						m_currentLine		= null;
	protected	int							m_currentLineIndex	= -1;
	protected	int							m_lineCount			= 0;

	protected	Token						m_pushBackToken		= null;	// I'm trying this for the time being to kludge ConfigVariable parsing.  Normally, the TemplateParser eats the first string after the opening delimiter and uses that as the block name to get the next block from the BlockFactory, but that screws us up in the ConfigVariable case because then there's nothing for it to parse but its closing delimiter.  Therefore, we'll use the PushBackToken() functionality to push the first string "back on the TemplateTokenizer" so that the ConfigVariable will get it when it calls GetNextToke(), etc.



	//*********************************
	public TemplateTokenizer() {}


	//*********************************
	public boolean Init(File t_sourceTempateFile) {
		try {
			m_sourceTempateFile = t_sourceTempateFile;
			m_fileReader 		= new BufferedReader(new FileReader(m_sourceTempateFile));

			m_symbolList.add(new SpecialSymbol("=", Token.TOKEN_TYPE_EQUALS));
			m_symbolList.add(new SpecialSymbol("\"", Token.TOKEN_TYPE_DOUBLE_QUOTE));

			if (!ReadHeader())	// Fails if there is no header or a malformed header in the template file.
				return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("TemplateTokenizer() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	public int GetLineCount() {
		return m_lineCount;
	}


	//*********************************
	public void PushBackToken(Token p_token) {
		if (m_pushBackToken != null) {
			throw new RuntimeException("TemplateTokenizer.PushBackToken() failed with error: a token is already in the pushback buffer. Only one at a time is allowed.");
		}

		m_pushBackToken = p_token;
	}


	//*********************************
	public Token GetNextToken() {
		// If a token has been pushed back on the tokenizer, then it is the next one that must returned.
		if (m_pushBackToken != null) {
			Token t_nextToken = m_pushBackToken;
			m_pushBackToken = null;
			return t_nextToken;
		}

		char			t_currentChar		= 0;
		int				t_currentTokenType	= Token.TOKEN_TYPE_NOT_TOKEN;
		StringBuilder	t_currentText		= new StringBuilder();
		Token			t_symbolToken;
		Token			t_resultToken		= null;

		try {
			while (m_currentLine != null) {
				while (true) {
//					if (m_currentLine.contains("<%first%>?"))
//							Logger.LogVerbose("Pause here.");

					if (m_currentLineIndex < m_currentLine.length())
						t_currentChar = m_currentLine.charAt(m_currentLineIndex);
					else if (m_currentLineIndex == m_currentLine.length())
						t_currentChar = '\n';	// We have to treat the end-of-line as a white-space character in this new parser so we can't leave it out of the stream;
					else if (!GetNextLine())
						return t_resultToken = null;
					else
						continue;

					switch (t_currentTokenType) {
						case Token.TOKEN_TYPE_NOT_TOKEN:
							if (Character.isWhitespace(t_currentChar)) {
								t_currentText.append(t_currentChar);
								t_currentTokenType = Token.TOKEN_TYPE_WHITE_SPACE;
							}
							else if ((t_symbolToken = IsSymbol(t_currentChar, true)) != null) {
								return t_resultToken = t_symbolToken;
							}
							else {
								t_currentText.append(t_currentChar);
								t_currentTokenType = Token.TOKEN_TYPE_WORD;
							}

							break;
						case Token.TOKEN_TYPE_WHITE_SPACE:
							if (Character.isWhitespace(t_currentChar))
								t_currentText.append(t_currentChar);
							else
								return t_resultToken = new Token(Token.TOKEN_TYPE_WHITE_SPACE, t_currentText.toString());	// If we started as white space, then anything that's not white space, symbol or not, marks the end of the whitespace.

							break;
						case Token.TOKEN_TYPE_WORD:
							if (Character.isWhitespace(t_currentChar) || ((t_symbolToken = IsSymbol(t_currentChar, false)) != null))
								return t_resultToken = new Token(Token.TOKEN_TYPE_WORD, t_currentText.toString());

							t_currentText.append(t_currentChar);
							break;
					}

					++m_currentLineIndex;	// Step to the next character in the line.
				}


			}
		}
		catch (Throwable t_error) {
			Logger.LogException("TemplateTokenizer.GetNextToken() failed with error at line [" + m_currentLineIndex + "]: ", t_error);
			return null;
		}
		finally {
			if (t_resultToken != null)
				Logger.LogVerbose("Token [" + t_resultToken.GetTokenTypeName() + "]	value [" + t_resultToken.m_tokenValue + "]");
		}

		return null;
	}


	//*********************************
	public String GetCurrentLine() {
		return (m_currentLine == null) ? "" : m_currentLine;
	}


	//*********************************
	protected boolean GetNextLine() throws Exception {
		m_currentLine = m_fileReader.readLine();
		if (m_currentLine == null)	// This means that we've reached the EOF.
			return false;

		m_currentLineIndex = 0;	// We have a new line, so we need to be sure that the index is reset.
		m_lineCount++;

		return true;
	}


	//*********************************
	protected boolean IsWhiteSpace(char p_unknownChar) {
		if ((p_unknownChar == ' ') ||
			(p_unknownChar == '\t'))
			return true;

		return false;
	}


	//*********************************
	protected Token IsSymbol(char p_unknownChar, boolean p_moveCurrentIndexIfIsSymbol) {
		int		t_peekIndex;
		char	t_peekChar;
		String	t_symbol;

NextSym:for (SpecialSymbol t_nextSymbol: m_symbolList) {
			t_peekIndex		= m_currentLineIndex;
			t_peekChar		= p_unknownChar;
			t_symbol		= t_nextSymbol.GetSymbol();

			// If this is a multi-char symbol, we have to be sure that its length isn't greater than the remaining characters in the current line, otherwise we'll get an exception in the loop below.
			if ((t_peekIndex + t_symbol.length() - 1) > m_currentLine.length())
				continue;

			for (int i = 0; i < t_symbol.length(); i++) {
				if (t_peekChar != t_symbol.charAt(i))
					continue NextSym;

				++t_peekIndex;

				// If
				if (t_peekIndex == m_currentLine.length()) {
					if (i < (t_symbol.length() - 1))	// If we get to the end of the line and the current symbol still has one or more characters in it, then this symbol is not a match and we need to move on to the next symbol.
						continue NextSym;
					else
						break;	// Otherwise, we've matched the symbol and we're done.
				}

				t_peekChar = m_currentLine.charAt(t_peekIndex);
			}

			if (p_moveCurrentIndexIfIsSymbol)
				m_currentLineIndex = t_peekIndex;

			return new Token(t_nextSymbol.GetTokenType(), t_nextSymbol.GetSymbol());
		}

		return null;
	}


	//*********************************
	/**
	 * This can be used in situations that the code knows that it doesn't have to preserve any white space that may be next in the stream and it wants to throw it away to get to the next word or symbol.
	 */
	public void EatWhiteSpace() {
		// Using GetNextToken() here will handle white space segments that wrap around an end-of-line so that we don't have to come up with that logic here.  If it finds white space, we can eat that token and be sure that the next call to GetNextToken() will not get a white space token.
		Token t_token = GetNextToken();
		if (t_token.m_tokenType == Token.TOKEN_TYPE_WHITE_SPACE)
			return;

		PushBackToken(t_token); // Otherwise, push the token back.  The next call to GetNextToken() will get it.
	}


	//*********************************
	protected boolean ReadHeader() {
		try {
			String t_headerLine = m_fileReader.readLine();
			m_lineCount++;

			String t_headerParts[] = t_headerLine.trim().split("[ \\t]+");
			if ((t_headerParts.length < 3) || !t_headerParts[0].startsWith("%%HEADER%%")) {
				Logger.LogError("ReadHeader() did not find a HEADER record in the first line of the template file.");
				return false;
			}

			String t_attribute[] = t_headerParts[1].split("=");
			if (t_attribute[0].contains("openingDelimiter"))
				m_symbolList.add(new SpecialSymbol(t_attribute[1].trim(), Token.TOKEN_TYPE_OPENING_DELIMITER));
			else  {
				Logger.LogError("ReadHeader() failed with error: the first attribute of the header was not openingDelimiter.");
				return false;
			}

			t_attribute = t_headerParts[2].split("=");
			if (t_attribute[0].contains("closingDelimiter"))
				m_symbolList.add(new SpecialSymbol(t_attribute[1].trim(), Token.TOKEN_TYPE_CLOSING_DELIMITER));
			else  {
				Logger.LogError("ReadHeader() failed with error: the first attribute of the header was not closingDelimiter.");
				return false;
			}

			if (!GetNextLine())	// If this returns FALSE, then we are at EOF and we need to just return what text we have.  m_currentLine is already set == null, so we will automatically be handled correctly (i.e. return null) the next time this function is called.
				return false;
		}
		catch (Throwable t_error) {
			Logger.LogException("TemplateTokenizer.ReadHeader() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}
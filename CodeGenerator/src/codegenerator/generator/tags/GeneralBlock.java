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



import codegenerator.generator.utils.*;
import codegenerator.generator.utils.TemplateTokenizer.*;
import coreutil.logging.*;



/**
	A helper class used by tags that can contain child tags.  It will parse the block contents until it finds a tag that is not a defined opening tag name (i.e. "unknown").
	That "unknown" tag should the the end tag for the block.  You can get it from the general block and check its name to be sure.  This can also be an intermediate tag
	such as "else" or "elseif".  FirstElseBlock is a good example of its usage.
 */
public class GeneralBlock extends TemplateBlock_Base {

	// Data members
	TagParser	m_unknownTag	= null;		// This is the unknown tag that the block found and stopped parsing.  This should be the closing tag for the parent block.  Since we can't push it back on the tokenizer, we have to put it here so that the parent can access it when we return.


	//*********************************
	public TagParser GetUnknownTag() {
		return m_unknownTag;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		// This should never be called, but we'll return true just in case.
		return true;
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return null;	// This should never be called for this class.
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			// EatWhiteSpace() will be used as needed because the white space in a tag is not significant (i.e. required) in its results.
			p_tokenizer.EatWhiteSpace();

			// A general block will parse child tags until it finds a tag that isn't a command.  That tag should be the closing tag for the parent block.
			Token				t_nextToken;
			TagParser			t_tagParser;
			TemplateBlock_Base	t_newBlock;
			while ((t_nextToken = p_tokenizer.GetNextToken()) != null) {
				if (t_nextToken.m_tokenType == Token.TOKEN_TYPE_CLOSING_DELIMITER) {
					Logger.LogError("GeneralBlock.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] at line [" + p_tokenizer.GetLineCount() + "].");
					return false;
				}
				else if (t_nextToken.m_tokenType == Token.TOKEN_TYPE_OPENING_DELIMITER) {
					t_tagParser = new TagParser();
					if (!t_tagParser.Parse(p_tokenizer)) {
						Logger.LogError("GeneralBlock.Parse() failed to parse the tag at line [" + p_tokenizer.GetLineCount() + "].");
						return false;
					}

					t_newBlock = BlockFactory.GetBlock(t_tagParser.GetTagName());
					if (t_newBlock == null) {
						// This should be the closing tag for the parent block, so we'll save it and return.  The parent will have to examine it and decide if it is actually what it expects.
						m_unknownTag = t_tagParser;
						return true;
					}

					if (!t_newBlock.Init(t_tagParser)) {
						Logger.LogError("GeneralBlock.Parse() failed attempting to initialize the tag [" + t_newBlock.GetName() + "] at line [" + p_tokenizer.GetLineCount() + "].");
						return false;
					}

					if (!t_newBlock.Parse(p_tokenizer)) {
						Logger.LogError("GeneralBlock.Parse() failed attempting to parse the tag [" + t_newBlock.GetName() + "].");
						return false;
					}

					m_blockList.add(t_newBlock);
				}
//				else {
//					Logger.LogError("GeneralBlock.Parse() found a token of type [" + t_nextToken.GetTokenTypeName() + "] when it was expecting a WORD for the attribute name at line [" + p_tokenizer.GetLineCount() + "].");
//					return false;
//				}
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("GeneralBlock.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}
}

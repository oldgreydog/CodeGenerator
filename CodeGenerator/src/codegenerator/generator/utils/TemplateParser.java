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

import java.io.*;

import codegenerator.generator.tags.*;



public class TemplateParser {


	//*********************************
	public Tag_Base ParseTemplate(File p_templateFile) {
		try {
			TemplateTokenizer t_fileTokenizer = new TemplateTokenizer();
			if (!t_fileTokenizer.Init(p_templateFile)) {
				Logger.LogFatal("TemplateParser.ParseTemplate() could not initialize the tokenizer for the template file [" + p_templateFile.getAbsolutePath() + "].");
				return null;
			}

			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(t_fileTokenizer)) {
				Logger.LogError("TemplateParser.ParseTemplate() general block parser failed for the template file [" + p_templateFile.getAbsolutePath() + "].");
				return null;
			}

			return t_generalBlock;
		}
		catch (Throwable t_error) {
			Logger.LogException("TemplateParser.ParseTemplate() failed with error : ", t_error);
			return null;
		}
	}
}

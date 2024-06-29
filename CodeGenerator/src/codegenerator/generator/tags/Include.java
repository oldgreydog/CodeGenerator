/*
	Copyright 2021 Wes Kaylor

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
<p>Parses the indicated template file and evaluates that file's contents as if they were part of the parent template file.</p>

<h3>Usage example</h3>

<pre>	<code><b>&lt;%include template = templates/marshalling/marshalling_interface_variables.template %&gt;</b></code></pre>

<h3>Attribute descriptions</h3>

<p><code><b>template</b></code>:  the file path of the template file that will be parsed into the parent file in place of the <code><b>include</b></code> tag.</p>
*/
public class Include extends Tag_Base {

	static public final String		TAG_NAME				= "include";

	static private final String		ATTRIBUTE_TEMPLATE		= "template";


	// Data members
	private	OptionalEvalValue		m_templateFileName		= null;		// I've changed this so that if the file name is constant (i.e. all text tags) at parsing time, then we only get the name once and we parse the target file in the parse phase.


	//*********************************
	public Include() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public Include GetInstance() {
		return new Include();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("Include.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TEMPLATE);
		if (t_nodeAttribute == null) {
			Logger.LogError("Include.Init() did not find the [" + ATTRIBUTE_TEMPLATE + "] attribute that is required for Include tags at line number [" + m_lineNumber + "].");
			return false;
		}

		// If the value requires evaluation, then we need to save the value's GeneralBlock so that we can evaluate it later instead of getting it's string value now.
		GeneralBlock t_valueBlock = t_nodeAttribute.GetAttributeValue();
		if ((t_valueBlock == null) || (t_valueBlock.GetChildTagList() == null)) {
			Logger.LogError("Include.Init() did not get the [" + ATTRIBUTE_TEMPLATE + "] string from attribute that is required for Include tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_templateFileName = new OptionalEvalValue(t_valueBlock);

		return true;
	}


	//*********************************
	public boolean ParseFile(String p_fileName) {
		try {
			File t_templateFile = new File(p_fileName);
			if (!t_templateFile.exists()) {
				Logger.LogFatal("Include.ParseFile() could not open the template file [" + m_templateFileName + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			if (m_tagList != null)
				m_tagList.clear();	// We have to be sure to clear any existing contents that might exist from a previous evaluation.

			// Parse the template file.  The passed in p_tokenizer is from a parent file's contents, but here we are going to parse the indicated template file for this tag and add its execution tree to this tag object so that when the parent's execution tree is being evaluated, this template file's tree can also be evaluated.
			TemplateParser t_parser = new TemplateParser();
			Tag_Base t_template = t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogError("Include.ParseFile() failed in file [" + t_templateFile.getAbsolutePath() + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			AddChildTag(t_template);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("Include.ParseFile() failed with error at line number [" + m_lineNumber + "] in file [" + m_templateFileName + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;	// Now that this is set up to take an attribute value that may have child tags that need to be evaluated in Evaluate(), we can no longer parse that template file here.  It must be deferred.
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			String t_templateFileName = m_templateFileName.Evaluate(p_evaluationContext);
			if ((t_templateFileName == null) || t_templateFileName.isBlank()) {
				Logger.LogError("Include.Evaluate() failed to evaluate the template file name at line [" + m_lineNumber + "].");
				return false;
			}

			// We only need to parse the file if this is the first time through (no child tags exist i.e. file not loaded) or if the filename is not constant (i.e. the filename has to be evaluated and loaded every time we pass through).
			if (!HasContentTags() || !m_templateFileName.IsConstant()) {
				if (!ParseFile(t_templateFileName)) {
					Logger.LogFatal("Include.Evaluate() failed to parse the template file [" + t_templateFileName + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			// I realize that I could mash this check into the nested if() above, but I feel more comfortable with it here since there isn't any way for SURE of getting past here if no tags were read from a file.
			if (!HasContentTags()) {
				Logger.LogError("Include.Evaluate() found no content for the template file [" + t_templateFileName + "] at line [" + m_lineNumber + "].");
				return false;
			}

			// We don't do all of the context changes that a regular FileTab object does because an include is always executing inside another file, so the passed-in context is correct and should not be changed.
			// However, it is possible that the included file has its own TabSettings tag, so we do need to be able to pop it if it gets added.
			int t_tagSettingsManagerStackDepth = p_evaluationContext.GetTabSettingsManagerStackDepth();	// This is kinda fugly, but it's the only way I could come up with to figure out if the file contains a TagSettings tag so that we can pop it below if it does.

			for (Tag_Base t_nextTag: m_tagList) {
				if (!t_nextTag.Evaluate(p_evaluationContext)) {
					Logger.LogError("Include.Evaluate() failed in template file [" + t_templateFileName + "] at line number [" + m_lineNumber + "].");
					return false;
				}
			}

			// If the file added a TabSettingsManager, then we need to pop it here.  And since it's possible for someone to have accidently included more than one tabSettings tag, we need to loop here to be sure that we've gotten all of them.
			while(t_tagSettingsManagerStackDepth < p_evaluationContext.GetTabSettingsManagerStackDepth())
				p_evaluationContext.PopTabSettingsManager();
		}
		catch (Throwable t_error) {
			Logger.LogException("Include.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name           :  " + m_name 											+ "\n");
		t_dump.append(p_tabs + "Template file name :  " + m_templateFileName								+ "\n");

		// This will output the child template file's contents ...
		if (m_tagList != null) {
			for (Tag_Base t_nextTag: m_tagList) {
				t_dump.append("\n\n");
				t_dump.append(t_nextTag.Dump(p_tabs + "\t"));
			}
		}

		return t_dump.toString();
	}
}

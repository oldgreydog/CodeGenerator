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
	Parses the indicated template file and, on evaluation, executes that files contents as if they were part of the parent template file.

	<p>Here's an example of this tag:</p>

	<pre>	<code>&lt;%include template = templates/marshalling/marshalling_interface_variables.template %&gt;</code></pre>
 */
public class Include extends Tag_Base {

	static public final String		TAG_NAME				= "include";

	static private final String		ATTRIBUTE_TEMPLATE		= "template";


	// Data members
	private	String		m_templateFileName;


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

		m_templateFileName = t_nodeAttribute.GetAttributeValueAsString();
		if (m_templateFileName == null) {
			Logger.LogError("Include.Init() did not get the [" + ATTRIBUTE_TEMPLATE + "] string from attribute that is required for Include tags at line number [" + m_lineNumber + "].");
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			File t_templateFile = new File(m_templateFileName);
			if (!t_templateFile.exists()) {
				Logger.LogFatal("Include.Parse() could not open the template file [" + m_templateFileName + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			// Parse the template file.  The passed in p_tokenizer is from a parent file's contents, but here we are going to parse the indicated template file for this tag and add its execution tree to this tag object so that when the parent's execution tree is being evaluated, this template file's tree can also be evaluated.
			TemplateParser t_parser = new TemplateParser();
			Tag_Base t_template = t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogError("Include.Parse() failed in file [" + t_templateFile.getAbsolutePath() + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			AddChildNode(t_template);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("Include.Parse() failed with error at line number [" + m_lineNumber + "] in file [" + m_templateFileName + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			if (m_tagList == null) {
				Logger.LogError("Include.Evaluate() doesn't have any executable content at line [" + m_lineNumber + "].");
				return false;
			}

			// We don't do all of the context changes that a regular FileTab object does because an include is always executing inside another file, so the passed-in context is correct and should not be changed.
			// However, it is possible that the included file has its own TabSettings tag, so we do need to be able to pop it if it gets added.
			int t_tagSettingsManagerStackDepth = p_evaluationContext.GetTabSettingsManagerStackDepth();	// This is kinda fugly, but it's the only way I could come up with to figure out if the file contains a TagSettings tag so that we can pop it below if it does.

			for (Tag_Base t_nextTag: m_tagList) {
				if (!t_nextTag.Evaluate(p_evaluationContext)) {
					Logger.LogError("Include.Evaluate() failed.");
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

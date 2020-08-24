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



import coreutil.config.*;
import coreutil.logging.*;

import java.io.*;
import java.util.concurrent.locks.*;

import codegenerator.generator.utils.*;



/**
	Parses the indicated template file and, on evaluation, writes the output from that template to a file with the given file name.

	<p>Here's an example of this tag:</p>

	<pre><code>&lt;%file template = templates/cached_templates/marshalling/marshalling_interface.template	filename = "&lt;%className%&gt;Marshalling.java"	destDir = "&lt;%root.global.outputPath%&gt;/marshalling" optionalContextName = "parentTable"%&gt;</code></pre>

	<p>As the example shows, you can use tags that evaluate to strings in the values of the attributes, but if you do, you are
	required to surround the value with double quotes as shown.</p>

	<p>Note that it is now possible to nest this file tag inside another file template.  To that ends, the optional
	attribute "optionalContextName" is used if you need to have the	nested file tag execute inside an outer context
	instead of the local context it is defined in.  The value you give this attribute will be the contextname
	you gave to the outerContext tag that encloses the nested instance of the file tag.</p>

	<p>Refer to the files in the <code>Examples/codegenerator</code> folders to better understand its usage.</p>
 */
public class FileBlock extends TemplateBlock_Base {

	static public final String		BLOCK_NAME	= "file";

	static public final String		ATTRIBUTE_TEMPLATE					= "template";
	static public final String		ATTRIBUTE_FILENAME					= "filename";
	static public final String		ATTRIBUTE_DEST_DIR					= "destDir";
	static public final String		ATTRIBUTE_OPTIONAL_CONTEXT_NAME		= "optionalContextName";


	// Static members
	static private final ReentrantLock	s_directoryCreateLock	= new ReentrantLock();

	static private final ReentrantLock	s_countLock				= new ReentrantLock();
	static private 		 int			s_fileCount				= 0;	// Simple way to count the number of files generated.


	//===========================================
	static public void IncrementFileCount() {
		try {
			s_countLock.lock();

			++s_fileCount;
		}
		finally {
			s_countLock.unlock();
		}
	}


	//===========================================
	/**
	 * This should only be called after all of the file threads have been completed, but we'll lock it just in case.
	 *
	 * @return
	 */
	static public int GetFileCount() {
		try {
			s_countLock.lock();

			return s_fileCount;
		}
		finally {
			s_countLock.unlock();
		}
	}



	// Data members
	private	String		m_templateFileName;
	private	String		m_contextName			= null;					// The optional outer context in which to evaluate this variable.

	// These values can themselves be composites of evaluation-time config variables and text, so we have to store them in their TextBlock form and evaluate them at runtime to get their final values.
	private	TemplateBlock_Base	m_fileNameBlock				= null;
	private	TemplateBlock_Base	m_destinationDirectoryBlock	= null;


	//*********************************
	public FileBlock() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public FileBlock GetInstance() {
		return new FileBlock();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("FileBlock.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TEMPLATE);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [" + ATTRIBUTE_TEMPLATE + "] attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_templateFileName = t_nodeAttribute.GetAttributeValueAsString();
		if (m_templateFileName == null) {
			Logger.LogError("FileBlock.Init() did not get the [" + ATTRIBUTE_TEMPLATE + "] string from attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_FILENAME);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [" + ATTRIBUTE_FILENAME + "] attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_fileNameBlock = t_nodeAttribute.GetAttributeValue();
		if (m_fileNameBlock == null) {
			Logger.LogError("FileBlock.Init() did not get the [" + ATTRIBUTE_FILENAME + "] value from attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_DEST_DIR);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [" + ATTRIBUTE_DEST_DIR + "] attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_destinationDirectoryBlock = t_nodeAttribute.GetAttributeValue();
		if (m_destinationDirectoryBlock == null) {
			Logger.LogError("FileBlock.Init() did not get the [" + ATTRIBUTE_DEST_DIR + "] value from attribute that is required for FileBlock tags at line number [" + m_lineNumber + "].");
			return false;
		}


		// The "contextname" attribute is optional, so it's fine if it doesn't exist.
		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_CONTEXT_NAME);
		if (t_nodeAttribute != null)
			m_contextName = t_nodeAttribute.GetAttributeValueAsString();

		return true;
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			File t_templateFile = new File(m_templateFileName);
			if (!t_templateFile.exists()) {
				Logger.LogFatal("FileBlock.Parse() could not open the template file [" + m_templateFileName + "] at line number [" + m_lineNumber + "].");
				System.exit(1);
			}

			// Parse the template file.  The passed in p_tokenizer is from a parent file's contents, but here we are going to parse the indicated template file for this tag and add its execution tree to this tag object so that when the parent's execution tree is being evaluated, this template file's tree can also be evaluated.
			TemplateParser t_parser = new TemplateParser();
			TemplateBlock_Base t_template = t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogError("FileBlock.Parse() failed in file [" + t_templateFile.getAbsolutePath() + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			m_blockList.add(t_template);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("FileBlock.Parse() failed with error at line number [" + m_lineNumber + "] in file [" + m_templateFileName + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			// Build the filename from the component destdir and filename parts.
			StringWriter		t_fileName			= new StringWriter();
			Cursor				t_fileNameCursor	= new Cursor(t_fileName);

			p_evaluationContext.PushNewCursor(t_fileNameCursor);

			m_destinationDirectoryBlock.Evaluate(p_evaluationContext);

			File t_destDirectory = new File(t_fileName.toString());

			try {
				s_directoryCreateLock.lock();

				if (!t_destDirectory.exists() && !t_destDirectory.mkdirs()) {
					Logger.LogError("FileBlock.Evaluate() failed to create the destination directory [" + t_destDirectory.getAbsolutePath() + "].");
					p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
					return false;
				}
			}
			finally {
				s_directoryCreateLock.unlock();
			}

			t_fileNameCursor.Write(File.separator);
			m_fileNameBlock.Evaluate(p_evaluationContext);

			File t_targetFile = new File(t_fileName.toString());
			if (t_targetFile.exists() && !p_evaluationContext.GetCustomCodeManager().ScanFile(t_targetFile)) {	// Check to see if the file has any custom code in it.  If it does, this will save it so that the CustomCode tags can re-insert it during the file generation.
				Logger.LogError("FileBlock.Evaluate() failed to scan the file [" + t_targetFile.getAbsolutePath() + "] for custom code blocks.");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
				return false;
			}

			Logger.LogDebug("FileBlock.Evaluate() is writing to file [" + t_fileName + "]");

			BufferedWriter	t_fileWriter		= new BufferedWriter(new FileWriter(t_targetFile));
			Cursor			t_fileWriterCursor	= new Cursor(t_fileWriter);

			p_evaluationContext.PopCurrentCursor();	// We need to throw away the filename cursor before we add the new file cursor to the context.
			p_evaluationContext.PushNewCursor(t_fileWriterCursor);

			// The addition of outer contexts means that if you use a file tag inside an inner context, you may need to point it to the outer context to get the correct values in the evaluation of the file.
			ConfigNode t_currentNode = p_evaluationContext.GetCurrentNode();
			if (m_contextName != null) {
				t_currentNode = p_evaluationContext.GetOuterContextManager().GetOuterContext(m_contextName);
				if (t_currentNode == null) {
					Logger.LogError("FileBlock.Evaluate() failed to find the outer context [" + m_contextName + "] for the evaluation mode at line [" + m_lineNumber + "].");
					return false;
				}
			}

			p_evaluationContext.PushNewCurrentNode(t_currentNode);	// This is unnecessarily redundant if we aren't changing the context above, but it simpler and cleaner, particularly if we error out in the if() below.

			for (TemplateBlock_Base t_nextBlock: m_blockList) {
				if (!t_nextBlock.Evaluate(p_evaluationContext)) {
					t_fileWriter.close();
					Logger.LogError("FileBlock.Evaluate() failed for file [" + t_targetFile.getAbsolutePath() + "].");
					p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
					return false;
				}
			}

			p_evaluationContext.PopCurrentNode();

			t_fileWriter.close();
			p_evaluationContext.PopCurrentCursor();	// We need to throw away the file cursor now that we're done with it.
		}
		catch (Throwable t_error) {
			Logger.LogException("FileBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		IncrementFileCount();
		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name    :  " + m_name 											+ "\n");
		t_dump.append(p_tabs + "Template file name :  " + m_templateFileName								+ "\n");
		t_dump.append(p_tabs + "Output file name   :  " + m_fileNameBlock.Dump(p_tabs + "\t")				+ "\n");
		t_dump.append(p_tabs + "Destination Dir    :  " + m_destinationDirectoryBlock.Dump(p_tabs + "\t")	+ "\n");

		// This will output the child template...
		for (TemplateBlock_Base t_nextBlock: m_blockList) {
			t_dump.append("\n\n");
			t_dump.append(t_nextBlock.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}
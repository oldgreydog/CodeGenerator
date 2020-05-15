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



import coreutil.logging.*;

import java.io.*;
import java.util.concurrent.locks.*;

import codegenerator.generator.utils.*;
import codegenerator.generator.utils.multithreading.*;



/**
	Parses the indicated template file and, on evaluation, writes the output from that template to a file with the given file name.

	<p>Here's an example of this tag:</p>

	<pre><code>&lt;%file template=templates/cached_templates/marshalling/marshalling_interface.template	filename=&lt;%className%&gt;Marshalling.java	destDir=&lt;%root.global.outputPath%&gt;/marshalling%&gt;</code></pre>

	<p>As the example shows, you can use tags that evaluate to strings in the values of the attributes.</p>

	<p>Refer to the files in the <code>Examples/codegenerator</code> folders to better understand its usage.</p>

	<p>I recently found a case where I needed to use the file tag nested inside another file template.  I quickly realized that everything
	that nesting required was also a large subset of what was needed to multithread the file generation code, so that's what I did.  I was
	essentially able to kill two birds with one stone.  Of course, multithreading wasn't really necessary.  The generator was fast enough as
	sequential-only code, but it was such a trivial add-on after setting up the changes for nesting the file tag that I couldn't resist.  I
	wanted to see how much difference it would make.  So far, on my 8-year-old machine, I'm getting 30-90% faster generation, which is seen
	in the time on the "Generation time (millisec):" line of the output depending on the complexity of the templates and number of files generated.
	This doesn't effect the template or config parse times.</p>
 */
public class FileBlock extends TemplateBlock_Base {

	static public final String		BLOCK_NAME	= "file";

	static public final String		ATTRIBUTE_TEMPLATE		= "template";
	static public final String		ATTRIBUTE_FILENAME		= "filename";
	static public final String		ATTRIBUTE_DEST_DIR		= "destDir";


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
	protected	String				m_templateFileName;

	// These values can themselves be composites of evaluation-time config variables and text, so we have to store them in their TextBlock form and evaluate them at runtime to get their final values.
	protected	TemplateBlock_Base	m_fileNameBlock				= null;
	protected	TemplateBlock_Base	m_destinationDirectoryBlock	= null;


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
			EvaluationContext	t_newContext	= new EvaluationContext(p_evaluationContext);
			FileTask			t_newTask		= new FileTask(this, t_newContext);
			ThreadPoolManager.AddTask(t_newTask);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("FileBlock.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	/**
	 * This should only ever be called from FileTask.run()!  As such, p_evaluationContext is expected to be a copy generated in Evaluate() above
	 * that can be freely altered in this thread.
	 *
	 * @param p_evaluationContext
	 * @return
	 */
	public boolean TaskEvaluate(EvaluationContext p_evaluationContext)
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

			for (TemplateBlock_Base t_nextBlock: m_blockList) {
				if (!t_nextBlock.Evaluate(p_evaluationContext)) {
					t_fileWriter.close();
					Logger.LogError("FileBlock.Evaluate() failed for file [" + t_targetFile.getAbsolutePath() + "].");
					p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.
					return false;
				}
			}

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
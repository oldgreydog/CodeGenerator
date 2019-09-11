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

import codegenerator.generator.utils.*;



/**
	Parses the indicated template file and, on evaluation, writes the output from that template to a file with the given file name.

	<p>Here's an example of this tag:</p>

	<pre><code>&lt;%file template=templates/cached_templates/marshalling/marshalling_interface.template	filename=&lt;%className%&gt;Marshalling.java	destDir=&lt;%root.global.outputPath%&gt;/marshalling%&gt;</code></pre>

	<p>As the example shows, you can use tags that evaluate to strings in the values of the attributes.</p>

	<p>Refer to the files in the <code>Examples/codegenerator</code> folders to better understand its usage.</p>
 */
public class FileBlock extends TemplateBlock_Base {

	static public final String	BLOCK_NAME	= "file";

	// Static members
	static private int		s_fileCount		= 0;	// Simple way to count the number of files generated.


	//===========================================
	static public void IncrementFileCount() {
		++s_fileCount;
	}


	//===========================================
	static public int GetFileCount() {
		return s_fileCount;
	}



	// Data members
	protected	String				m_templateFileName;

	// These values can themselves be composites of evaluation-time config variables and text, so we have to store them in their TextBlock form and evaluate them at runtime to get their final values.
	protected	TemplateBlock_Base	m_fileNameBlock			= null;
	protected	TemplateBlock_Base	m_destDirectoryBlock	= null;


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
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute("template");
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [template] attribute that is required for FileBlock tags.");
			return false;
		}

		m_templateFileName = t_nodeAttribute.GetAttributeValueAsString();
		if (m_templateFileName == null) {
			Logger.LogError("FileBlock.Init() did not get the [template] string from attribute that is required for FileBlock tags.");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute("filename");
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [filename] attribute that is required for FileBlock tags.");
			return false;
		}

		m_fileNameBlock = t_nodeAttribute.GetAttributeValue();
		if (m_fileNameBlock == null) {
			Logger.LogError("FileBlock.Init() did not get the [filename] value from attribute that is required for FileBlock tags.");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute("destDir");
		if (t_nodeAttribute == null) {
			Logger.LogError("FileBlock.Init() did not find the [destDir] attribute that is required for FileBlock tags.");
			return false;
		}

		m_destDirectoryBlock = t_nodeAttribute.GetAttributeValue();
		if (m_destDirectoryBlock == null) {
			Logger.LogError("FileBlock.Init() did not get the [destDir] value from attribute that is required for FileBlock tags.");
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
				Logger.LogFatal("FileBlock.Parse() could not open the template file [" + m_templateFileName + "].");
				System.exit(1);
			}

			// Parse the template file.  The passed in p_tokenizer is from a parent file's contents, but here we are going to parse the indicated template file for this tag and add its execution tree to this tag object so that when the parent's execution tree is being evaluated, this template file's tree can also be evaluated.
			TemplateParser t_parser = new TemplateParser();
			TemplateBlock_Base t_template = t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogError("FileBlock.Parse() failed in file [" + t_templateFile.getAbsolutePath() + "].");
				return false;
			}

			m_blockList.add(t_template);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("FileBlock.Parse() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			// Build the filename from the component destdir and filename parts.
			StringWriter	t_fileName			= new StringWriter();
			Cursor			t_fileNameCursor	= new Cursor(t_fileName);

			m_destDirectoryBlock.Evaluate(p_currentNode, p_rootNode, t_fileNameCursor, p_iterationCounter);

			File t_destDirectory = new File(t_fileName.toString());
			if (!t_destDirectory.exists() && !t_destDirectory.mkdirs()) {
				Logger.LogError("FileBlock.Evaluate() failed to create the destination directory [" + t_destDirectory.getAbsolutePath() + "].");
				return false;
			}

			t_fileNameCursor.Write(File.separator);
			m_fileNameBlock.Evaluate(p_currentNode, p_rootNode, t_fileNameCursor, p_iterationCounter);

			File t_targetFile = new File(t_fileName.toString());
			if (t_targetFile.exists() && !CustomCodeManager.ScanFile(t_targetFile)) {	// Check to see if the file has any custom code in it.  If it does, this will save it so that the CustomCode tags can re-insert it during the file generation.
				Logger.LogError("FileBlock.Evaluate() failed to scan the file [" + t_targetFile.getAbsolutePath() + "] for custom code blocks.");
				return false;
			}

			Logger.LogDebug("FileBlock.Evaluate() is writing to file [" + t_fileName + "]");

			BufferedWriter	t_fileWriter		= new BufferedWriter(new FileWriter(t_targetFile));
			Cursor			t_fileWriterCursor	= new Cursor(t_fileWriter);

			for (TemplateBlock_Base t_nextBlock: m_blockList) {
				if (!t_nextBlock.Evaluate(p_currentNode, p_rootNode, t_fileWriterCursor, p_iterationCounter)) {	// We always start with a zero iteration count when we start the file evaluation.
					t_fileWriter.close();
					Logger.LogError("FileBlock.Evaluate() failed for file [" + t_targetFile.getAbsolutePath() + "].");
					return false;
				}
			}

			t_fileWriter.close();
		}
		catch (Throwable t_error) {
			Logger.LogError("FileBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		IncrementFileCount();
		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name    :  " + m_name 			+ "\n");
		t_dump.append(p_tabs + "Template file name :  " + m_templateFileName	+ "\n");
		t_dump.append(p_tabs + "Output file name   :  " + m_fileNameBlock.Dump(p_tabs + "\t"));
		t_dump.append(p_tabs + "Destination Dir    :  " + m_destDirectoryBlock.Dump(p_tabs + "\t"));

		// This will output the child template...
		for (TemplateBlock_Base t_nextBlock: m_blockList) {
			t_dump.append("\n\n");
			t_dump.append(t_nextBlock.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}

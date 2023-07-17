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


package codegenerator.generator.tags;



import coreutil.config.*;
import coreutil.logging.*;

import java.io.*;
import java.util.concurrent.locks.*;

import codegenerator.generator.utils.*;



/**
	Parses the indicated template file and, on evaluation, writes the output from that template to a file with the given [filename] to the [destDir] directory.

	<p>Here's an example of this tag:</p>

	<pre><code>&lt;%file template = templates/cached_templates/marshalling/marshalling_interface.template filename = "&lt;%className%&gt;Marshalling.java" destDir = "&lt;%root.global.outputPath%&gt;/marshalling" optionalUseTempFile = "false" optionalContextName = "parentTable"%&gt;</code></pre>

	<p>As the example shows, you can use tags that evaluate to strings in the values of the attributes.  The quotes are optional unless you have spaces in the file and/or path name.</p>

	<p>optionalUseTempFile is an optional boolean attribute to indicate that generation should go to a temp file so that if an error occurs, then the original file and any custom code it contains will not be lost.  If the generation completes successfully, then the original file will be deleted and the temp file renamed with the specified filename.</p>

	<p>Note that it is now possible to nest this file tag inside another file template.  To that ends, the optional
	attribute <code>optionalContextName</code> is used if you need to have the	nested file tag execute inside an outer context
	instead of the local context it is defined in.  The value you give this attribute will be the <code>contextName</code>
	you gave to the <code>outerContext</code> tag that encloses the nested instance of the <code>file</code> tag.</p>

	<p>Refer to the files in the <code>Examples/codegenerator</code> folders to better understand its usage.</p>

	<p>NOTE: I changed the class name from FileBlock to FileTag instead of just File since file is a java class and it was simpler to just use a non-clashing name.</p>
 */
public class FileTag extends Tag_Base {

	static public final String		TAG_NAME							= "file";

	static private final String		ATTRIBUTE_TEMPLATE					= "template";
	static private final String		ATTRIBUTE_FILENAME					= "filename";
	static private final String		ATTRIBUTE_DEST_DIR					= "destDir";
	static private final String		ATTRIBUTE_OPTIONAL_USE_TEMP_FILE	= "optionalUseTempFile";	// A boolean to indicate that generation should go to a temp file so that if an error occurs, then the original file and any custom code it contains will not be lost.
	static private final String		ATTRIBUTE_OPTIONAL_CONTEXT_NAME		= "optionalContextName";


	// Static members
	// These locks were only really necessary when I was trying to multi-thread the file tags.  Now that I've backed that out and gone back to single-threading for the time being, these aren't strictly necessary but I'll leave them in anyway.
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
	private boolean		m_useTempFile			= true;		// Optional flag indicating whether the output should go to a temp file or directly overwriting the original file.  We'll default to using the temp file so that we err on the side of saving people from themselves.
	private	String		m_contextName			= null;		// The optional outer context in which to evaluate this variable.

	// These values can themselves be composites of evaluation-time config variables and text, so we have to store them in their Text object form and evaluate them at runtime to get their final values.
	private	Tag_Base	m_fileName				= null;
	private	Tag_Base	m_destinationDirectory	= null;


	//*********************************
	public FileTag() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public FileTag GetInstance() {
		return new FileTag();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("FileTag.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TEMPLATE);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileTag.Init() did not find the [" + ATTRIBUTE_TEMPLATE + "] attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_templateFileName = t_nodeAttribute.GetAttributeValueAsString();
		if (m_templateFileName == null) {
			Logger.LogError("FileTag.Init() did not get the [" + ATTRIBUTE_TEMPLATE + "] string from attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_FILENAME);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileTag.Init() did not find the [" + ATTRIBUTE_FILENAME + "] attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_fileName = t_nodeAttribute.GetAttributeValue();
		if (m_fileName == null) {
			Logger.LogError("FileTag.Init() did not get the [" + ATTRIBUTE_FILENAME + "] value from attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_DEST_DIR);
		if (t_nodeAttribute == null) {
			Logger.LogError("FileTag.Init() did not find the [" + ATTRIBUTE_DEST_DIR + "] attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_destinationDirectory = t_nodeAttribute.GetAttributeValue();
		if (m_destinationDirectory == null) {
			Logger.LogError("FileTag.Init() did not get the [" + ATTRIBUTE_DEST_DIR + "] value from attribute that is required for FileTag tags at line number [" + m_lineNumber + "].");
			return false;
		}


		// The "optionalUseTempFile" attribute is optional, so it's fine if it doesn't exist.
		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_USE_TEMP_FILE);
		if (t_nodeAttribute != null) {
			String t_value = t_nodeAttribute.GetAttributeValueAsString();
			try {
				if ((t_value != null) && !t_value.isBlank())
					m_useTempFile = Boolean.parseBoolean(t_value);
			}
			catch (Throwable t_error) {
				Logger.LogException("FileTag.Init() failed with error in file [" + m_templateFileName + "] because it did not receive an invalid boolean value [" + t_value + "]: ", t_error);
				return false;
			}
		}

		// The "optionalContextName" attribute is optional, so it's fine if it doesn't exist.
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
				Logger.LogFatal("FileTag.Parse() could not open the template file [" + m_templateFileName + "] at line number [" + m_lineNumber + "].");
				System.exit(1);
			}

			// Parse the template file.  The passed in p_tokenizer is from a parent file's contents, but here we are going to parse the indicated template file for this tag and add its execution tree to this tag object so that when the parent's execution tree is being evaluated, this template file's tree can also be evaluated.
			TemplateParser t_parser = new TemplateParser();
			Tag_Base t_template = t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogError("FileTag.Parse() failed in file [" + t_templateFile.getAbsolutePath() + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			AddChildNode(t_template);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("FileTag.Parse() failed with error at line number [" + m_lineNumber + "] in file [" + m_templateFileName + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			if (m_tagList == null) {
				Logger.LogError("FileTag.Evaluate() doesn't have any executable content at line [" + m_lineNumber + "].");
				return false;
			}

			// Build the filename from the component destdir and filename parts.  This has to be done in this confusing way because both the directory path and the filename will almost certainly contain config value tags in them that need to be evaluated to get the correct final value for both.
			StringWriter		t_filePath			= new StringWriter();
			Cursor				t_filePathCursor	= new Cursor(t_filePath);

			p_evaluationContext.PushNewCursor(t_filePathCursor);

			if (!m_destinationDirectory.Evaluate(p_evaluationContext)) {
				Logger.LogError("FileTag.Evaluate() failed to evaluate the destination directory path.");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp filename cursor before we return.
				return false;
			}

			p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp path name cursor before we go any further.

			File t_destDirectory = new File(t_filePath.toString());

			try {
				// I put this lock in when I was trying to multi-thread things.  I didn't go far enough with that effort to completely get it to work, but I didn't remove this because I might try again and it's not causing any harm now that we're back to single-threading.
				s_directoryCreateLock.lock();

				if (!t_destDirectory.exists() && !t_destDirectory.mkdirs()) {
					Logger.LogError("FileTag.Evaluate() failed to create the destination directory [" + t_destDirectory.getAbsolutePath() + "].");
					return false;
				}
			}
			finally {
				s_directoryCreateLock.unlock();
			}


			StringWriter		t_fileName			= new StringWriter();
			Cursor				t_fileNameCursor	= new Cursor(t_fileName);

			p_evaluationContext.PushNewCursor(t_fileNameCursor);

			if (!m_fileName.Evaluate(p_evaluationContext)) {
				Logger.LogError("FileTag.Evaluate() failed to evaluate the filename.");
				p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp filename cursor before we return.
				return false;
			}

			p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp filename cursor before we go any further.

			// Create the full filename path and see if it exists.
			File t_originalFile = new File(t_filePath.toString() + File.separator + t_fileName.toString());
			if (t_originalFile.exists()) {
				if (!p_evaluationContext.GetCustomCodeManager().ScanFile(t_originalFile)) {	// Check to see if the file has any custom code in it.  If it does, this will save it so that the CustomCode tags can re-insert it during the file generation.
					Logger.LogError("FileTag.Evaluate() failed to scan the file [" + t_originalFile.getAbsolutePath() + "] for custom code blocks.");
					return false;
				}
			}
			else {
				p_evaluationContext.GetCustomCodeManager().ClearCache();	// If a new file is being generated after an existing file was regenerated and the custom code blocks aren't unique, then the new file will inherit the previous file's custom code blocks.  !!!That's really BAD!!!  This will clear the custom code cache in those cases.
			}


			// Now we need to figure out if we are doing a temp file or not.
			File t_targetFile = t_originalFile;	// We'll default the target file to the "original" file and only switch it to a temp file if needed.
			if (m_useTempFile) {
				t_targetFile = new File(t_originalFile.getAbsolutePath() + ".temp");
				if (t_targetFile.exists()) {
					if (!t_targetFile.delete()) {	// This should theoretically never happen if this code is properly cleaning up after itself, but just in case, we'll delete it here before we move on.
						Logger.LogError("FileTag.Evaluate() failed to delete the unexpected temp file [" + t_targetFile.getAbsolutePath() + "].");
						return false;
					}
				}
			}

			Logger.LogDebug("FileTag.Evaluate() is writing to file [" + t_targetFile.getPath() + "]");


			// The addition of outer contexts means that if you use a file tag inside an inner context, you may need to point it to the outer context to get the correct values in the evaluation of the file.
			ConfigNode t_currentNode = p_evaluationContext.GetCurrentNode();
			if (m_contextName != null) {
				t_currentNode = p_evaluationContext.GetOuterContextManager().GetOuterContext(m_contextName);
				if (t_currentNode == null) {
					Logger.LogError("FileTag.Evaluate() failed to find the outer context [" + m_contextName + "] for the evaluation mode at line [" + m_lineNumber + "].");
					return false;
				}
			}

			p_evaluationContext.PushNewCurrentNode(t_currentNode);	// This is unnecessarily redundant if we aren't changing the context above, but it simpler and cleaner, particularly if we error out in the if() below.

			int t_tagSettingsManagerStackDepth = p_evaluationContext.GetTabSettingsManagerStackDepth();	// This is kinda fugly, but it's the only way I could come up with to figure out if the file contains a TagSettings tag so that we can pop it below if it does.


			BufferedWriter	t_fileWriter		= new BufferedWriter(new FileWriter(t_targetFile));
			Cursor			t_fileWriterCursor	= new Cursor(t_fileWriter);

			p_evaluationContext.PushNewCursor(t_fileWriterCursor);

			for (Tag_Base t_nextTag: m_tagList) {
				if (!t_nextTag.Evaluate(p_evaluationContext)) {
					t_fileWriter.close();
					Logger.LogError("FileTag.Evaluate() failed for file [" + t_targetFile.getAbsolutePath() + "].");
					p_evaluationContext.PopCurrentCursor();	// We need to clean up the temp cursor before we fail out of the function.

// NOTE!!! I put this in at first, but then I remembered that it can be useful sometimes to see where the generator failed in the file so I commented it out.  I'll leave this here just in case there's ever a reason to bring it back.
					// If we were using a temp file, we need to delete it before we return.
//					if (m_useTempFile) {
//						Logger.LogError("FileTag.Evaluate() will delete the temp file [" + t_targetFile.getAbsolutePath() + "].");
//						if (!t_targetFile.delete()) {
//							Logger.LogError("FileTag.Evaluate() failed to delete the temp file [" + t_targetFile.getAbsolutePath() + "].");
//							return false;
//						}
//					}

					return false;
				}
			}

			// If the file added a TabSettingsManager, then we need to pop it here.  And since it's possible for someone to have accidently included more than one tabSettings tag, we need to loop here to be sure that we've gotten all of them.
			while(t_tagSettingsManagerStackDepth < p_evaluationContext.GetTabSettingsManagerStackDepth())
				p_evaluationContext.PopTabSettingsManager();

			p_evaluationContext.PopCurrentNode();

			t_fileWriter.close();
			p_evaluationContext.PopCurrentCursor();	// We need to throw away the file cursor now that we're done with it.


			// Finally, if we were using a temp file, we need to delete the original file and replace it with the temp file.
			if (m_useTempFile) {
				if (t_originalFile.exists() && !t_originalFile.delete()) {
					Logger.LogError("FileTag.Evaluate() failed to delete the original file [" + t_targetFile.getAbsolutePath() + "].");
					return false;
				}

				if (!t_targetFile.renameTo(t_originalFile)) {
					Logger.LogError("FileTag.Evaluate() failed to rename the temp file [" + t_targetFile.getAbsolutePath() + "] to the original file name [" + t_originalFile.getAbsolutePath() + "].");
					return false;
				}

				Logger.LogDebug("FileTag.Evaluate() replaced the original file [" + t_originalFile.getPath() + "] with the temp file [" + t_targetFile.getPath() + "]");
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FileTag.Evaluate() failed with error: ", t_error);
			return false;
		}

		IncrementFileCount();
		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name           :  " + m_name 											+ "\n");
		t_dump.append(p_tabs + "Template file name :  " + m_templateFileName								+ "\n");
		t_dump.append(p_tabs + "Output file name   :  " + m_fileName.Dump(p_tabs + "\t")				+ "\n");
		t_dump.append(p_tabs + "Destination Dir    :  " + m_destinationDirectory.Dump(p_tabs + "\t")	+ "\n");

		if (m_tagList == null)
			return t_dump.toString();

		// This will output the child template file's contents ...
		for (Tag_Base t_nextTag: m_tagList) {
			t_dump.append("\n\n");
			t_dump.append(t_nextTag.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}

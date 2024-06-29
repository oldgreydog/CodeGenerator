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

import java.nio.file.*;
import java.nio.file.attribute.*;

import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
<p>Copies a file to the destination directory without evaluating it as a template.  It was needed for certain files, such as batch files, in
the ArchTemplates project that did have any generated content but needed to be moved into the output project tree.  Technically, <code>file</code>
could have been used with a template whose content was completely wrapped in a <code>text</code> tag, but that seemed a little unnecessarily indirect.</p>

<h3>Usage example</h3>

<p><pre><code><b>&lt;%copyfile  sourceFilePath = &lt;%root.global.rootTemplatesPath%&gt;/project/generate_all  targetDirectory = "&lt;%root.global.outputPath%&gt;/coredb"  optionalMakeFileExecutable = true %&gt;</b></code></pre></p>

<h3>Attribute descriptions</h3>

<p><code>sourceFilePath</code>: the file path, including the filename, of the file that will be copied from</p>

<p><code><b>targetDirectory</b></code>: the directory path where the file will be copied to</p>

<p><code><b>optionalMakeFileExecutable</b></code>: an optional boolean attribute to indicate that once the file has been copied then it needs to be marked as executable.  This will probably be used mostly with batch files, for example.</p>
 */
public class CopyFile extends Tag_Base {

	static public final String		TAG_NAME							= "copyfile";

	static private final String		ATTRIBUTE_SOURCE_FILE_PATH			= "sourceFilePath";
	static private final String		ATTRIBUTE_TARGET_DIRECTORY_PATH		= "targetDirectory";
	static private final String		ATTRIBUTE_OPTIONAL_MAKE_EXECUTABLE	= "optionalMakeFileExecutable";


	// Data members

	// These values can themselves be composites of evaluation-time config variables and text, so we have to store them in their Text object form and evaluate them at runtime to get their final values.
	private	OptionalEvalValue	m_sourceFileName		= null;		// This attribute is now optional if you are going to include the "contents" inside the tag and a matching <endFile> tag.
	private	OptionalEvalValue	m_destinationDirectory	= null;
	private boolean				m_makeFileExecutable	= false;


	//*********************************
	public CopyFile() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public CopyFile GetInstance() {
		return new CopyFile();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("CopyFile.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}


		// Now that the [template] attribute is optional, we need to treat it differently from how we treat the required attributes below.
		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_SOURCE_FILE_PATH);
		if (t_nodeAttribute == null) {
			Logger.LogError("CopyFile.Init() did not find the [" + ATTRIBUTE_SOURCE_FILE_PATH + "] attribute that is required for CopyFile tags at line number [" + m_lineNumber + "].");
			return false;
		}

		GeneralBlock t_valueBlock = t_nodeAttribute.GetAttributeValue();
		if ((t_valueBlock == null) || !t_valueBlock.HasContentTags()) {
			Logger.LogError("CopyFile.Init() did not get the [" + ATTRIBUTE_SOURCE_FILE_PATH + "] value from attribute that is required for CopyFile tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_sourceFileName = new OptionalEvalValue(t_valueBlock);


		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TARGET_DIRECTORY_PATH);
		if (t_nodeAttribute == null) {
			Logger.LogError("CopyFile.Init() did not find the [" + ATTRIBUTE_TARGET_DIRECTORY_PATH + "] attribute that is required for CopyFile tags at line number [" + m_lineNumber + "].");
			return false;
		}

		t_valueBlock = t_nodeAttribute.GetAttributeValue();
		if ((t_valueBlock == null) || !t_valueBlock.HasContentTags()) {
			Logger.LogError("CopyFile.Init() did not get the [" + ATTRIBUTE_TARGET_DIRECTORY_PATH + "] value from attribute that is required for CopyFile tags at line number [" + m_lineNumber + "].");
			return false;
		}

		m_destinationDirectory = new OptionalEvalValue(t_valueBlock);


		// The "optionalMakeFileExecutable" attribute is optional, so it's fine if it doesn't exist.
		t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_MAKE_EXECUTABLE);
		if (t_nodeAttribute != null) {
			String t_value = t_nodeAttribute.GetAttributeValueAsString();
			try {
				if ((t_value != null) && !t_value.isBlank())
					m_makeFileExecutable = Boolean.parseBoolean(t_value);
			}
			catch (Throwable t_error) {
				Logger.LogException("CopyFile.Init() failed because it received an invalid boolean value [" + t_value + "] for attribute [" + ATTRIBUTE_OPTIONAL_MAKE_EXECUTABLE + "]: ", t_error);
				return false;
			}
		}

		return true;
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		// This is a unary tag so there is nothing to parse for it.
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			String t_sourceFileName = m_sourceFileName.Evaluate(p_evaluationContext);
			if ((t_sourceFileName == null) || t_sourceFileName.isBlank()) {
				Logger.LogError("CopyFile.Evaluate() failed to evaluate the source file name at line [" + m_lineNumber + "].");
				return false;
			}

			Path t_sourcePath = Paths.get(t_sourceFileName);
			if (Files.notExists(t_sourcePath)) {
				Logger.LogError("CopyFile.Evaluate() failed to find the source file [" + t_sourcePath.toString() + "].");
				return false;
			}

			if (Files.size(t_sourcePath) == 0) {
				Logger.LogError("CopyFile.Evaluate() failed to find the source file [" + t_sourcePath.toString() + "] is empty.  No copy will be performed.");
				return false;
			}


			String t_destinationDirectory = m_destinationDirectory.Evaluate(p_evaluationContext);
			if ((t_destinationDirectory == null) || t_destinationDirectory.isBlank()) {
				Logger.LogError("CopyFile.Evaluate() failed to evaluate the destination directory path.");
				return false;
			}

			Path t_destinationPath = Paths.get(t_destinationDirectory);
			if (Files.notExists(t_destinationPath) && (Files.createDirectory(t_destinationPath) == null)) {
				Logger.LogError("CopyFile.Evaluate() failed to create the destination directory [" + t_destinationPath.toString() + "].");
				return false;
			}


			Logger.LogDebug("CopyFile.Evaluate() is copying the source file [" + t_sourcePath.toString() + "] to directory [" + t_destinationPath.toString() + "].");


			Path t_targetPath = t_destinationPath.resolve(t_sourcePath.getFileName());
			Files.deleteIfExists(t_targetPath);

			if (Files.copy(t_sourcePath, t_targetPath) == null) {
				Logger.LogError("CopyFile.Evaluate() failed to copy the source file [" + t_sourcePath.toString() + "] to directory [" + t_destinationPath.toString() + "].");
				return false;
			}

			if (m_makeFileExecutable) {
				if (Files.setPosixFilePermissions(t_targetPath, PosixFilePermissions.fromString("rwxr--r--")) == null) {
					Logger.LogError("CopyFile.Evaluate() failed to set the permissions for the destination file [" + t_destinationPath.toString() + "].");
					return false;
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("CopyFile.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name           :  " + m_name 					+ "\n");

// Disabled these two since they now require evaluation to get their values and I don't care about setting that up right now.
//		t_dump.append(p_tabs + "Source file path   :  " + m_sourceFileName			+ "\n");
//		t_dump.append(p_tabs + "Destination path   :  " + m_destinationDirectory	+ "\n");

		return t_dump.toString();
	}
}

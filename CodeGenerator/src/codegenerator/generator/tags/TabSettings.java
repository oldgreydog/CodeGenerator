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



import java.io.*;

import codegenerator.generator.utils.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	Sets the tab parameters for the template until the next <code>tabSettings</code> changes them.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%tabSettings tabLength = "4" outputType = "tabs" %&gt;</code></pre>

	<pre><code>&lt;%tabSettings tabLength = "4" outputType = "spaces" %&gt;</code></pre>

	<p>The <code>tabLength</code> attribute defines how many columns will be represented by a tab.</p>

	<p>The <code>outputType</code> attribute defines whether a tab character or spaces will be written
	to the output to represent the tab stop.</p>
 */
public class TabSettings extends TemplateBlock_Base {

	static public final String		BLOCK_NAME					= "tabSettings";

	static private final String		STOP_ATTRIBUTE_TAB_LENGTH	= "tabLength";
	static private final String		STOP_ATTRIBUTE_OUTPUT_TYPE	= "outputType";


	// Data members
	private int		m_tabLength		= -1;
	private int		m_outputType	= -1;


	//*********************************
	public TabSettings() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			m_lineNumber = p_tagParser.GetLineNumber();

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(STOP_ATTRIBUTE_TAB_LENGTH);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabSettings.Init() did not find the [" + STOP_ATTRIBUTE_TAB_LENGTH + "] attribute that is required for [" + BLOCK_NAME + "] tags at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_tabLengthString = t_nodeAttribute.GetAttributeValueAsString();
			if (t_tabLengthString == null) {
				Logger.LogError("TabSettings.Evaluate() failed to get the [" + STOP_ATTRIBUTE_TAB_LENGTH + "] value at line number [" + m_lineNumber + "].");
				return false;
			}

			m_tabLength = Integer.parseInt(t_tabLengthString);


			// The offset attribute is required for type "stop" but not for "marker".
			t_nodeAttribute = p_tagParser.GetNamedAttribute(STOP_ATTRIBUTE_OUTPUT_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabSettings.Init() did not find the [" + STOP_ATTRIBUTE_OUTPUT_TYPE + "] attribute that is required for [" + BLOCK_NAME + "] tags at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_outputType = t_nodeAttribute.GetAttributeValueAsString();
			if (t_outputType == null) {
				Logger.LogError("TabSettings.Evaluate() failed to get the [" + STOP_ATTRIBUTE_OUTPUT_TYPE + "] value at line number [" + m_lineNumber + "].");
				return false;
			}

			if (t_outputType.equalsIgnoreCase("tabs"))
				m_outputType = TabStop.OUTPUT_TYPE_TABS;
			else
				m_outputType = TabStop.OUTPUT_TYPE_SPACES;

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("TabSettings.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new TabSettings();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			if (m_tabLength == -1) {
				Logger.LogError("TabSettings.Evaluate() - the tab length was not set for this template.");
				return false;
			}

			if (m_outputType == -1) {
				Logger.LogError("TabSettings.Evaluate() - the output type was not set for this template.");
				return false;
			}

			TabStop.SetTabSize(m_tabLength);
			TabStop.SetOutputType(m_outputType);
		}
		catch (Throwable t_error) {
			Logger.LogError("TabSettings.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	private void AddTabSpaces(Cursor p_writer, int p_spaceCount) {
		for (int i = 0; i < p_spaceCount; i++)
			p_writer.Write(" ");
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name  :  " + m_name			+ "\n");
		t_dump.append(p_tabs + "Tab length		 :  " + m_tabLength		+ "\n");
		t_dump.append(p_tabs + "Output type		 :  " + m_outputType 	+ "\n");

		return t_dump.toString();
	}
}

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
	Enables better alignment of code elements.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%tabStop stopType = "stop" offset = "40" %&gt;</code></pre>

	<pre><code>&lt;%tabStop stopType = "marker" offset = "20" %&gt;</code></pre>

	<pre><code>&lt;%tabStop stopType = "marker" %&gt;</code></pre>

	<p>A <code>stopType</code> of "stop" requires the <code>offset</code> attribute be set because
	this type will use the offset as a hard stop from the beginning of the line.</p>

	<p>A <code>stopType</code> of "marker" requires that a <code>tabMarker</code> tag be set somewhere
	before this tag and whatever the offset of the beginning of that marker's tag is becomes the
	base offset for this <code>tabStop</code>.  The <code>offset</code> attribute can optionally be
	used to add an extra offset to the arbitrary marker value to create multiple columns of alignment.</p>

	<p>!!!NOTE!!! I highly recommend that you always precede the use of this tag with a tab or space so that
	if the current line of generated text has already passed the desired tab stop, then there will be
	at least one white space between the last text and whatever is after this tag.  If you don't, then you
	will risk running two pieces of text together with no white space between them and thereby causing
	the generation of invalid code.</p>
 */
public class TabStop extends TemplateBlock_Base {

	static public final String		BLOCK_NAME					= "tabStop";

	static private final String		STOP_ATTRIBUTE_STOP_TYPE	= "stopType";
	static private final String		STOP_ATTRIBUTE_OFFSET		= "offset";

	static private final String		STOP_TYPE_lABEL_STOP		= "stop";
	static private final String		STOP_TYPE_lABEL_MARKER		= "marker";

	static public  final int		OUTPUT_TYPE_UNDEFINED		= -1;
	static public  final int		OUTPUT_TYPE_TABS			= 1;
	static public  final int		OUTPUT_TYPE_SPACES			= 2;

	static private final int		STOP_TYPE_UNDEFINED			= -1;
	static private final int		STOP_TYPE_STOP				= 1;
	static private final int		STOP_TYPE_MARKER			= 2;


	// Static members
	// !!!!NOTE!!!! Since the generator is strictly single threaded, these static class members do not require locking around them in this code.
	static private int		s_tabSize				= -1;
	static private int		s_outputType			= OUTPUT_TYPE_UNDEFINED;
	static private int		s_markerColumnNumber	= -1;


	//===========================================
	static public void SetTabSize(int p_tabSize) {
		s_tabSize = p_tabSize;
	}


	//===========================================
	static public int GetTabSize() {
		return s_tabSize;
	}


	//===========================================
	static public void SetOutputType(int p_outputType) {
		s_outputType = p_outputType;
	}


	//===========================================
	static public int GetOutputType() {
		return s_outputType;
	}


	//===========================================
	static public void SetMarker(int p_markerColumnNumber) {
		s_markerColumnNumber = p_markerColumnNumber;
	}


	//===========================================
	static public int GetMarker() {
		return s_markerColumnNumber;
	}


	//===========================================
	/**
	 * This steps through the current line and counts the columns, substituting the tab size for each tab and handling adjustment for when there is text within a tab stop.
	 * @return
	 */
	static public int GetCurrentLineLength(String p_currentLine) {
		int		t_index			= 0;
		int		t_lineLength	= 0;
		boolean t_lastWasTab	= false;
		while (t_index < p_currentLine.length()) {
			if (p_currentLine.charAt(t_index++) == '\t') {
				if (t_lastWasTab) {
					t_lineLength += s_tabSize;
					continue;
				}

				t_lastWasTab = true;
				t_lineLength = ((t_lineLength / s_tabSize) + 1) * s_tabSize;	// When a tab is preceded by some number of characters and since tab stops are fixed and you can type "into" the next tab space up to (s_tabSize - 1) characters, this is the easiest way to calculate the next tab stop position after the end of an arbitrary character string.
				continue;
			}

			t_lastWasTab = false;
			++t_lineLength;			// This may not be internationalization safe if the template charset has characters that are more than one column wide.
		}


		return t_lineLength;
	}




	// Data members
	private	int		m_stopType			= STOP_TYPE_UNDEFINED;
	private	int		m_offset			= -1;


	//*********************************
	public TabStop() {
		super(BLOCK_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(STOP_ATTRIBUTE_STOP_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabStop.Init() did not find the [" + STOP_ATTRIBUTE_STOP_TYPE + "] attribute that is required for TabStop tags.");
				return false;
			}

			StringWriter	t_valueWriter		= new StringWriter();
			Cursor			t_valueCursor		= new Cursor(t_valueWriter);
			LoopCounter		t_iterationCounter	= new LoopCounter();
			if (!t_nodeAttribute.GetValue().Evaluate(null, null, t_valueCursor, t_iterationCounter)) {
				Logger.LogError("TabStop.Evaluate() failed to evaluate the [" + STOP_ATTRIBUTE_STOP_TYPE + "] value.");
				return false;
			}

			String t_value = t_valueWriter.toString();
			if (t_value.equalsIgnoreCase(STOP_TYPE_lABEL_STOP))
				m_stopType = STOP_TYPE_STOP;
			else if (t_value.equalsIgnoreCase(STOP_TYPE_lABEL_MARKER))
				m_stopType = STOP_TYPE_MARKER;
			else {
				Logger.LogError("TabStop.Init() received an invalid value [" + t_value + "]] for the attribute [" + STOP_ATTRIBUTE_STOP_TYPE + "].");
				return false;
			}


			// The offset attribute is required for type "stop" but not for "marker".
			t_nodeAttribute = p_tagParser.GetNamedAttribute(STOP_ATTRIBUTE_OFFSET);
			if ((m_stopType == STOP_TYPE_STOP) && (t_nodeAttribute == null)) {
				Logger.LogError("TabStop.Init() did not find the [" + STOP_ATTRIBUTE_OFFSET + "] attribute that is required when the [" + STOP_ATTRIBUTE_STOP_TYPE + "] attribute is set to [" + STOP_TYPE_lABEL_STOP + "].");
				return false;
			}

			if (t_nodeAttribute != null) {
				t_valueWriter	= new StringWriter();
				t_valueCursor	= new Cursor(t_valueWriter);
				if (!t_nodeAttribute.GetValue().Evaluate(null, null, t_valueCursor, t_iterationCounter)) {
					Logger.LogError("TabStop.Evaluate() failed to evaluate the [" + STOP_ATTRIBUTE_OFFSET + "] value.");
					return false;
				}

				m_offset = Integer.parseInt(t_valueWriter.toString());
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("TabStop.Init() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new TabStop();
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
			if (s_tabSize == -1) {
				Logger.LogError("TabStop.Evaluate() - the tab size was not set for this template.");
				return false;
			}

			int t_stopOffset = -1;
			if (m_stopType == STOP_TYPE_STOP) {
				t_stopOffset = m_offset;

				// For the STOP type, if the offset isn't a round tab multiple, we'll round it up to the next tab stop.
				if ((t_stopOffset % s_tabSize) > 0)
					t_stopOffset = ((t_stopOffset / s_tabSize) + 1) * s_tabSize;
			}
			else {
				t_stopOffset = GetMarker();
				if (t_stopOffset < 0) {
					Logger.LogError("TabStop.Evaluate() - the tab marker was not set for this tab stop.");
					return false;
				}

				if (m_offset > -1)
					t_stopOffset += m_offset;
			}


			int t_lineLength = GetCurrentLineLength(p_writer.GetCurrentLineContents());
			if (t_lineLength >= t_stopOffset)
				return true;	// We're already past the tab stop requested so we don't need to do anything.


			// If the line length isn't at a tab stop, then we'll add one tab to get it up to the next tab stop and then loop from there to get any remaining tabs.
			if ((t_lineLength % s_tabSize) > 0) {
				int t_roundUpTabLineLength = ((t_lineLength / s_tabSize) + 1) * s_tabSize;	// The current line length may or may not fall on a tab boundary and adding a tab to square it up might put it passed the final target offset if we are aiming for a non-stop-boundary marker.
				if (t_roundUpTabLineLength <= t_stopOffset) {
					// If it is safe to add a tab's worth of space without going past the final target offset, then do so.
					t_lineLength = t_roundUpTabLineLength;

					if (s_outputType == OUTPUT_TYPE_TABS)
						p_writer.Write("\t");
					else
						AddTabSpaces(p_writer, s_tabSize - (t_lineLength % s_tabSize));
				}
				else {
					// Otherwise, we are dealing with a non-stop-boundary marker and we only need to add enough spaces to get to the final target offset.
					for (int i = (t_stopOffset - t_lineLength); i > 0; --i)
						p_writer.Write(" ");

					return true;	// We're done if we get here.
				}
			}

			// Now that we have the line length to a tab stop, we can add full tab stops (and, in the case of a non-stop-boundary marker, trailing spaces) until we reach the target offset.
			int t_remainingLength		= t_stopOffset - t_lineLength;
			int t_remainingTabsToAdd	= t_remainingLength / s_tabSize;
			int t_remainingSpacesToAdd	= t_remainingLength % s_tabSize;

			while (t_remainingTabsToAdd > 0) {
				if (s_outputType == OUTPUT_TYPE_TABS)
					p_writer.Write("\t");
				else
					AddTabSpaces(p_writer, s_tabSize);

				--t_remainingTabsToAdd;
			}

			while (t_remainingSpacesToAdd > 0) {
				p_writer.Write(" ");
				--t_remainingSpacesToAdd;
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("TabStop.Evaluate() failed with error: ", t_error);
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

		t_dump.append(p_tabs + "Block type name  :  " + m_name		+ "\n");
		t_dump.append(p_tabs + "Stop type		 :  " + ((m_stopType == STOP_TYPE_STOP) ? STOP_TYPE_lABEL_STOP : STOP_TYPE_lABEL_MARKER) 	+ "\n");
		t_dump.append(p_tabs + "Offset			 :  " + m_offset 	+ "\n");

		return t_dump.toString();
	}
}

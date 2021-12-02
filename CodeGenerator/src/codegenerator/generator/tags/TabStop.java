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



import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
	<p>Enables better alignment of code elements.</p>

	<p>Example use of this tag:</p>

	<pre>	<code>&lt;%tabStop stopType = "stop" offset = "40" %&gt;</code></pre>

	<pre>	<code>&lt;%tabStop stopType = "marker" offset = "20" %&gt;</code></pre>

	<pre>	<code>&lt;%tabStop stopType = "marker" %&gt;</code></pre>

	<p>A <code><B>stopType</B></code> of "stop" requires the <code><B>offset</B></code> attribute be set because
	this type will use the offset as a hard stop from the beginning of the line.</p>

	<p>A <code><B>stopType</B></code> of "marker" requires that a <code><B>tabMarker</B></code> tag be set somewhere
	before this tag and whatever the offset of the beginning of that marker's tag is becomes the
	base offset for this <code><B>tabStop</B></code>.  The <code><B>offset</B></code> attribute can optionally be
	used to add an extra offset to the arbitrary marker value to create multiple columns of alignment.</p>

	<p>!!!NOTE!!!  If the text already written to the current line passes where the tab stop is calculated to be,
	then this code will not add ANY whitespace at all!  Because of that, I highly recommend that you always precede
	the use of this tag with a tab or space so that if the current line of generated text has already passed the
	desired tab stop, then there will be at least one white space between the last text and whatever is after this tag.
	If you don't, then you will risk running two pieces of text together with no white space between them and thereby causing
	the generation of invalid code.</p>
 */
public class TabStop extends Tag_Base {

	static public final String		TAG_NAME					= "tabStop";

	static private final String		ATTRIBUTE_STOP_TYPE			= "stopType";
	static private final String		ATTRIBUTE_OFFSET			= "offset";

	static private final String		STOP_TYPE_LABEL_STOP		= "stop";
	static private final String		STOP_TYPE_LABEL_MARKER		= "marker";

	static private final int		STOP_TYPE_UNDEFINED			= -1;
	static private final int		STOP_TYPE_STOP				= 1;
	static private final int		STOP_TYPE_MARKER			= 2;


	// Data members
	private	int		m_stopType			= STOP_TYPE_UNDEFINED;
	private	int		m_offset			= -1;


	//*********************************
	public TabStop() {
		super(TAG_NAME);
		m_isSafeForText = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TabStop.Init() failed in the parent Init().");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_STOP_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabStop.Init() did not find the [" + ATTRIBUTE_STOP_TYPE + "] attribute that is required for TabStop tags at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_stopType = t_nodeAttribute.GetAttributeValueAsString();
			if (t_stopType == null) {
				Logger.LogError("TabStop.Evaluate() failed to get the [" + ATTRIBUTE_STOP_TYPE + "] value at line number [" + m_lineNumber + "].");
				return false;
			}

			if (t_stopType.equalsIgnoreCase(STOP_TYPE_LABEL_STOP))
				m_stopType = STOP_TYPE_STOP;
			else if (t_stopType.equalsIgnoreCase(STOP_TYPE_LABEL_MARKER))
				m_stopType = STOP_TYPE_MARKER;
			else {
				Logger.LogError("TabStop.Init() received an invalid value [" + t_stopType + "]] for the attribute [" + ATTRIBUTE_STOP_TYPE + "] at line number [" + m_lineNumber + "].");
				return false;
			}


			// The offset attribute is required for type "stop" but not for "marker".
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OFFSET);
			if ((m_stopType == STOP_TYPE_STOP) && (t_nodeAttribute == null)) {
				Logger.LogError("TabStop.Init() did not find the [" + ATTRIBUTE_OFFSET + "] attribute that is required when the [" + ATTRIBUTE_STOP_TYPE + "] attribute is set to [" + STOP_TYPE_LABEL_STOP + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			if (t_nodeAttribute != null) {
				String t_offsetValue = t_nodeAttribute.GetAttributeValueAsString();
				if (t_offsetValue == null) {
					Logger.LogError("TabStop.Evaluate() failed to get the [" + ATTRIBUTE_OFFSET + "] value at line number [" + m_lineNumber + "].");
					return false;
				}

				m_offset = Integer.parseInt(t_offsetValue.toString());
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TabStop.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TabStop();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			TabSettingsManager t_tabSettingsManager = p_evaluationContext.GetTabSettingsManager();

			int t_tabsize = t_tabSettingsManager.GetTabSize();
			if (t_tabsize == -1) {
				Logger.LogError("TabStop.Evaluate() - the tab size was not set for this template.");
				return false;
			}

			int t_stopOffset = -1;
			if (m_stopType == STOP_TYPE_STOP) {
				t_stopOffset = m_offset;

				// For the STOP type, if the offset isn't a round tab multiple, we'll round it up to the next tab stop.
				if ((t_stopOffset % t_tabsize) > 0)
					t_stopOffset = ((t_stopOffset / t_tabsize) + 1) * t_tabsize;
			}
			else {
				t_stopOffset = t_tabSettingsManager.GetMarker();
				if (t_stopOffset < 0) {
					Logger.LogError("TabStop.Evaluate() - the tab marker was not set for this tab stop.");
					return false;
				}

				if (m_offset > -1)
					t_stopOffset += m_offset;
			}


			Cursor	t_cursor		= p_evaluationContext.GetCursor();
			int		t_lineLength	= t_tabSettingsManager.GetCurrentLineLength(t_cursor.GetCurrentLineContents());
			if (t_lineLength >= t_stopOffset)
				return true;	// We're already past the tab stop requested so we don't need to do anything.


			// If the line length isn't at a tab stop, then we'll add one tab to get it up to the next tab stop and then loop from there to get any remaining tabs.
			if ((t_lineLength % t_tabsize) > 0) {
				int t_roundUpTabLineLength = ((t_lineLength / t_tabsize) + 1) * t_tabsize;	// The current line length may or may not fall on a tab boundary and adding a tab to square it up might put it passed the final target offset if we are aiming for a non-stop-boundary marker.
				if (t_roundUpTabLineLength <= t_stopOffset) {
					// If it is safe to add a tab's worth of space without going past the final target offset, then do so.
					t_lineLength = t_roundUpTabLineLength;

					if (t_tabSettingsManager.GetOutputType() == TabSettingsManager.OUTPUT_TYPE_TABS)
						t_cursor.Write("\t");
					else
						AddTabSpaces(t_cursor, t_tabsize - (t_lineLength % t_tabsize));
				}
				else {
					// Otherwise, we are dealing with a non-stop-boundary marker and we only need to add enough spaces to get to the final target offset.
					for (int i = (t_stopOffset - t_lineLength); i > 0; --i)
						t_cursor.Write(" ");

					return true;	// We're done if we get here.
				}
			}

			// Now that we have the line length to a tab stop, we can add full tab stops (and, in the case of a non-stop-boundary marker, trailing spaces) until we reach the target offset.
			int t_remainingLength		= t_stopOffset - t_lineLength;
			int t_remainingTabsToAdd	= t_remainingLength / t_tabsize;
			int t_remainingSpacesToAdd	= t_remainingLength % t_tabsize;

			while (t_remainingTabsToAdd > 0) {
				if (t_tabSettingsManager.GetOutputType() == TabSettingsManager.OUTPUT_TYPE_TABS)
					t_cursor.Write("\t");
				else
					AddTabSpaces(t_cursor, t_tabsize);

				--t_remainingTabsToAdd;
			}

			while (t_remainingSpacesToAdd > 0) {
				t_cursor.Write(" ");
				--t_remainingSpacesToAdd;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("TabStop.Evaluate() failed with error: ", t_error);
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

		t_dump.append(p_tabs + "Tag name         :  " + m_name		+ "\n");
		t_dump.append(p_tabs + "Stop type		 :  " + ((m_stopType == STOP_TYPE_STOP) ? STOP_TYPE_LABEL_STOP : STOP_TYPE_LABEL_MARKER) 	+ "\n");
		t_dump.append(p_tabs + "Offset			 :  " + m_offset 	+ "\n");

		return t_dump.toString();
	}
}

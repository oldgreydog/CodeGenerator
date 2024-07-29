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


package codegenerator.generator.utils;



import java.io.*;

import coreutil.logging.*;



/**
 * This is the object passed in the evaluation context that is used by <code><b>text</b></code> tags to write out to the current file or text buffer.
 */
public class Cursor {

	// Data members
	private Writer 			m_writer		= null;
	private StringBuilder	m_currentLine	= new StringBuilder();


	//*********************************
	public Cursor(Writer p_writer) {
		m_writer = p_writer;
	}


	//*********************************
	public boolean Write(String p_newText) {
		try {
			// Since text tags can contain multiple lines, we have to step through the new text and write it out line by line so that we end up in the end with only the remaining text in the cursor.
			int		t_nextNewLineIndex	= 0;
			int		t_lastNewLineIndex	= 0;
			String	t_fragment			= null;

			while ((t_nextNewLineIndex = p_newText.indexOf('\n', t_lastNewLineIndex)) >= 0) {
				t_nextNewLineIndex += 1;	// We want to write the newline with the line so we'll increment the index by one to be sure that we do.
				t_fragment = p_newText.substring(t_lastNewLineIndex, t_nextNewLineIndex);
				//m_currentLine.append(t_fragment);	// There's no point adding the fragment to the current line if we are just going to clear it two lines below.
				m_writer.write(t_fragment);

				m_currentLine.setLength(0);					// Clear the current line buffer for the next line.
				t_lastNewLineIndex = t_nextNewLineIndex;
			}

			// Any part of the p_newText that remains after any complete lines have been written out needs to be captured in the current line.
			if (t_lastNewLineIndex < p_newText.length()) {
				t_fragment = p_newText.substring(t_lastNewLineIndex);
				m_currentLine.append(t_fragment);
				m_writer.write(t_fragment);	// We can't forget to push this fragment out to the writer, too.
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("Cursor.Write() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	public String GetCurrentLineContents() {
		return m_currentLine.toString();
	}
}

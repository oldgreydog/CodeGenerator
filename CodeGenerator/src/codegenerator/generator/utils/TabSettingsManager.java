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


package codegenerator.generator.utils;



public class TabSettingsManager {

	static public  final int		OUTPUT_TYPE_UNDEFINED		= -1;
	static public  final int		OUTPUT_TYPE_TABS			= 1;
	static public  final int		OUTPUT_TYPE_SPACES			= 2;


	// Static members
	// !!!!NOTE!!!! Since the generator is strictly single threaded, these static class members do not require locking around them in this code.
	private int		m_tabSize				= -1;
	private int		m_outputType			= OUTPUT_TYPE_UNDEFINED;
	private int		m_markerColumnNumber	= -1;


	//*********************************
	public TabSettingsManager(int p_tabSize, int p_outputType) {
		m_tabSize		= p_tabSize;
		m_outputType	= p_outputType;
	}


	//*********************************
	public void SetTabSize(int p_tabSize) {
		m_tabSize = p_tabSize;
	}


	//*********************************
	public int GetTabSize() {
		return m_tabSize;
	}


	//*********************************
	public void SetOutputType(int p_outputType) {
		m_outputType = p_outputType;
	}


	//*********************************
	public int GetOutputType() {
		return m_outputType;
	}


	//*********************************
	public void SetMarker(int p_markerColumnNumber) {
		m_markerColumnNumber = p_markerColumnNumber;
	}


	//*********************************
	public int GetMarker() {
		return m_markerColumnNumber;
	}


	//*********************************
	/**
	 * This steps through the current line and counts the columns, substituting the tab size for each tab and handling adjustment for when there is text within a tab stop.
	 * @return
	 */
	public int GetCurrentLineLength(String p_currentLine) {
		int		t_index			= 0;
		int		t_lineLength	= 0;
		boolean t_lastWasTab	= false;
		while (t_index < p_currentLine.length()) {
			if (p_currentLine.charAt(t_index++) == '\t') {
				if (t_lastWasTab) {
					t_lineLength += m_tabSize;
					continue;
				}

				t_lastWasTab = true;
				t_lineLength = ((t_lineLength / m_tabSize) + 1) * m_tabSize;	// When a tab is preceded by some number of characters and since tab stops are fixed and you can type "into" the next tab space up to (s_tabSize - 1) characters, this is the easiest way to calculate the next tab stop position after the end of an arbitrary character string.
				continue;
			}

			t_lastWasTab = false;
			++t_lineLength;			// This may not be internationalization safe if the template charset has characters that are more than one column wide.
		}


		return t_lineLength;
	}



}

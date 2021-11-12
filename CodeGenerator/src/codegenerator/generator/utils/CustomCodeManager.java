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



import java.io.*;
import java.util.*;

import coreutil.logging.*;



/**
 * This is a simple singleton that will scan a file and catalog all of the custom code blocks that it finds so that
 * when the file is regenerated, the custom code tags can generate their keys and then use this manager to find and
 * insert the custom code back into the new file.  Since the code generator is strictly single threaded, this does
 * not require locking.
 *
 * <p>See {@link codegenerator.generator.tags.CustomCode} for more info on how custom code tags work.
 */
public class CustomCodeManager {

	static public final String		START_CUSTOM_CODE		= "StartCustomCode";
	static public final String		END_CUSTOM_CODE			= "EndCustomCode";


	// Static members
	private TreeMap<String, String>		m_keyCodeMap	= new TreeMap<String, String>();


	//*********************************
	public void ClearCache() {
		m_keyCodeMap.clear();	// We have to clear the map when we start a new file.  We do not want to accidently cross-contaminate code into other files.
	}

	//*********************************
	public boolean ScanFile(File p_targetFile) {
		BufferedReader t_lineReader = null;
		try {
			m_keyCodeMap.clear();	// We have to clear the map when we start a new file.  We do not want to accidently cross-contaminate code into other files.

			String			t_line;
			String			t_startKey;
			String			t_endKey;		// Detecting nested "start" of custom code blocks is easy, if someone copied in an "end" custom code line without the matching start, then the only way to know that the "end" is a nesting error is to check its key against the start key we already have.
			StringBuilder	t_customCode	= new StringBuilder();
			int				t_lineCount		= 0;

			t_lineReader = new BufferedReader(new FileReader(p_targetFile));
			while ((t_line = t_lineReader.readLine()) != null) {
				t_lineCount++;
				if (t_line.contains(START_CUSTOM_CODE)) {
					t_startKey = t_line.substring(t_line.indexOf(":") + 1);

					// Now that custom code comments can have closing comment characters, we have to trim them off here so that we have just the key and nothing else.
					if (t_startKey.indexOf("\t") > 0)
						t_startKey = t_startKey.substring(0, t_startKey.indexOf("\t"));

					if (m_keyCodeMap.containsKey(t_startKey)) {
						Logger.LogError("CustomeCodeManager.ScanFile() found a duplicate custom code marker key [" + t_startKey + "] in file [" + p_targetFile.getAbsolutePath() + "] at line [" + t_lineCount + "].  Duplicates are not allowed because that will lead to code loss.");
						return false;
					}

					t_customCode.setLength(0);	// Reset the string builder before each new block of code.
					while ((t_line = t_lineReader.readLine()) != null) {
						t_lineCount++;
						if (t_line.contains(START_CUSTOM_CODE)) {
							Logger.LogError("CustomeCodeManager.ScanFile() found a nested custom code block in file [" + p_targetFile.getAbsolutePath() + "] at line [" + t_lineCount + "].");
							return false;
						}

						if (t_line.contains(END_CUSTOM_CODE)) {
							t_endKey = t_line.substring(t_line.indexOf(":") + 1);

							// Now that custom code comments can have closing comment characters, we have to trim them off here so that we have just the key and nothing else.
							if (t_endKey.indexOf("\t") > 0)
								t_endKey = t_endKey.substring(0, t_endKey.indexOf("\t"));

							if (!t_endKey.equals(t_startKey)) {
								Logger.LogError("CustomeCodeManager.ScanFile() found a nested custom code block end marker key [" + t_startKey + "] in file [" + p_targetFile.getAbsolutePath() + "] at line [" + t_lineCount + "] that doesn't match the start marker key [" + t_startKey + "].");
								return false;
							}

							m_keyCodeMap.put(t_startKey, t_customCode.toString());
							break;
						}

						t_customCode.append(t_line + "\n");
					}
				}
				else if (t_line.contains(END_CUSTOM_CODE)) {
					Logger.LogError("CustomeCodeManager.ScanFile() found a custom code block end marker without a matching start marker in file [" + p_targetFile.getAbsolutePath() + "] at line [" + t_lineCount + "].");
					return false;
				}
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("CustomeCodeManager.ScanFile() failed with error: ", t_error);
			return false;
		}
		finally {
			if (t_lineReader != null)
				try { t_lineReader.close(); } catch (Throwable t_dontCare) {}
		}
	}


	//*********************************
	public String GetCodeSegment(String p_key) {
		return m_keyCodeMap.get(p_key);
	}
}

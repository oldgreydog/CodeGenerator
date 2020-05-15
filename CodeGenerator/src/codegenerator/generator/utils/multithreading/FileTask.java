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


package codegenerator.generator.utils.multithreading;



import codegenerator.generator.tags.*;
import codegenerator.generator.utils.*;
import coreutil.logging.*;



public class FileTask implements Runnable {

	// Data members
	private FileBlock			m_targetFile	= null;
	private EvaluationContext	m_context		= null;


	//*********************************
	public FileTask(FileBlock p_targetFile, EvaluationContext p_context) {
		m_targetFile	= p_targetFile;
		m_context		= p_context;
	}


	//*********************************
	@Override
	public void run() {
		try {
			if (!m_targetFile.TaskEvaluate(m_context)) {
				Logger.LogError("FileTask.run() to execute its file evaluation.");
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("FileTask.run() failed with error: ", t_error);
		}
	}
}

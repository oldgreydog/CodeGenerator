/*
	Copyright 2019 Wes Kaylor

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



import java.util.*;

import coreutil.config.*;



public class OuterContextManager {

	// Data members
	private final TreeMap<String, ConfigNode>	m_contextMap	= new TreeMap<>();


	//*********************************
	public OuterContextManager() {}


	//*********************************
	/**
	 * In shifting to multithreading on the "file" evaluations (and allowing nested file blocks), we have to snap-shot the context so that
	 * we have the correct values while we are in the file evaluation.  If we didn't, the context would be changing outside the file
	 * eval and would completely pollute any usage of those context values inside the file evaluation.
	 *
	 * @param p_otherOuterContextManager
	 */
	public OuterContextManager(OuterContextManager p_otherOuterContextManager) {
		m_contextMap.putAll(p_otherOuterContextManager.m_contextMap);
	}


	//*********************************
	public void SetOuterContext(String p_contextName, ConfigNode p_contextNode) {
		m_contextMap.put(p_contextName, p_contextNode);
	}


	//*********************************
	public ConfigNode GetOuterContext(String p_contextName) {
		return m_contextMap.get(p_contextName);
	}


	//*********************************
	public void RemoveOuterContext(String p_contextName) {
		m_contextMap.remove(p_contextName);
	}
}

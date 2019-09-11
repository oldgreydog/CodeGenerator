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
	static private final TreeMap<String, ConfigNode>	s_contextMap	= new TreeMap<>();


	//===========================================
	static public void SetOuterContext(String p_contextName, ConfigNode p_contextNode) {
		synchronized (s_contextMap) {
			s_contextMap.put(p_contextName, p_contextNode);
		}
	}


	//===========================================
	static public ConfigNode GetOuterContext(String p_contextName) {
		synchronized (s_contextMap) {
			return s_contextMap.get(p_contextName);
		}
	}


	//===========================================
	static public void RemoveOuterContext(String p_contextName) {
		synchronized (s_contextMap) {
			s_contextMap.remove(p_contextName);
		}
	}
}

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



public class LoopCounter {

	// Static members
	static private int		s_idSource	= 0;

	static private synchronized String GetNextID() {
		int t_id = ++s_idSource;
		return Integer.toString(t_id);
	}

	private LoopCounter		m_parentCounter		= null;			// This lets us find name counters by recursing up the parent pointers.
	private String			m_counterID			= GetNextID();
	private int				m_counter			= 1;

	public LoopCounter(LoopCounter p_parentCounter) {
		m_parentCounter = p_parentCounter;
	}

	public void SetOptionalCounterName(String p_optionalCounterName) {
		m_counterID = p_optionalCounterName;
	}

	public LoopCounter GetNamedCounter(String p_optionalCounterName) {
		if (m_counterID.equalsIgnoreCase(p_optionalCounterName))
			return this;

		if (m_parentCounter == null)
			return null;

		return m_parentCounter.GetNamedCounter(p_optionalCounterName);
	}

	public String GetCounterID() {
		return m_counterID;
	}

	public void IncrementCounter() {
		++m_counter;
	}

	public void DecrementCounter() {
		--m_counter;
	}

	public int GetCounter() {
		return m_counter;
	}
}
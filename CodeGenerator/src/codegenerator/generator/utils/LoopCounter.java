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


/**
 * This is used by {@link ForEach} and {@link CounterVariable} to hold their counter values in the {@link EvaluationContext} objects.
 */
public class LoopCounter {

	// Static members
	static private int		s_idSource	= 0;

	static private synchronized int GetNextID() {
		return ++s_idSource;
	}

	// Data members
	private int				m_counterID				= GetNextID();
	private String			m_optionalCounterName	= null;
	private int				m_counter				= 0;			// This needs to be zero so that we can properly control First/Else tags by only incrementing the counter when it should be.  Otherwise, with a default of 1, it looks like the counter and, therefore, any First/Else is "ON" immediately, which is definitely the wrong behavior.


	//*********************************
	public LoopCounter() {}


	//*********************************
	// This was only used when I tried to set up multi-threading of {@link FileTag} objects.  Since I backed that out when I realized it required more context management than I was willing to mess with at the time, this function is no longer needed.  However, since I can't say "Never" on trying multi-threading again, I'm going to leave it hear just in case.
//	private LoopCounter(LoopCounter p_parentCounter) {
//		m_parentCounter			= p_parentCounter.m_parentCounter;
//		m_counterID				= p_parentCounter.m_counterID;
//		m_optionalCounterName	= p_parentCounter.m_optionalCounterName;
//		m_counter				= p_parentCounter.m_counter;
//	}


	//*********************************
	// This was only used when I tried to set up multi-threading of {@link FileTag} objects.  Since I backed that out when I realized it required more context management than I was willing to mess with at the time, this function is no longer needed.  However, since I can't say "Never" on trying multi-threading again, I'm going to leave it hear just in case.
//	/**
//	 * In shifting to multithreading on the "file" evaluations (and allowing nested file tags), we have to snap-shot the parent loop counters so that
//	 * we have the correct static values while we are in the file evaluation.  If we didn't, the parent loop counters would be changing outside the file
//	 * eval and would completely pollute any usage of those parent loop counters inside the file evaluation.
//	 *
//	 * @param p_parentCounter
//	 */
//	public LoopCounter DuplicateCountersForNewFile() {
//		LoopCounter t_newCounter = new LoopCounter(this);
//		if (m_parentCounter != null)
//			t_newCounter.SetParentCounter(m_parentCounter.DuplicateCountersForNewFile());
//
//		return t_newCounter;
//	}


	//*********************************
	public void SetOptionalCounterName(String p_optionalCounterName) {
		m_optionalCounterName = p_optionalCounterName;
	}


	//*********************************
	public String GetOptionalCounterName() {
		return m_optionalCounterName;
	}


	//*********************************
	public int GetCounterID() {
		return m_counterID;
	}


	//*********************************
	public void IncrementCounter() {
		++m_counter;
	}


	//*********************************
	public void DecrementCounter() {
		--m_counter;
	}


	//*********************************
	public int GetCounter() {
		return m_counter;
	}
}

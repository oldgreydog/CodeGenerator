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



import java.util.*;

import coreutil.config.*;
import coreutil.logging.*;



/**
 * This holds everything that needs to be passed down through the execution tree of objects in the Evaluate() function call.
 */
public class EvaluationContext {

	// Data members
	private final LinkedList<ConfigNode>			m_currentNodeStack		= new LinkedList<>();
	private ConfigNode								m_rootNode;
	private final LinkedList<Cursor> 				m_writerStack			= new LinkedList<>();
	private final LinkedList<LoopCounter>			m_iterationCounterStack	= new LinkedList<>();	// There are rare cases (i.e. FirstElse) where we need to grab a named counter from the current counter and set it as the temporary counter for the evaluation of the tag.
	private final TreeMap<String, LoopCounter>		m_counterVariableMap	= new TreeMap<>();		// Since counter variables aren't tied to forEach loops, we need to handle them separately.  If we pushed them onto the forEach loop counter stack, we could potentially seriously poison that stack because any first tags that weren't naming the counter they were working with could possibly use the wrong counter.
	private OuterContextManager						m_contextManager		= null;
	private CustomCodeManager						m_customCodeManager		= null;
	private final LinkedList<TabSettingsManager>	m_tabSettingsManager	= new LinkedList<>();


	//*********************************
	public EvaluationContext(ConfigNode		p_currentNode,
							 ConfigNode		p_rootNode,
							 Cursor 		p_writer,
							 LoopCounter	p_iterationCounter)
	{
		m_currentNodeStack.push(p_currentNode);
		m_rootNode = p_rootNode;
		m_writerStack.push(p_writer);
		m_iterationCounterStack.push(p_iterationCounter);

		m_contextManager		= new OuterContextManager();
		m_customCodeManager		= new CustomCodeManager();
	}


	//*********************************
// This was only used when I tried to set up multi-threading of {@link FileTag} objects.  Since I backed that out when I realized it required more context management than I was willing to mess with at the time, this function is no longer needed.  However, since I can't say "Never" on trying multi-threading again, I'm going to leave it hear just in case.
//	public EvaluationContext(EvaluationContext	p_otherEvaluationContext)
//	{
//		m_currentNodeStack.addAll(p_otherEvaluationContext.m_currentNodeStack);
//
//		m_rootNode			= p_otherEvaluationContext.m_rootNode;
//		//m_writerStack		= ;		// This copy constructor should never be called in a context where we don't create a new Cursor, so we'll skip this member here.
//
//		// In situations where this copy constructor is used, there is no way the new context we are entering can know about the other loop counters that may exist "outside", so we only need the "current" counter from here on.
//		if (!p_otherEvaluationContext.m_iterationCounterStack.isEmpty())
//			m_iterationCounterStack.push(p_otherEvaluationContext.m_iterationCounterStack.getFirst().DuplicateCountersForNewFile());
//
//		// Copy all of the named counter variables.
//		for (Entry<String, LoopCounter> t_nextCounterVariable: p_otherEvaluationContext.m_counterVariableMap.entrySet())
//			m_counterVariableMap.put(t_nextCounterVariable.getKey(), t_nextCounterVariable.getValue().DuplicateCountersForNewFile());
//
//		m_contextManager		= new OuterContextManager(p_otherEvaluationContext.m_contextManager);
//		m_customCodeManager		= new CustomCodeManager();
//		m_tabSettingsManager	= new TabSettingsManager();
//	}


	//*********************************
	public void PushNewCurrentNode(ConfigNode p_newConfigNode) {
		m_currentNodeStack.push(p_newConfigNode);
	}


	//*********************************
	public ConfigNode GetCurrentNode() {
		try {
			if (m_currentNodeStack.isEmpty())
				return null;

			return m_currentNodeStack.getFirst();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.GetCurrentNode() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public void PopCurrentNode() {
		try {
			if (m_currentNodeStack.isEmpty())
				return;

			m_currentNodeStack.pop();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.PopCurrentNode() failed with error: ", t_error);
		}
	}


	//*********************************
	public ConfigNode GetRootNode() {
		return m_rootNode;
	}


	//*********************************
	public void PushNewCursor(Cursor p_cursor) {
		m_writerStack.push(p_cursor);
	}


	//*********************************
	public Cursor GetCursor() {
		try {
			if (m_writerStack.isEmpty())
				return null;

			return m_writerStack.getFirst();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.GetCursor() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public void PopCurrentCursor() {
		try {
			if (m_writerStack.isEmpty())
				return;

			m_writerStack.pop();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.PopCurrentCursor() failed with error: ", t_error);
		}
	}


	//*********************************
	public void PushLoopCounter(LoopCounter p_counter) {
		m_iterationCounterStack.push(p_counter);
	}


	//*********************************
	public LoopCounter GetLoopCounter() {
		try {
			if (m_iterationCounterStack.isEmpty())
				return null;

			return m_iterationCounterStack.getFirst();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.GetLoopCounter() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public void PopCurrentLoopCounter() {
		try {
			if (m_iterationCounterStack.isEmpty())
				return;

			m_iterationCounterStack.pop();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.PopCurrentLoopCounter() failed with error: ", t_error);
		}
	}


	//*********************************
	public void AddCounterVariable(String p_counterName, LoopCounter p_counter) {
		m_counterVariableMap.put(p_counterName, p_counter);
	}


	//*********************************
	public LoopCounter GetCounterVariable(String p_counterName) {
		return m_counterVariableMap.get(p_counterName);
	}


	//*********************************
	public void RemoveCounterVariable(String p_counterName) {
		m_counterVariableMap.remove(p_counterName);
	}


	//*********************************
	public OuterContextManager GetOuterContextManager() {
		return m_contextManager;
	}


	//*********************************
	public CustomCodeManager GetCustomCodeManager() {
		return m_customCodeManager;
	}


	//*********************************
	public void PushTabSettingsManager(TabSettingsManager p_tabManager) {
		m_tabSettingsManager.push(p_tabManager);
	}


	//*********************************
	public int GetTabSettingsManagerStackDepth() {
		return m_tabSettingsManager.size();
	}


	//*********************************
	public TabSettingsManager GetTabSettingsManager() {
		try {
			if (m_tabSettingsManager.isEmpty())
				return null;

			return m_tabSettingsManager.getFirst();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.GetTabSettingsManager() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	public void PopTabSettingsManager() {
		try {
			if (m_tabSettingsManager.isEmpty())
				return;

			m_tabSettingsManager.pop();
		}
		catch (Throwable t_error) {
			Logger.LogException("EvaluationContext.GetTabSettingsManager() failed with error: ", t_error);
		}
	}
}

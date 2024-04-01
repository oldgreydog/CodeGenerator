package codegenerator.generator.utils;


import java.util.LinkedList;

import codegenerator.generator.tags.*;



/**
 * This is a helper class for attribute names and/or values that may have tags in their values that need to be evaluated and that can only
 * happen in the Evaluate() phase.  If the value is otherwise constant, then we can return that value without evaluation.
 */
public class OptionalEvalValue {

	// Data members
	private GeneralBlock	m_value;
	private String			m_constValue;	// If the value does NOT contain any tags that require evaluation, then we can grab that string the first time we go through Evaluate() and cache it here.


	//*********************************
	public OptionalEvalValue(GeneralBlock p_value) {
		m_value = p_value;
	}


	//*********************************
	/**
	 * If the const value has been set, then == TRUE, otherwise false.  If the const value is set the first time through the Evaluate() phase, then this will
	 * obviously become TRUE at that time.
	 * @return
	 */
	public boolean IsConstant() {
		if (m_constValue != null)
			return true;

		return false;
	}


	//*********************************
	public String GetValue() {
		if (m_constValue != null)
			return m_constValue;

		return null;
	}


	//*********************************
	public String Evaluate(EvaluationContext p_evaluationContext) {
		if (m_constValue != null)
			return m_constValue;

		// If there is anything other than TEXT tags in the contents of the value, then it will require evaluation to get the final value every time we come through.
		LinkedList<Tag_Base> t_valueContents = m_value.GetChildTagList();
		for (Tag_Base t_nextContent: t_valueContents) {
			if (!t_nextContent.GetName().equalsIgnoreCase(Text.TAG_NAME))
				return Tag_Base.EvaluateToString(m_value, p_evaluationContext);
		}

		// If we get here, then there were only Text tags in the value so we can just evaluate it once here and return that const value.
		m_constValue = Tag_Base.EvaluateToString(m_value, p_evaluationContext);

		return m_constValue;
	}
}

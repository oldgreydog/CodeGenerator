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


package codegenerator.generator.tags;



import codegenerator.generator.tags.IfElse.*;
import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
	<p>Base class for the tags that will handle the <code><b>and</b></code>, <code><b>or</b></code> or <code><b>not</b></code> or
	other test conditions for <code><b>if</b></code>s.</p>

	<p>Refer to {@link IfElse} for more info on how to use the <code><b>and</b></code>, <code><b>or</b></code> or <code><b>not</b></code>
	tags in an <code><b>if</b></code> tag.</p>
 */
public abstract class If_Boolean extends Tag_Base {

	// Static members
	static private final String		RESULT_TRUE		= "true";
	static private final String		RESULT_FALSE	= "false";


	/**
	 * <p>The <code><b>and</b></code> tag implements the expected boolean logic for use in <code><b>if</b></code> tags with short-circuit
	 * evaluation.  It can have any number and mix of <code><b>and</b></code>, <code><b>or</b></code> or <code><b>not</b></code> boolean tags as attributes.
	 * It can also include <code>[string value] = [string value]</code> boolean attributes.</p>
	 *
	 * <p>The <code><b>and</b></code> returns either "true" or "false" strings.  Since it is used as a tag attribute, it must have the form:</p>
	 *
	 *	<p><code><b>&lt;%and  [two or more boolean attributes] %&gt; = [true|false]</code></p>
	 *
	 * <p>Refer to {@link IfElse} for more info on how to use the <code><b>and</b></code> tag in an <code><b>if</b></code> tag.</p>
	 */
	static public class And extends If_Boolean {

		static public final String	TAG_NAME	= "and";

		public And() {
			super(TAG_NAME);
			m_isSafeForAttributes	= true;
		}

		@Override
		public Tag_Base GetInstance() {
			return new And();
		}

		@Override
		public Boolean EvaluateChild(EvaluationContext p_evaluationContext)
		{
			try {
				if (m_tagList == null) {
					Logger.LogError("And.EvaluateChild() doesn't have any executable content at line [" + m_lineNumber + "].");
					return false;
				}

				Boolean t_result;
				for (Tag_Base t_nextCondition: m_tagList) {
					// Since this is an "AND" operation, we can return FALSE on the first condition that fails.
					t_result = ((IfCondition)t_nextCondition).Test(p_evaluationContext);
					if (t_result == null)
						return null;
					else if (!t_result)
						return false;
				}

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogException("And.EvaluateChild() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return null;
			}
		}
	}


	/**
	 * <p>The <code><b>or</b></code> tag implements the expected boolean logic for use in <code><b>if</b></code> tags with short-circuit
	 * evaluation.  It can have any number and mix of <code><b>and</b></code>, <code><b>or</b></code> or <code><b>not</b></code> boolean tags as attributes.
	 * It can also include <code>[string value] = [string value]</code> boolean attributes.</p>
	 *
	 * <p>The <code><b>or</b></code> returns either "true" or "false" strings.  Since it is used as a tag attribute, it must have the form:</p>
	 *
	 *	<p><code><b>&lt;%or  [two or more boolean attributes] %&gt; = [true|false]</code></p>
	 *
	 * <p>Refer to {@link IfElse} for more info on how to use the <code><b>or</b></code> tag in an <code><b>if</b></code> tag.</p>
	 */
	static public class Or extends If_Boolean {

		static public final String	TAG_NAME	= "or";

		public Or() {
			super(TAG_NAME);
			m_isSafeForAttributes	= true;
		}

		@Override
		public Tag_Base GetInstance() {
			return new Or();
		}

		@Override
		public Boolean EvaluateChild(EvaluationContext p_evaluationContext)
		{
			try {
				if (m_tagList == null) {
					Logger.LogError("Or.EvaluateChild() doesn't have any executable content at line [" + m_lineNumber + "].");
					return false;
				}

				Boolean t_result;
				for (Tag_Base t_nextCondition: m_tagList) {
					// Since this is an "OR" operation, we can return TRUE on the first condition that succeeds.
					t_result = ((IfCondition)t_nextCondition).Test(p_evaluationContext);
					if (t_result == null)
						return null;
					else if (t_result)
						return true;
				}

				return false;
			}
			catch (Throwable t_error) {
				Logger.LogException("Or.EvaluateChild() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return null;
			}
		}
	}


	/**
	 * <p>The <code><b>not</b></code> tag implements the expected boolean logic for use in <code><b>if</b></code> tags.
	 * It can have only one <code><b>and</b></code>, <code><b>or</b></code>, <code><b>not</b></code> boolean tag or one
	 * <code>[string value] = [string value]</code> comparison.</p>
	 *
	 * <p>The <code><b>not</b></code> returns either "true" or "false" strings.  Since it is used as a tag attribute, it must have the form:</p>
	 *
	 *	<p><code><b>&lt;%or  [one boolean attribute] %&gt; = [true|false]</code></p>
	 *
	 * <p>Refer to {@link IfElse} for more info on how to use the <code><b>not</b></code> tag in an <code><b>if</b></code> tag.</p>
	 */
	static public class Not extends If_Boolean {

		static public final String	TAG_NAME	= "not";

		public Not() {
			super(TAG_NAME);
			m_isSafeForAttributes	= true;
		}

		@Override
		public Tag_Base GetInstance() {
			return new Not();
		}

		@Override
		public boolean Init(TagParser p_tagParser) {
			try {
				if (!super.Init(p_tagParser)) {
					Logger.LogError("Not.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
					return false;
				}

				if (m_tagList == null) {
					Logger.LogError("Not.Init() doesn't have any child tags at line [" + m_lineNumber + "].");
					return false;
				}

				if (m_tagList.size() > 1) {
					Logger.LogError("Not.Init() found more than one child condition tag at line number [" + m_lineNumber + "].");
					return false;
				}

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogException("Not.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return false;
			}
		}

		@Override
		public Boolean EvaluateChild(EvaluationContext p_evaluationContext)
		{
			try {
				if (m_tagList == null) {
					Logger.LogError("Not.EvaluateChild() doesn't have any executable content at line [" + m_lineNumber + "].");
					return false;
				}

				Boolean t_result;
				for (Tag_Base t_nextCondition: m_tagList) {
					// Since this is an "Not" operation, we only execute the first condition.
					t_result = ((IfCondition)t_nextCondition).Test(p_evaluationContext);
					if (t_result == null)
						return null;
					else if (t_result)
						return false;

					return true;
				}

				return false;
			}
			catch (Throwable t_error) {
				Logger.LogException("Not.EvaluateChild() failed with error at line number [" + m_lineNumber + "]: ", t_error);
				return null;
			}
		}
	}


	// Data members


	//*********************************
	public If_Boolean(String p_tagName) {
		super(p_tagName);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("If_Boolean.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			IfCondition t_ifCondition;
			for (TagAttributeParser t_nextAttributeParser: p_tagParser.GetTagAttributes()) {
				t_ifCondition = new IfCondition();
				if (!t_ifCondition.Init(t_nextAttributeParser)) {
					Logger.LogError("If_Boolean.Init() failed to initialize the first IfCondition tag at line number [" + m_lineNumber + "].");
					return false;
				}

				AddChildTag(t_ifCondition);
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("If_Boolean.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext) throws Throwable
	{
		try {
			Boolean t_result = EvaluateChild(p_evaluationContext);
			if (t_result == null)
				throw new Exception("If_Boolean.Evaluate() failed to evaluate at line number [" + m_lineNumber + "]");
			else if (t_result)
				p_evaluationContext.GetCursor().Write(RESULT_TRUE);
			else
				p_evaluationContext.GetCursor().Write(RESULT_FALSE);
		}
		catch (Throwable t_error) {
			Logger.LogException("If_Boolean.Evaluate() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			throw t_error;
		}

		return true;
	}


	//*********************************
	public abstract Boolean EvaluateChild(EvaluationContext p_evaluationContext);


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name			+ "\n");

		if (m_tagList != null) {
			for (Tag_Base t_nextTag: m_tagList) {
				t_dump.append("\n\n");
				t_dump.append(t_nextTag.Dump(p_tabs + "\t"));
			}
		}

		return t_dump.toString();
	}
}

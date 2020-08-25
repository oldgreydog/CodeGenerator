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



import codegenerator.generator.tags.IfElseBlock.*;
import codegenerator.generator.utils.*;
import coreutil.logging.*;



/**
	Base class for the tag blocks that will handle the AND, OR or other test conditions for IFs.

	<p>Refer to {@link IfElseBlock} for more info on how to use the AND and OR tags in an IF tag.</p>
 */
public abstract class If_Boolean extends TemplateBlock_Base {

	// Static members
	static public final String		RESULT_TRUE		= "true";
	static public final String		RESULT_FALSE	= "false";


	/**
	 * Refer to {@link IfElseBlock} for more info on how to use the AND and OR tags in an IF tag.
	 */
	static public class And extends If_Boolean {

		static public final String	BLOCK_NAME	= "and";

		public And() {
			super(BLOCK_NAME);
			m_isSafeForTextBlock = true;
		}

		public And(String p_blockName) {
			super(p_blockName);
		}

		@Override
		public TemplateBlock_Base GetInstance() {
			return new And();
		}

		@Override
		public Boolean EvaluateChild(EvaluationContext p_evaluationContext)
		{
			try {
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
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
	 * Refer to {@link IfElseBlock} for more info on how to use the AND and OR tags in an IF tag.
	 */
	static public class Or extends If_Boolean {

		static public final String	BLOCK_NAME	= "or";

		public Or() {
			super(BLOCK_NAME);
			m_isSafeForTextBlock = true;
		}

		public Or(String p_blockName) {
			super(p_blockName);
		}

		@Override
		public TemplateBlock_Base GetInstance() {
			return new Or();
		}

		@Override
		public Boolean EvaluateChild(EvaluationContext p_evaluationContext)
		{
			try {
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
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
	 * Refer to {@link IfElseBlock} for more info on how to use the AND and OR tags in an IF tag.
	 */
	static public class Not extends If_Boolean {

		static public final String	BLOCK_NAME	= "not";

		public Not() {
			super(BLOCK_NAME);
			m_isSafeForTextBlock = true;
		}

		public Not(String p_blockName) {
			super(p_blockName);
		}

		@Override
		public TemplateBlock_Base GetInstance() {
			return new Not();
		}


		//*********************************
		@Override
		public boolean Init(TagParser p_tagParser) {
			try {
				if (!super.Init(p_tagParser)) {
					Logger.LogError("Not.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
					return false;
				}

				if (m_blockList.size() > 1) {
					Logger.LogError("Not.Init() found more than one child condition block at line number [" + m_lineNumber + "].");
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
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
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
	public If_Boolean(String p_blockName) {
		super(p_blockName);
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
					Logger.LogError("If_Boolean.Init() failed to initialize the first IfCondition block at line number [" + m_lineNumber + "].");
					return false;
				}

				m_blockList.add(t_ifCondition);
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

		t_dump.append(p_tabs + "Block type name  :  " + m_name			+ "\n");

		for (TemplateBlock_Base t_nextBlock: m_blockList) {
			t_dump.append("\n\n");
			t_dump.append(t_nextBlock.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}

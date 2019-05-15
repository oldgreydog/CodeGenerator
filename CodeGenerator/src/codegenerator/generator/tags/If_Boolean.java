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


package codegenerator.generator.tags;



import codegenerator.generator.tags.IfElseBlock.*;
import codegenerator.generator.utils.*;
import coreutil.config.*;
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
		}

		public And(String p_blockName) {
			super(p_blockName);
		}

		@Override
		public TemplateBlock_Base GetInstance() {
			return new And();
		}

		@Override
		public Boolean EvaluateChild(ConfigNode		p_currentNode,
									 ConfigNode		p_rootNode,
									 Cursor			p_writer,
									 LoopCounter	p_iterationCounter)
		{
			try {
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
					// Since this is an "AND" operation, we can return FALSE on the first condition that fails.
					t_result = ((IfCondition)t_nextCondition).Test(p_currentNode, p_rootNode, p_iterationCounter);
					if (t_result == null)
						return null;
					else if (!t_result)
						return false;
				}

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogError("And.EvaluateChild() failed with error: ", t_error);
				return false;
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
		}

		public Or(String p_blockName) {
			super(p_blockName);
		}

		@Override
		public TemplateBlock_Base GetInstance() {
			return new Or();
		}

		@Override
		public Boolean EvaluateChild(ConfigNode		p_currentNode,
									 ConfigNode		p_rootNode,
									 Cursor			p_writer,
									 LoopCounter	p_iterationCounter)
		{
			try {
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
					// Since this is an "OR" operation, we can return TRUE on the first condition that succeeds.
					t_result = ((IfCondition)t_nextCondition).Test(p_currentNode, p_rootNode, p_iterationCounter);
					if (t_result == null)
						return null;
					else if (t_result)
						return true;
				}

				return false;
			}
			catch (Throwable t_error) {
				Logger.LogError("Or.EvaluateChild() failed with error: ", t_error);
				return false;
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
				super.Init(p_tagParser);

				if (m_blockList.size() > 1) {
					Logger.LogError("Not.Init() found more than one child condition block.");
					return false;
				}

				return true;
			}
			catch (Throwable t_error) {
				Logger.LogError("If_Boolean.Init() failed with error: ", t_error);
				return false;
			}
		}

		@Override
		public Boolean EvaluateChild(ConfigNode		p_currentNode,
									 ConfigNode		p_rootNode,
									 Cursor			p_writer,
									 LoopCounter	p_iterationCounter)
		{
			try {
				Boolean t_result;
				for (TemplateBlock_Base t_nextCondition: m_blockList) {
					// Since this is an "Not" operation, we only execute the first condition.
					t_result = ((IfCondition)t_nextCondition).Test(p_currentNode, p_rootNode, p_iterationCounter);
					if (t_result == null)
						return null;
					else if (t_result)
						return false;

					return true;
				}

				return false;
			}
			catch (Throwable t_error) {
				Logger.LogError("Or.EvaluateChild() failed with error: ", t_error);
				return false;
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
			IfCondition t_ifCondition;
			for (TagAttributeParser t_nextAttributeParser: p_tagParser.GetTagAttributes()) {
				t_ifCondition = new IfCondition();
				if (!t_ifCondition.Init(t_nextAttributeParser)) {
					Logger.LogError("If_Boolean.Init() failed to initialize the first IfCondition block.");
					return false;
				}

				m_blockList.add(t_ifCondition);
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("If_Boolean.Init() failed with error: ", t_error);
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
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			Boolean t_result = EvaluateChild(p_currentNode, p_rootNode, p_writer, p_iterationCounter);
			if (t_result == null)
				return false;
			else if (t_result)
				p_writer.Write(RESULT_TRUE);
			else
				p_writer.Write(RESULT_FALSE);
		}
		catch (Throwable t_error) {
			Logger.LogError("If_Boolean.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	public abstract Boolean EvaluateChild(ConfigNode	p_currentNode,
										  ConfigNode	p_rootNode,
										  Cursor		p_writer,
										  LoopCounter	p_iterationCounter);


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

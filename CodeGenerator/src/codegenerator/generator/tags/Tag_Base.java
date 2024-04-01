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



import coreutil.logging.*;

import java.io.*;
import java.util.*;

import codegenerator.generator.utils.*;



/**
	The base class API definition for all tag subclasses.  Most tags can contain child
	tags so this also provides the common functionality to add and evaluate child tags.
*/
public abstract class Tag_Base {

	// Data members
	protected	String					m_name					= null;
	protected	boolean					m_isSafeForText			= false;	// This is a simple flag that should elliminate the need to hard-code which tags are allowed inside a Text tag.
	protected	boolean					m_isSafeForAttributes	= false;	// This is a simple flag that lets us mark which tags are allowed inside an attribute's name or value.  This represents a set that does not completely overlap with those that are safe for use in text tags.
	protected	LinkedList<Tag_Base>	m_tagList				= null;		// Tags that have child content should use this list to hold them.
	protected	int						m_state					= -1;

	protected	int						m_lineNumber			= -1;		// The line number in the template file where this instance of a tag was defined.



	//*********************************
	public Tag_Base() {
		m_name = null;
	}


	//*********************************
	public Tag_Base(String p_name) {
		m_name = p_name;
	}


	//*********************************
	public boolean IsSafeForText() {
		return m_isSafeForText;
	}


	//*********************************
	public boolean IsSafeForAttributes() {
		return m_isSafeForAttributes;
	}


	//*********************************
	/**
	 * The child class can grab any attributes from the tag parser to initialize itself.
	 *
	 * @param p_tagParser Generated by the calling code to parse this tag.  It contains all of the attribute definitions from the tag.
	 * @return
	 */
	public boolean Init(TagParser p_tagParser) {
		m_lineNumber = p_tagParser.GetLineNumber();

		return true;
	}


	//*********************************
	/**
	 * Used by {@link TagFactory} to get a new instance of this handler class when it finds one that handles the requested tag.  This lets the instances held by the {@link TagFactory} to act as Prototype-pattern objects.
	 *
	 * @return
	 */
	public abstract Tag_Base GetInstance();


	//*********************************
	public String GetName() {
		return m_name;
	}


	//*********************************
	/**
	 * As a child class that represents a tag that can contain other tags parses its content, it will
	 * add each new tag handler class here so that they can be evaluated in the original order.
	 *
	 * @param p_childTag The newly parsed child tag handler.
	 */
	public void AddChildTag(Tag_Base p_childTag) {
		if (m_tagList == null)
			m_tagList = new LinkedList<Tag_Base>();

		m_tagList.add(p_childTag);
	}


	//*********************************
	/**
	 * There may be cases where a tag like GeneralBlock will be used for things like parsing attribute
	 * names and that code may need to check to see if, for example, whether the attribute name is a
	 * simple text tag or a mixed value of text and tags.
	 *
	 */
	public LinkedList<Tag_Base> GetChildTagList() {
		return m_tagList;
	}


	//*********************************
	/**
	 * For tags that can contain other tags, the child class will implement this to parse through
	 * that child content.
	 *
	 * @param p_tokenizer	Tokenizes the template input file so that it is easily processed by the handlers.
	 * @return
	 */
	public abstract boolean Parse(TemplateTokenizer p_tokenizer);


	//*********************************
	/**
	 * Creates the output for each node.  The base class implementation iterates through all of the
	 * child nodes that the child class implementation attached to itself during the Parse().  If a
	 * child class needs a different evaluation, then it can override this function.
	 *
	 * @paramp_evaluationContext
	 * @return
	 * @throws Throwable
	 */
	public boolean Evaluate(EvaluationContext p_evaluationContext) throws Throwable
	{
		try {
			// If this tag doesn't have child content, then this should return false since the child tag should have overridden this function and we should never have gotten here.
			if (m_tagList == null) {
				Logger.LogError("Tag_Base.Evaluate() for tag [" + m_name + "] at line [" + m_lineNumber + "] did not have child content.  This function should have been overridden by the child class.");
				return false;
			}

			for (Tag_Base t_nextTag: m_tagList) {
				if (!t_nextTag.Evaluate(p_evaluationContext)) {
					return false;
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("Tag_Base.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	/**
	 * This is a helper function that can be used to evaluate attribute names and values and other instances where you know you need the string value locally and not in the p_writer stream.
	 * @param p_contentsToEvaluate
	 * @param p_evaluationContext
	 * @return
	 */
	static public String EvaluateToString(Tag_Base				p_contentsToEvaluate,
										  EvaluationContext		p_evaluationContext)
	{
		try {
			if (p_contentsToEvaluate == null) {
				Logger.LogError("Tag_Base.EvaluateToString() received no contents to evaluate.");
				return null;
			}

			StringWriter		t_valueWriter	= new StringWriter();
			Cursor				t_valueCursor	= new Cursor(t_valueWriter);

			p_evaluationContext.PushNewCursor(t_valueCursor);

			if (!p_contentsToEvaluate.Evaluate(p_evaluationContext)) {
				p_evaluationContext.PopCurrentCursor();	// We need to throw away the temp cursor now that we're done with it.
				return null;
			}

			p_evaluationContext.PopCurrentCursor();	// We need to throw away the temp cursor now that we're done with it.

			return t_valueWriter.toString();
		}
		catch (Throwable t_error) {
			Logger.LogException("Tag_Base.EvaluateToString() failed with error: ", t_error);
			return null;
		}
	}


	//*********************************
	/**
	 * Output descriptive information about the tag for debugging purposes.  The output will show
	 * the tree representation of all of the tag objects parsed from the template.
	 * @param p_tabs	A string containing the tab whitespace to be used in front of each line of
	 * the output so that the parent and child tag definitions are spaced in a proper tree format.
	 *
	 * @return The string representation of this node's definition and the definitions of all of its
	 * child nodes.
	 */
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		if (m_name != null)
			t_dump.append(p_tabs + "Tag name        :  " + m_name + "\n");

		if (m_tagList != null) {
			for (Tag_Base t_nextTag: m_tagList)
				t_dump.append("\n\n" + t_nextTag.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}

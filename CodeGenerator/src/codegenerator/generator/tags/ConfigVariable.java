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



import codegenerator.generator.utils.*;
import coreutil.config.*;
import coreutil.logging.*;


/**
	Accesses values in the config tree so that they can be inserted into the template output.  (For
	an explanation of the config tree and how it is traversed, please see {@link ForEach}.)

	<p>There are three modes of use for this tag:</p>

	<p>	- Simple variable: a single child variable name is looked for on the current parent node</p>

	<p>	- Parent references: the caret ( ^ ) is used one or more times before the variable name to
	indicate that it should be found that number of parent nodes above the current parent</p>

	<p>	- Global path name: A fully specified variable name that starts with "root." will be dereferenced from the root node</p>

	<h3>Examples</h3>

	<p>We'll use this example config as the reference point:</p>

	<pre><code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Node name="root"&gt;
	&lt;Node name="global"&gt;
		&lt;Value name="databaseName"&gt;Operations&lt;/Value&gt;
		&lt;Value name="packageName"&gt;codegenerator.examples.operations&lt;/Value&gt;
		&lt;Value name="outputPath"&gt;/home/dev/temp/code_generator/operations_db&lt;/Value&gt;
	&lt;/Node&gt;
	&lt;Node name="table"&gt;
		&lt;Value name="className"&gt;User&lt;/Value&gt;
		&lt;Value name="sqlName"&gt;USER&lt;/Value&gt;
		&lt;Node name="column"&gt;
			&lt;Value name="name"&gt;UserId&lt;/Value&gt;
			&lt;Value name="sqlName"&gt;USER_ID&lt;/Value&gt;
			&lt;Value name="memberName"&gt;userId&lt;/Value&gt;
			&lt;Value name="type"&gt;int&lt;/Value&gt;
			&lt;Value name="isNullable"&gt;false&lt;/Value&gt;
			&lt;Value name="isPrimaryKey"&gt;true&lt;/Value&gt;
		&lt;/Node&gt;
		&lt;Node name="column"&gt;
			&lt;Value name="name"&gt;LoginName&lt;/Value&gt;
			&lt;Value name="sqlName"&gt;LOGIN_NAME&lt;/Value&gt;
			&lt;Value name="memberName"&gt;loginName&lt;/Value&gt;
			&lt;Value name="type"&gt;varchar&lt;/Value&gt;
			&lt;Value name="valueMaxSize"&gt;50&lt;/Value&gt;
			&lt;Value name="isNullable"&gt;false&lt;/Value&gt;
			&lt;Value name="isPrimaryKey"&gt;false&lt;/Value&gt;
		&lt;/Node&gt;

		...

	&lt;/Node&gt;

	...

&lt;/Node&gt;
</code></pre>

	<p>Let's assume that we are at a point in the template such that the current node pointer is at the
	first "member" node under the "class" node.</p>

	<p>	- A simple variable reference like <code>&lt;%sqlName%&gt;</code> will be evaluated to <b><code>USER_ID</code></b>.</p>

	<p>	- A variable reference with one parent reference caret like <code>&lt;%^sqlName%&gt;</code> will be evaluated to <b><code>USER</code></b>.</p>

	<p>	- A fully qualified variable reference like <code>&lt;%root.global.databaseName%&gt;</code> will be evaluated to <b><code>Operations</code></b>.</p>
 */
public class ConfigVariable extends Tag_Base {

	static public final String		TAG_NAME		= "ConfigVariable";


	// Data members
	private	String		m_variableName			= null;
	private	int			m_parentReferenceCount	= 0;		// (i.e. "^varname") This count tells us how many levels up to go to reference the following variable name on a parent node (or a parent-of-a-parent node "^^varname", etc.)


	//*********************************
	public ConfigVariable() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		// This should never be called for this class.
		return false;
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new ConfigVariable();
	}


	//*********************************
	public boolean Init(TagParser p_tagParser, int p_lineNumber) {
		if (!p_tagParser.GetTagAttributes().isEmpty()) {
			Logger.LogError("ConfigVariable.Init() was handed a tag definition for [" + p_tagParser.GetTagName() + "] at line number [" + p_tagParser.GetLineNumber() + "] that has [" + p_tagParser.GetTagAttributes().size() + "] attribute(s) and is, therefore, not a config variable name.");
			return false;
		}

		if (!super.Init(p_tagParser)) {
			Logger.LogError("ConfigVariable.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "]");
			return false;
		}

		String t_variableName = p_tagParser.GetTagName();
		if (t_variableName == null) {
			Logger.LogError("ConfigVariable.Init() did not find the required variable name at line [" + p_lineNumber + "].");
			return false;
		}

		return Init(t_variableName, p_lineNumber);
	}


	//*********************************
	public boolean Init(String p_variableName, int p_lineNumber) {
		// Count (and remove) any parent references.
		String t_variableName = p_variableName;
		while (t_variableName.startsWith("^")) {
			m_parentReferenceCount++;
			t_variableName = t_variableName.substring(1);	// Chop off the leading ^.
		}

		m_variableName	= t_variableName;
		m_lineNumber	= p_lineNumber;

		return true;
	}



	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		// All configuration of this node should now happen in Init().
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)

	{
		try {
			ConfigNode	t_currentNode	= p_evaluationContext.GetCurrentNode();
			String		t_variableName	= m_variableName;
			if (t_variableName.startsWith("root.")) {
				t_currentNode = p_evaluationContext.GetRootNode();
				t_variableName.replace("root.", "");	// Remove the "root." reference so that the variable name will work correctly below.
			}
			else if (m_parentReferenceCount > 0) {
				// This will kick the node reference up the tree the specified number of times.
				for (int i = 0; i < m_parentReferenceCount; i++) {
					if (t_currentNode == null) {
						Logger.LogError("ConfigVariable.Evaluate() appears to have too many parent references [" + m_parentReferenceCount + "] to the variable named [" + m_variableName + "] at line [" + m_lineNumber + "].  It ran off the top of the tree.");
						return false;
					}

					t_currentNode = t_currentNode.GetParentNode();
				}
			}

			if (t_currentNode == null) {
				Logger.LogError("ConfigVariable.Evaluate() appears to have too many parent references [" + m_parentReferenceCount + "] to the variable named [" + m_variableName + "] at line [" + m_lineNumber + "].  It ran off the top of the tree.");
				return false;
			}

			String t_value = t_currentNode.GetNodeValue(m_variableName);
			if (t_value == null) {
				Logger.LogError("ConfigVariable.Evaluate() could not find the variable named [" + m_variableName + "] defined in line [" + m_lineNumber + "] in either the current or root config nodes.");
				return false;
			}

			p_evaluationContext.GetCursor().Write(t_value);
		}
		catch (Throwable t_error) {
			Logger.LogException("ConfigVariable.Evaluate() failed with error at line [" + m_lineNumber + "]: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name 					+ "\n");
		t_dump.append(p_tabs + "Variable name    :  " + m_variableName			+ "\n");
		t_dump.append(p_tabs + "Parent Ref Count :  " + m_parentReferenceCount	+ "\n");

		return t_dump.toString();
	}
}

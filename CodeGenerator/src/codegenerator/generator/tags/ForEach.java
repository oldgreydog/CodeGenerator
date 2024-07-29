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



import coreutil.config.*;
import coreutil.logging.*;

import codegenerator.generator.utils.*;



/**
<p>Provides the looping mechanism that iterates over config child nodes of the specified name.</p>

	<pre><code><b>&lt;%forEach value = column %&gt;
...
&lt;%endFor%&gt;</b></code></pre>

<p>This tag is the main flow control in the templates.  The config values are defined in a tree structure
in the XML file and the <code><b>forEach</b></code> tag lets you build templates that can iterate down
through that tree structure.  That XML structure is made up of <code><b>node</b></code>(s) and <code><b>value</b></code>(s).  Think of the nodes
as file folders and the values as files.  Like a folder tree, each node can hold any number of child nodes
and values.  In the vast majority of cases, the values are like a class's attributes and are, therefore,
uniquely named in each node.  But occasionally you need a list of values that have the same name and, in
that case, you can use the <code><b>value</b></code> attribute instead of the <code><b>node</b></code> attribute to tell the <code><b>forEach</b></code>
tag that you want it to iterate over the child values of the specified name instead of child nodes.</p>

<p>When you start the code generator, the current node pointer is pointed at the
root node of the config value tree.  The evaluation of each template file is started with whatever
the current node pointer is pointed at at the start of the <code>file</code> tag.  For the <code><b>root</b></code> template
file that is passed into the generator, it's the <code><b>root</b></code> node in the config file.</p>

<p>Each node in the config value tree can have both values and child nodes.  That means that at any
point in a template, you can access the values under the current node or you can use the <code><b>forEach</b></code>
tag to start a loop over that node's child nodes.  Inside the <code><b>forEach</b></code> tag, the current
pointer is changed every iteration to point to the next available child of the specified name.
If there are child nodes with different names, only the ones with the name specified in the tag will
be iterated over in that particular <code><b>forEach</b></code> tag.  Therefore, if you have multiple
child node groups, you can use multiple <code><b>forEach</b></code> tags to iterate over them.</p>

<p>A critical feature of the <code><b>forEach</b></code> tag is that it maintains an internal counter of how
many child nodes it has iterated over.  This is the primary counter used by {@link FirstElse} tags so that they
can figure out which iteration they are seeing and can choose to execute either the <code><b>first</b></code> block or the <code><b>else</b></code>
block.  These iterator values can also be output in the generated text for various reasons (i.e. creating primary
key values for default data that will be inserted into a database).  You can use the optional <code><b>optionalCounterName</b></code>
attribute to add a name for a particular <code><b>forEach</b></code> tag's counter so that you can access it where there
are nested <code><b>forEach</b></code> tags and the one you want to use is not the inner most <code><b>forEach</b></code> tag enclosing
the point where you are accessing the couter.</p>

<h3>Usage example</h3>

<p>Let's start with an example of a config value XML tree (truncated in places for brevity):</p>

	<pre><code><b>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Node name="root"&gt;
	&lt;Node name="global"&gt;
		&lt;Value name="databaseName"&gt;OpsDB&lt;/Value&gt;
		&lt;Value name="packageName"&gt;coreutil.model.opsdb&lt;/Value&gt;
		&lt;Value name="outputPath"&gt;~/temp/code_generator/ArchDev&lt;/Value&gt;
		...
	&lt;/Node&gt;

	&lt;Node name="table">
		&lt;Value name="sqlName">DEVICE_PROPERTY&lt;/Value&gt;
		&lt;Value name="className">DeviceProperty&lt;/Value&gt;
		&lt;Value name="parameterName">deviceProperty&lt;/Value&gt;

		&lt;Node name="column">
			&lt;Value name="sqlName">DEVICE_PROPERTY_ID&lt;/Value&gt;
			&lt;Value name="name">DevicePropertyId&lt;/Value&gt;
			&lt;Value name="parameterName">devicePropertyId&lt;/Value&gt;
			&lt;Value name="type">int&lt;/Value&gt;
			&lt;Value name="valueMaxSize">&lt;/Value&gt;
			&lt;Value name="isNullable">false&lt;/Value&gt;
			&lt;Value name="isPrimaryKey">true&lt;/Value&gt;
			&lt;Value name="isKeyGenerated">true&lt;/Value&gt;

			<!--	StartCustomCode:DeviceProperty_DevicePropertyId_Misc	-->
			<!--	EndCustomCode:DeviceProperty_DevicePropertyId_Misc	-->
		&lt;/Node&gt;
		&lt;Node name="column">
			&lt;Value name="sqlName">DEVICE_ID&lt;/Value&gt;
			&lt;Value name="name">DeviceId&lt;/Value&gt;
			&lt;Value name="parameterName">deviceId&lt;/Value&gt;
			&lt;Value name="type">int&lt;/Value&gt;
			&lt;Value name="valueMaxSize">&lt;/Value&gt;
			&lt;Value name="isNullable">false&lt;/Value&gt;
			&lt;Value name="isPrimaryKey">false&lt;/Value&gt;
			&lt;Node name="foreignKey">
				&lt;Value name="parentTableName">DEVICE&lt;/Value&gt;
				&lt;Value name="parentColumnName">DEVICE_ID&lt;/Value&gt;
			&lt;/Node&gt;
		&lt;/Node&gt;
		&lt;Node name="column"&gt;
			...
		&lt;/Node&gt;
		...
	&lt;/Node&gt;
&lt;/Node&gt;</b></code></pre>

<p>Heres an example "top" template that might be passed to the generator on startup (a simplified version of one
from the Examples folders):</p>

<pre><code><b>
%%HEADER%% openingDelimiter=&lt;% closingDelimiter=%&gt;

&lt;%forEach node=table%&gt;
	&lt;%file template=database_class.template                      filename="&lt;%name%&gt;.java"                       destDir="&lt;%root.global.outputPath%&gt;"%&gt;
&lt;%endFor%&gt;

&lt;%file template=marshalling/marshalling_factory.template         filename="&lt;%root.global.databaseName%&gt;MarshallingFactory.java"  destDir="&lt;%root.global.outputPath%&gt;/marshalling"%&gt;</b></code>

</pre>

<p>When the generator starts up with these two files, the current node pointer points to <code><b>root</b></code>.
That means that when the template evaluation starts, and at any point outside the <code><b>forEach</b></code>
tag, the current pointer for this template is <code><b>root</b></code>.  However, inside this template's <code><b>forEach</b></code>
the current node pointer will iterate over each root child node that has the name <code><b>table</b></code>.  The
target child node name is specified as the <code><b>node</b></code> attribute value in the <code><b>forEach</b></code> tag.
In this example, that looks like:</p>

<p><code><b>&lt;%forEach node=table%&gt;</b></code></p>

<p>After <code><b>forEach</b></code> has iterated over all of the <code><b>table</b></code> child nodes that it can find,
it exits its block at the <code><b>endFor</b></code> tag and from there the current node is again <code><b>root</b></code>.</p>

<p>However, all of the <code>file</code> tags inside the <code><b>forEach</b></code> tag start their
evaluation with a current pointer pointing to a <code><b>table</b></code> node, not <code><b>root</b></code>.  So when you are looking
at any of those child template files, you have to always keep that in mind.  All of the config references
in those inner template files start with a <code><b>table</b></code> node as their current node,
not whatever outer current node is seen by the parent (which itself may be a child of some other parent node).  Looking at the
examples in the <code><b>Examples/codegenerator</b></code> folders is the best way to understand this.</p>

<h3>Attribute descriptions</h3>

<p>NOTE!!!  Only one of either the <code><b>node</b></code> or <code><b>value</b></code> attributes can be used!  If both attributes are used at the same
time, the generator will exit with an error.</p>

<p><code><b>node</b></code>:  specifies that the <code><b>forEach</b></code> will iterate over child nodes of the given name.</p>

<p><code><b>value</b></code>:  specifies that the <code><b>forEach</b></code> will iterate over child values of the given name.</p>

<p><code><b>optionalCounterName</b></code>:  It lets you give the <code><b>forEach</b></code> tag's counter
a user-controlled name so that it can be accessed by name inside nested <code><b>forEach</b></code> tags.  For example, if you have
two nested <code><b>forEach</b></code> loops and you want a <code><b>first</b></code> tag in the inner loop to only run the first time that
the outer <code><b>forEach</b></code> runs regardless of how many times the inner loop has stepped through, then you use the
<code><b>optionalCounterName</b></code> on the outer <code><b>forEach</b></code> as the <code><b>optionalCounterName</b></code> on the
inner <code><b>first</b></code> tag.</p>

	<pre><code><b>&lt;%forEach node = table  optionalCounterName = "tableCounter" %&gt;
	&lt;%forEach value = column %&gt;
		&lt;%first optionalCounterName = tableCounter %&gt;
		...
		&lt;%else%&gt;
		...
		&lt;%endFirst%&gt;
	&lt;%endFor%&gt;
&lt;%endFor%&gt;</b></code></pre>
 */
public class ForEach extends Tag_Base {

	static public final String		TAG_NAME							= "forEach";
	static public final String		TAG_END_NAME						= "endFor";

	static private final String		ATTRIBUTE_NODE						= "node";
	static private final String		ATTRIBUTE_VALUE						= "value";
	static private final String		ATTRIBUTE_OPTIONAL_COUNTER_NAME		= "optionalCounterName";


	enum CONFIG_TYPE { NODE, VALUE };

	// Data members
	private	String		m_nodeName;										// This is the name of the config node that will be the temporary "root" node for each iteration of the loop.  For example, if this is == "class", then when we enter Evaluate(), we will run through the loop once for each "class" child node we find on the passed-in p_currentNode.
	private CONFIG_TYPE	m_configType			= CONFIG_TYPE.NODE;		// This tells us whether we are evaluating NODEs or VALUEs.
	private	int			m_parentReferenceCount	= 0;					// (i.e. "^nodename") This count tells us how many levels up to go to reference the following variable name on a parent node (or a parent-of-a-parent node "^^varname", etc.)
	private	String		m_optionalCounterName	= null;					// Providing a name for the loop counter lets you access it inside inner loops to achieve finer control over first tags and other counter uses.


	//*********************************
	public ForEach() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public ForEach GetInstance() {
		return new ForEach();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("ForEach.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			TagAttributeParser t_nodeAttribute	= p_tagParser.GetNamedAttribute(ATTRIBUTE_NODE);
			TagAttributeParser t_valueAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_VALUE);
			if ((t_nodeAttribute == null) && (t_valueAttribute == null)) {
				Logger.LogError("ForEach.Init() did not find either the [" + ATTRIBUTE_NODE + "] or [" + ATTRIBUTE_VALUE + "] attributes, one of which is required for [" + TAG_NAME + "] tags.");
				return false;
			}
			else if ((t_nodeAttribute != null) && (t_valueAttribute != null)) {
				Logger.LogError("ForEach.Init() found both the [" + ATTRIBUTE_NODE + "] or [" + ATTRIBUTE_VALUE + "] attributes.  Only one or the other can be used per [" + TAG_NAME + "] tag.");
				return false;
			}

			if (t_nodeAttribute == null) {
				t_nodeAttribute = t_valueAttribute;
				m_configType = CONFIG_TYPE.VALUE;
			}
			// m_configType defaults to CONFIG_TYPE.NODE, so we don't need to set it here.

			m_nodeName = t_nodeAttribute.GetAttributeValueAsString();
			if ((m_nodeName == null) || m_nodeName.isBlank()) {
				Logger.LogError("ForEach.Init() did not get the value from the attribute [" + ((m_configType == CONFIG_TYPE.NODE) ? ATTRIBUTE_NODE : ATTRIBUTE_VALUE) + "] that is required for [" + TAG_NAME + "] tags.");
				return false;
			}

			// Count (and remove) any parent references.
			while (m_nodeName.startsWith("^")) {
				++m_parentReferenceCount;
				m_nodeName = m_nodeName.substring(1);	// Chop off the leading ^.
			}


			// The attribute "optionalCounterName" is, obviously, optional, so we need to handle it that way.
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_COUNTER_NAME);
			if (t_nodeAttribute != null) {
				m_optionalCounterName = t_nodeAttribute.GetAttributeValueAsString();
				if (m_optionalCounterName == null) {
					Logger.LogError("ForEach.Init() did not get the value from attribute [" + ATTRIBUTE_OPTIONAL_COUNTER_NAME + "] that is required for [" + TAG_NAME + "] tags.");
					return false;
				}
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("ForEach.Init() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("ForEach.Parse() general block parser failed at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].");
				return false;
			}

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase(TAG_END_NAME)) {
				Logger.LogError("ForEach.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "].  The closing tag [" + TAG_END_NAME + "] was expected.");
				return false;
			}

			AddChildTag(t_generalBlock);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("ForEach.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "] in the tag starting at [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			if (m_tagList == null) {
				Logger.LogError("ForEach.Evaluate() doesn't have any executable content at line [" + m_lineNumber + "].");
				return false;
			}

			// You can use parent references (i.e. "^") in the node name to jump this ForEach tag's context up one or more parent nodes.
			// This will probably always be used inside one or more OuterContext tags.
			ConfigNode t_currentNode = p_evaluationContext.GetCurrentNode();
			if (m_parentReferenceCount > 0) {
				// This will kick the node reference up the tree the specified number of times.
				for (int i = 0; i < m_parentReferenceCount; i++) {
					if (t_currentNode == null) {
						Logger.LogError("ForEach.Evaluate() appears to have too many parent references [" + m_parentReferenceCount + "] to the node name [" + m_nodeName + "].  It ran off the top of the tree.");
						return false;
					}

					t_currentNode = t_currentNode.GetParentNode();
				}
			}

			if (t_currentNode == null) {
				Logger.LogError("ForEach.Evaluate() appears to have too many parent references [" + m_parentReferenceCount + "] to the node name [" + m_nodeName + "].  It ran off the top of the tree.");
				return false;
			}

			LoopCounter t_iterationCount = new LoopCounter();

			if (m_optionalCounterName != null)
				t_iterationCount.SetOptionalCounterName(m_optionalCounterName);

			p_evaluationContext.PushLoopCounter(t_iterationCount);	// This PushLoopCounter() has to be bookended with a matching PopCurrentLoopCounter() below!

			if (m_configType == CONFIG_TYPE.NODE) {
				for (ConfigNode t_nextConfigNode: t_currentNode.GetChildNodeList()) {
					// For each child config node of the name t_nodeName, we will re-evaluate all of our child tags.
					if (t_nextConfigNode.GetName().compareToIgnoreCase(m_nodeName) == 0) {
						t_iterationCount.IncrementCounter();	// Now that I've changed LoopCounter to default to 0 so that it works correctly with CounterVariable, then we need to increment it here at the start of the if() instead of the end of the if().
						p_evaluationContext.PushNewCurrentNode(t_nextConfigNode);

						for (Tag_Base t_nextTag: m_tagList) {
							if (!t_nextTag.Evaluate(p_evaluationContext)) {
								p_evaluationContext.PopCurrentNode();
								p_evaluationContext.PopCurrentLoopCounter();
								return false;
							}
						}

						p_evaluationContext.PopCurrentNode();
					}
				}
			}
			else if (m_configType == CONFIG_TYPE.VALUE) {
				for (coreutil.config.ConfigValue t_nextConfigValue: t_currentNode.GetChildValueList()) {
					// For each child config node of the name t_nodeName, we will re-evaluate all of our child tags.
					if (t_nextConfigValue.GetName().compareToIgnoreCase(m_nodeName) == 0) {
						t_iterationCount.IncrementCounter();	// Now that I've changed LoopCounter to default to 0 so that it works correctly with CounterVariable, then we need to increment it here at the start of the if() instead of the end of the if().
						p_evaluationContext.SetCurrentValue(t_nextConfigValue);

						for (Tag_Base t_nextTag: m_tagList) {
							if (!t_nextTag.Evaluate(p_evaluationContext)) {
								p_evaluationContext.ClearCurrentValue();
								p_evaluationContext.PopCurrentLoopCounter();
								return false;
							}
						}

						p_evaluationContext.ClearCurrentValue();
					}
				}
			}

			p_evaluationContext.PopCurrentLoopCounter();
		}
		catch (Throwable t_error) {
			Logger.LogException("ForEach.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name        :  " + m_name		+ "\n");
		t_dump.append(p_tabs + "Node name       :  " + m_nodeName	+ "\n");

		if (m_tagList != null) {
			for (Tag_Base t_nextTag: m_tagList)
				t_dump.append("\n\n" + t_nextTag.Dump(p_tabs + "\t"));
		}

		return t_dump.toString();
	}
}

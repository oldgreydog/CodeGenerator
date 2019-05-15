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
	<p>Provides the looping mechanism that iterates over child nodes of the specified name.</p>

	<pre><code>&lt;%foreach node=column %&gt;
...
&lt;%endfor%&gt;</code></pre>

	<p>This tag is the main control in the templates.  The config values are defined in a tree structure
	in the XML file and the <code>foreach</code> tag lets you build templates that can iterate down
	through that tree structure.</p>

	<p>When you start the code generator, the "current" node pointer is, conceptually, pointed at the
	root node of the config value tree.  The evaluation of each template file is started with whatever
	the current node pointer is pointed at at the start of the <code>file</code> tag or at "root" for
	the template file that the code generator was started with.</p>

	<p>Each node in the config value tree can have both values and child nodes.  That means that at any
	point in a template, you can access the values under the "current" node or you can use the <code>foreach</code>
	tag to start a loop over that node's child nodes.  Inside the <code>foreach</code> block, the current
	pointer is changed every iteration to point to the next available child of the specified name.
	If there are child nodes with different names, only the ones with the name specified in the tag will
	be iterated over in that particular <code>foreach</code> block.  Therefore, if you have multiple
	child node groups, you can use multiple <code>foreach</code> blocks to iterate over them.</p>

	<p>Let's start with an example of a config value XML tree (truncated in places for brevity):</p>

	<pre><code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Node name="root"&gt;
	&lt;Node name="global"&gt;
		&lt;Value name="databaseName"&gt;OpsDB&lt;/Value&gt;
		&lt;Value name="packageName"&gt;coreutil.model.opsdb&lt;/Value&gt;
		...
	&lt;/Node&gt;
	&lt;Node name="table"&gt;
		&lt;Value name="tableName"&gt;User&lt;/Value&gt;
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
			...
		&lt;/Node&gt;
		...

		&lt;Node name="foreignKey"&gt;
			&lt;Value name="name"&gt;CustomerId&lt;/Value&gt;
			&lt;Value name="childKeyColumnName"&gt;CUSTOMER_ID&lt;/Value&gt;
			&lt;Value name="type"&gt;int&lt;/Value&gt;
		&lt;/Node&gt;
		&lt;Node name="foreignKey"&gt;
			...
		&lt;/Node&gt;
		...
	&lt;/Node&gt;
	&lt;Node name="table"&gt;
		&lt;Node name="column"&gt;
			...
		&lt;/Node&gt;
		...

		&lt;Node name="foreignKey"&gt;
			...
		&lt;/Node&gt;
		...
	&lt;/Node&gt;
&lt;/Node&gt;</code></pre>

	<p>This may not be a perfect example since you might not normally represent the foreign keys
	separately from the columns, but this worked for the way the templates shown in the Examples
	were set up.  And it serves my purposes here.  Anyway, on to an example "root" template that
	might be passed to the generator on startup (a slightly tweaked version of one from the Examples
	folders):</p>

	<pre><code>
%%HEADER%% openingDelimiter=&lt;% closingDelimiter=%&gt;

// This is dummy text to make sure the parser ignores it since it isn't in a text block.
&lt;%foreach node=table%&gt;
	&lt;%file template=database_class.template                      filename=&lt;%tableName%&gt;.java                       destDir=&lt;%global.outputPath%&gt;%&gt;

	&lt;%file template=marshalling/marshalling_interface.template   filename=&lt;%tableName%&gt;Marshalling.java            destDir=&lt;%global.outputPath%&gt;/marshalling%&gt;
	&lt;%file template=marshalling/marshalling_gson.template        filename=Gson&lt;%tableName%&gt;Marshalling.java        destDir=&lt;%global.outputPath%&gt;/marshalling/gson%&gt;
	&lt;%file template=marshalling/marshalling_dmapi.template       filename=DMAPIJson&lt;%tableName%&gt;Marshalling.java   destDir=&lt;%global.outputPath%&gt;/marshalling/dmapijson%&gt;

	&lt;%file template=dao/dao_interface.template                   filename=&lt;%tableName%&gt;DAO.java                    destDir=&lt;%global.outputPath%&gt;/dao%&gt;
	&lt;%file template=dao/dao_db.template                          filename=&lt;%tableName%&gt;DAO_DB.java                 destDir=&lt;%global.outputPath%&gt;/dao/db%&gt;
	&lt;%file template=dao/dao_cache.template                       filename=&lt;%tableName%&gt;DAO_Cache.java              destDir=&lt;%global.outputPath%&gt;/dao/cache%&gt;

	&lt;%file template=dao/dao_net_client.template                  filename=&lt;%tableName%&gt;DAO_NET.java                destDir=&lt;%global.outputPath%&gt;/dao/net%&gt;
	&lt;%file template=dao/dao_net_server.template                  filename=&lt;%tableName%&gt;DAO_NET_Server.java         destDir=&lt;%global.outputPath%&gt;/dao/net/server%&gt;
&lt;%endfor%&gt;

&lt;%file template=marshalling/marshalling_factory.template         filename=&lt;%global.databaseName%&gt;MarshallingFactory.java  destDir=&lt;%global.outputPath%&gt;/marshalling%&gt;
&lt;%file template=dao/dao_factory_interface.template               filename=&lt;%global.databaseName%&gt;DAOFactory.java          destDir=&lt;%global.outputPath%&gt;/dao/factory%&gt;
&lt;%file template=dao/dao_server_factory.template                  filename=&lt;%global.databaseName%&gt;ClientDAOFactory.java    destDir=&lt;%global.outputPath%&gt;/dao/factory%&gt;
&lt;%file template=dao/dao_server_factory.template                  filename=&lt;%global.databaseName%&gt;ServerDAOFactory.java    destDir=&lt;%global.outputPath%&gt;/dao/factory%&gt;
</code></pre>

	<p>When the generator starts up with these two files, the "current" node pointer points to "root".
	That means that when the template evaluation starts, and at any point outside a <code>foreach</code>
	block, the current pointer for this template is "root".  However, inside this template's <code>foreach</code>
	the current node pointer will iterate over each root child node that has the name "table".  The
	target child node name is specified as the "node" attribute value in the <code>foreach</code> tag.
	In this example, that looks like:</p>

	<p><code>&lt;%foreach node=table%&gt;</code></p>

	<p>After <code>foreach</code> has iterated over all of the "table" child nodes that it can find,
	it exits its block at the <code>endfor</code> tag and from there the current node is again "root".
	That's why the <code>file</code> tags after the <code>foreach</code> block use fully-qualified
	value names like <code>global.outputPath</code> to access the "global" values.</p>

	<p>However, all of the <code>file</code> tags inside the <code>foreach</code> block start their
	evaluation with a current pointer pointing to a "table" node, not "root".  So when you are looking
	at any of those child template files, you have to always keep that in mind.</p>

	<p>Inside those child templates, more <code>foreach</code> blocks can be used to iterate over the
	<code>column</code> and <code>foreignKey</code> child nodes.  Thus <code>foreach</code> blocks are
	nested either directly inside a template or indirectly in a template's <code>file</code> tags to
	traverse the config value tree and generate the desired output.</p>

	<p>Multiple examples can be found in the Examples/codegenerator folders</p>
 */
public class ForEachBlock extends TemplateBlock_Base {


	// Data members
	protected	String		m_nodeName;	// This is the name of the config node that will be the temporary "root" node for each iteration of the loop.  For example, if this is == "class", then when we enter Evaluate(), we will run through the loop once for each "class" child node we find on the passed-in p_currentNode.


	//*********************************
	public ForEachBlock() {
		super("foreach");
	}


	//*********************************
	@Override
	public ForEachBlock GetInstance() {
		return new ForEachBlock();
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute("node");
			if (t_nodeAttribute == null) {
				Logger.LogError("ForEachBlock.Init() did not find the [node] attribute that is required for foreach tags.");
				return false;
			}

			m_nodeName = t_nodeAttribute.GetValue().GetText();
			if (m_nodeName == null) {
				Logger.LogError("ForEachBlock.Init() did not get the value from attribute that is required for foreach tags.");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("ForEachBlock.Init() failed with error: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		try {
			GeneralBlock t_generalBlock	= new GeneralBlock();
			if (!t_generalBlock.Parse(p_tokenizer)) {
				Logger.LogError("ForEachBlock.Parse() general block parser failed.");
				return false;
			}

			String t_endingTagName = t_generalBlock.GetUnknownTag().GetTagName();
			if (!t_endingTagName.equalsIgnoreCase("endfor")) {
				Logger.LogError("ForEachBlock.Parse() general block ended on a tag named [" + t_endingTagName + "] at line [" + p_tokenizer.GetLineCount() + "].  The closing tag [endfor] was expected.");
				return false;
			}

			m_blockList.add(t_generalBlock);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogError("ForEachBlock.Parse() failed with error at line [" + p_tokenizer.GetLineCount() + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			LoopCounter t_iterationCount = new LoopCounter();

			for (ConfigNode t_nextConfigNode: p_currentNode.GetChildNodeList()) {
				// For each child config node of the name m_nodeName, we will re-evaluate all of our child blocks.
				if (t_nextConfigNode.GetName().compareToIgnoreCase(m_nodeName) == 0) {
					for (TemplateBlock_Base t_nextBlock: m_blockList) {
						if (!t_nextBlock.Evaluate(t_nextConfigNode, p_rootNode, p_writer, t_iterationCount)) {
							return false;
						}
					}

					t_iterationCount.IncrementCounter();
				}
			}
		}
		catch (Throwable t_error) {
			Logger.LogError("ForEachBlock.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name :  " + m_name		+ "\n");
		t_dump.append(p_tabs + "Node name       :  " + m_nodeName	+ "\n");

		for (TemplateBlock_Base t_nextBlock: m_blockList)
			t_dump.append("\n\n" + t_nextBlock.Dump(p_tabs + "\t"));

		return t_dump.toString();
	}
}

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
	A kludge that lets you skip nodes at the beginning of the list but still let a <code>first</code>
	tag work correctly.

	<p>This is from the example templates:</p>

	<pre><code>&lt;%foreach node=member%&gt;
	&lt;%if &lt;%isPrimaryKey%&gt; = true%&gt;
		&lt;%--counter%&gt;	<!-- This trick lets us skip over the primary keys AND adjust the counter
		so that we still get the correct code built in the "else" that follows. -->
	&lt;%else%&gt;
		&lt;%first%&gt;
			&lt;%text%&gt;			t_sql.append(" &lt;%sqlName%&gt; = ?");
&lt;%endtext%&gt;
		&lt;%else%&gt;
			&lt;%text%&gt;			t_sql.append(", &lt;%sqlName%&gt; = ?");
&lt;%endtext%&gt;
		&lt;%endfirst%&gt;

	&lt;%endif%&gt;
&lt;%endfor%&gt;</code></pre>

	<p>It is building the list of table columns to be set in an SQL UPDATE statement.  I wrote that
	to treat primary keys as immutable so the template will skip them.  However, it also needs to use
	the <code>first</code> tag to get the commas right.  If you skip leading items in the loop, then
	the counter is going to be greater than 1 and the <code>first</code> tag will not work correctly
	when you get the first item that isn't skipped.</p>

	<p>Using <code>--counter</code> as shown above fixes the problem by decrementing the counter set
	by the <code>foreach</code> loop when a node is skipped.  When the first non-skipped node is found,
	then the counter will be at 2 and after that, no matter what combination of skipped and non-skipped
	nodes occurs, the <code>first</code> block will never run again.</p>
 */
public class DecrementCounter extends TemplateBlock_Base {


	//*********************************
	public DecrementCounter() {
		super("--counter");
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		// No action needs to be taken here.
		return true;
	}


	//*********************************
	@Override
	public DecrementCounter GetInstance() {
		return new DecrementCounter();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		// Nothing to do here.
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(ConfigNode		p_currentNode,
							ConfigNode		p_rootNode,
							Cursor 			p_writer,
							LoopCounter		p_iterationCounter)
	{
		try {
			p_iterationCounter.DecrementCounter();
		}
		catch (Throwable t_error) {
			Logger.LogError("DecrementCounter.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Block type name :  --counter\n");

		return t_dump.toString();
	}
}

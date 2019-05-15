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
This lets you output the counter value.

<pre><code>&lt;%counter%&gt;</code></pre>
*/
public class Counter extends TemplateBlock_Base {

	static public final String		BLOCK_NAME			= "counter";



//*********************************
public Counter() {
	super("counter");
}


//*********************************
@Override
public boolean Init(TagParser p_tagParser) {
	// No action needs to be taken here.
	return true;
}


//*********************************
@Override
public Counter GetInstance() {
	return new Counter();
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
		p_writer.Write(Integer.toString(p_iterationCounter.GetCounter()));
	}
	catch (Throwable t_error) {
		Logger.LogError("Counter.Evaluate() failed with error: ", t_error);
		return false;
	}

	return true;
}


//*********************************
@Override
public String Dump(String p_tabs) {
	StringBuilder t_dump = new StringBuilder();

	t_dump.append(p_tabs + "Block type name :  counter\n");

	return t_dump.toString();
}
}

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



import codegenerator.generator.utils.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	Sets the tab marker offset to the length of the line as it exists when this tag is evaluated.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%tabMarker%&gt;</code></pre>

	<p>At any point after this tag is used, a <code>tabStop</code> tag with its <code>stopType</code>
	attribute set to <code>marker</code> can use that marker location as its offset by itself or
	it can add an offset to place a column relative to the marker.  Please refer to the examples.</p>
 */
public class TabMarker extends TemplateBlock_Base {

	static public final String		BLOCK_NAME		= "tabMarker";


	// Data members


	//*********************************
	public TabMarker() {
		super(BLOCK_NAME);
		m_isSafeForTextBlock = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		return true;
	}


	//*********************************
	@Override
	public TemplateBlock_Base GetInstance() {
		return new TabMarker();
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
			TabStop.SetMarker(TabStop.GetCurrentLineLength(p_writer.GetCurrentLineContents()));
		}
		catch (Throwable t_error) {
			Logger.LogError("TabMarker.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

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



import java.util.*;



/**
	Factory-pattern class that generates the correct object for the requested tag name.
 */
public class BlockFactory {

	// Data Members
	protected static	TreeMap<String, TemplateBlock_Base>	m_blocks = new TreeMap<>();


	// The block name is converted to lower case so that we can universally force the names searched for in GetBlock() below to lower case and thereby make it possible to handle whatever capitalization the user uses in their templates (i.e. camelcase, all lower, all cap, etc.).
	static {
		//m_blocks.put(.BLOCK_NAME.toLowerCase(), new ConfigVariable());	// This class is a special case that has to be handled by the TextBlock.
		m_blocks.put(CamelCase.BLOCK_NAME.toLowerCase(),				new CamelCase());
		m_blocks.put(Counter.BLOCK_NAME.toLowerCase(),					new Counter());
		m_blocks.put(CounterDecrement.BLOCK_NAME.toLowerCase(),			new CounterDecrement());
		m_blocks.put(CounterIncrement.BLOCK_NAME.toLowerCase(),			new CounterIncrement());
		m_blocks.put(CounterVariable.BLOCK_NAME.toLowerCase(),			new CounterVariable());
		m_blocks.put(CustomCodeBlock.BLOCK_NAME.toLowerCase(),			new CustomCodeBlock());
		m_blocks.put(FileBlock.BLOCK_NAME.toLowerCase(),				new FileBlock());
		m_blocks.put(FirstElseBlock.BLOCK_NAME.toLowerCase(),			new FirstElseBlock());
		m_blocks.put(FirstLetterToLowerCase.BLOCK_NAME.toLowerCase(),	new FirstLetterToLowerCase());
		m_blocks.put(ForEachBlock.BLOCK_NAME.toLowerCase(),				new ForEachBlock());

		m_blocks.put(IfElseBlock.BLOCK_NAME.toLowerCase(),				new IfElseBlock());
			m_blocks.put(If_Boolean.And.BLOCK_NAME.toLowerCase(),		new If_Boolean.And());	// The "AND", "OR" and "NOT" boolean logic tags for the IfElseBlock.
			m_blocks.put(If_Boolean.Or.BLOCK_NAME.toLowerCase(),		new If_Boolean.Or());
			m_blocks.put(If_Boolean.Not.BLOCK_NAME.toLowerCase(),		new If_Boolean.Not());

		m_blocks.put(OuterContext.BLOCK_NAME.toLowerCase(),				new OuterContext());
		m_blocks.put(OuterContextEval.BLOCK_NAME.toLowerCase(),			new OuterContextEval());
		m_blocks.put(TabMarker.BLOCK_NAME.toLowerCase(),				new TabMarker());
		m_blocks.put(TabSettings.BLOCK_NAME.toLowerCase(),				new TabSettings());
		m_blocks.put(TabStop.BLOCK_NAME.toLowerCase(),					new TabStop());
		m_blocks.put(TextBlock.BLOCK_NAME.toLowerCase(),				new TextBlock());
		m_blocks.put(TypeConvert.BLOCK_NAME.toLowerCase(),				new TypeConvert());
		m_blocks.put(TypeConvertLoadFile.BLOCK_NAME.toLowerCase(),		new TypeConvertLoadFile());
		m_blocks.put(VariableBlock.BLOCK_NAME.toLowerCase(),			new VariableBlock());
	}


	//=================================
	/**
	 * Returns the object for the requested tag name.
	 *
	 * @param p_tagName
	 * @return			Returns NULL if the tag name isn't defined by any of the contained classes.
	 */
	public static TemplateBlock_Base GetBlock(String p_tagName) {
		TemplateBlock_Base t_sourceBlock = m_blocks.get(p_tagName.toLowerCase());	// As noted above, forcing everything to lower case makes it possible to handle whatever capitalization scheme is used in the template.
		if (t_sourceBlock != null)
			return t_sourceBlock.GetInstance();

		return null;
	}
}

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



import java.util.*;



/**
	Factory-pattern class that generates the correct object for the requested tag name.
 */
public class BlockFactory {

	// Data Members
	protected static	LinkedList<TemplateBlock_Base>	m_blocks = new LinkedList<TemplateBlock_Base>();


	// Each block class has to define in its constructor the tag name that it handles.  This class then uses that fact to find the target handler for the requested tag name.
	static {
		//m_blocks.add(new ConfigVariable());	// This class is a special case that has to be handled by the TextBlock.
		m_blocks.add(new CamelCase());
		m_blocks.add(new Counter());
		m_blocks.add(new CustomCodeBlock());
		m_blocks.add(new DecrementCounter());
		m_blocks.add(new FileBlock());
		m_blocks.add(new FirstElseBlock());
		m_blocks.add(new FirstLetterToLowerCase());
		m_blocks.add(new ForEachBlock());

		m_blocks.add(new IfElseBlock());
			m_blocks.add(new If_Boolean.And());	// The "AND", "OR" and "NOT" boolean logic tags for the IfElseBlock.
			m_blocks.add(new If_Boolean.Or());
			m_blocks.add(new If_Boolean.Not());

		m_blocks.add(new TabMarker());
		m_blocks.add(new TabSettings());
		m_blocks.add(new TabStop());
		m_blocks.add(new TextBlock());
		m_blocks.add(new TypeConvert());
		m_blocks.add(new VariableBlock());
	}


	//=================================
	/**
	 * Returns the object for the requested tag name.
	 *
	 * @param p_tagName
	 * @return			Returns NULL if the tag name isn't defined by any of the contained classes.
	 */
	public static TemplateBlock_Base GetBlock(String p_tagName) {
		for (TemplateBlock_Base t_nextBlock: m_blocks) {
			if (t_nextBlock.GetName().equalsIgnoreCase(p_tagName))
				return t_nextBlock.GetInstance();
		}

		return null;
	}
}

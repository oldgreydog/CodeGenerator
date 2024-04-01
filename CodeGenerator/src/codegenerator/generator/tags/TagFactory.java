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
public class TagFactory {

	// Data Members
	private static	TreeMap<String, Tag_Base>	m_tags = new TreeMap<>();


	// The tag name is converted to lower case so that we can universally force the names searched for in GetTag() below to lower case and thereby make it possible to handle whatever capitalization the user uses in their templates (i.e. camelcase, all lower, all cap, etc.).
	static {
		//m_tags.put(.TAG_NAME.toLowerCase(),						new ConfigValue());	// This class is a special case that has to be handled by the Text tag parsing.
		m_tags.put(CamelCase.TAG_NAME.toLowerCase(),				new CamelCase());
		m_tags.put(CopyFile.TAG_NAME.toLowerCase(),					new CopyFile());
		m_tags.put(Counter.TAG_NAME.toLowerCase(),					new Counter());
		m_tags.put(CounterDecrement.TAG_NAME.toLowerCase(),			new CounterDecrement());
		m_tags.put(CounterIncrement.TAG_NAME.toLowerCase(),			new CounterIncrement());
		m_tags.put(CounterVariable.TAG_NAME.toLowerCase(),			new CounterVariable());
		m_tags.put(CustomCode.TAG_NAME.toLowerCase(),				new CustomCode());
		m_tags.put(FileTag.TAG_NAME.toLowerCase(),					new FileTag());
		m_tags.put(FirstElse.TAG_NAME.toLowerCase(),				new FirstElse());
		m_tags.put(FirstLetterToLowerCase.TAG_NAME.toLowerCase(),	new FirstLetterToLowerCase());
		m_tags.put(ForEach.TAG_NAME.toLowerCase(),					new ForEach());

		m_tags.put(IfElse.TAG_NAME.toLowerCase(),					new IfElse());
			m_tags.put(If_Boolean.And.TAG_NAME.toLowerCase(),		new If_Boolean.And());	// The "AND", "OR" and "NOT" boolean logic tags for IfElse.
			m_tags.put(If_Boolean.Or.TAG_NAME.toLowerCase(),		new If_Boolean.Or());
			m_tags.put(If_Boolean.Not.TAG_NAME.toLowerCase(),		new If_Boolean.Not());

		m_tags.put(Include.TAG_NAME.toLowerCase(),					new Include());
		m_tags.put(OuterContext.TAG_NAME.toLowerCase(),				new OuterContext());
		m_tags.put(OuterContextEval.TAG_NAME.toLowerCase(),			new OuterContextEval());
		m_tags.put(TabMarker.TAG_NAME.toLowerCase(),				new TabMarker());
		m_tags.put(TabSettings.TAG_NAME.toLowerCase(),				new TabSettings());
		m_tags.put(TabStop.TAG_NAME.toLowerCase(),					new TabStop());
		m_tags.put(Text.TAG_NAME.toLowerCase(),						new Text());
		m_tags.put(TypeConvert.TAG_NAME.toLowerCase(),				new TypeConvert());
		m_tags.put(TypeConvertLoadFile.TAG_NAME.toLowerCase(),		new TypeConvertLoadFile());
		m_tags.put(Variable.TAG_NAME.toLowerCase(),					new Variable());
	}


	//=================================
	/**
	 * Returns the object for the requested tag name.
	 *
	 * @param p_tagName
	 * @return			Returns NULL if the tag name isn't defined by any of the contained classes.
	 */
	public static Tag_Base GetTag(String p_tagName) {
		Tag_Base t_sourceTag = m_tags.get(p_tagName.toLowerCase());	// As noted above, forcing everything to lower case makes it possible to handle whatever capitalization scheme is used in the template.
		if (t_sourceTag != null)
			return t_sourceTag.GetInstance();

		return null;
	}
}

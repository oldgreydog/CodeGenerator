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
import coreutil.logging.*;



/**
	Allows for multiple code-language-based mappings in the same template set.

	<p>Example use of this tag:</p>

	<pre><code>&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "builtin" %&gt;</code></pre>

	<p>A very common use case for this tag is where you have a template set that will output DDL for
	creating a database instance and the matching data access classes for one or more languages.  To do
	that, it is easiest to create the config variable file with the broadest set of data types (in this
	case, the SQL data types) and then use the data type mapping files to define what language-specific
	types (or anything else like getter/setter function names) are most appropriate for a particular
	language template.</p>

	<p>While I will discuss the important details below, the best way to see how to use this tag will be
	to look in the <code>Examples/codegenerator</code> folders.  In that directory, I have provided two versions
	of the example templates with the largest difference being that the <code>database</code> directory doesn't
	use this tag and <code>database_alt</code> does.  If you use a diff tool like BeyondCompare or kdiff3 to compare
	those two folders, you will be able to easily see the differences.</p>

	<p>The core element to this type conversion functionality is the type map files.  If you look in
	<code>Examples/codegenerator/database_alt</code> directory, you will find a file named
	<code>DataType_Conversion_SQL_Server_to_Java.xml</code>.  We'll use a snip from the top of that
	file to discuss it's features.</p>

	<pre><code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;Node name="root"&gt;
	&lt;Node name="dataTypeMaps"&gt;
		&lt;Node name="typeMap"&gt;
			&lt;Value name="targetLanguage"&gt;java&lt;/Value&gt;
			&lt;Value name="targetTypeFieldDelimiter"&gt;:&lt;/Value&gt;

			&lt;!--	The targetType field is defined as:	&lt;groupID(i.e. builtin,object)&gt;:&lt;type&gt;	--&gt;
			&lt;!--	The "groupID" subfield allows you to have as many mappings as you need for a particular type, including, for example, specific functions needed to read and write that value to the database.	--&gt;
			&lt;Node name="type"&gt;
				&lt;Value name="sourceType"	&gt;int&lt;/Value&gt;

				&lt;Value name="targetType"	&gt;builtin:int&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;object:Integer&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;objectParseFunc:Integer.parseInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;prepStmntSetFunc:setInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;prepStmntSetNullConst:INTEGER&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;resultSetGetFunc:getInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;jsonGetParamFunc:GetIntParameterValue&lt;/Value&gt;
			&lt;/Node&gt;

			&lt;Node name="type"&gt;
				&lt;Value name="sourceType"	&gt;tinyint&lt;/Value&gt;

				&lt;Value name="targetType"	&gt;builtin:int&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;object:Integer&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;objectParseFunc:Integer.parseInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;prepStmntSetFunc:setInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;prepStmntSetNullConst:INTEGER&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;resultSetGetFunc:getInt&lt;/Value&gt;
				&lt;Value name="targetType"	&gt;jsonGetParamFunc:GetIntParameterValue&lt;/Value&gt;
			&lt;/Node&gt;
...
</code></pre>

	<p>This XML structure was set up to handle multiple language maps in the same file if you desire.
	One or more <code>typeMap</code> tags are wrapped in an outer <code>dataTypeMaps</code> tag.</p>

	<p>The first two values the <code>targetType</code> are <code>targetLanguage</code> and
	<code>targetTypeFieldDelimiter</code>.  <code>targetLanguage</code> defines the language mapping
	that this mapping defines.  In the example above, it's "java".  This concept needs to be pretty
	loosely interpreted, though.  A language mapping is really an anything-mapping.  You may do everything
	in java, but you might have one set of templates that generate code based on, say, native java
	database access and a different set of templates that use Spring.  In that case, you may have two
	different mapping files with very different mappings defined.</p>

	<p>That's really the whole point.  That lets you have one config value set that defines a database
	or API and it can be used with any number of template sets to generate code for any number of language
	and/or toolkit combinations.  In fact, when someone creates a template set, they will also probably
	define a language mapping to match it.  The trick is using a "source" data type set that is the
	broadest one used by any of the template sets.  If one of your target template sets is SQL, for
	example, then you are going to use the SQL data types as the source because SQL generally has way
	more built in data types than any normal language.  Then you set up the data type map files to
	down-convert the broad SQL type set to the more limited type set for the target language.  And
	you can add any mappings like database library getter/setter function names for each type so that
	the templates need little to no hard-coded type mapping.</p>

	<p>The second value in the <code>typeMap</code> node is <code>targetTypeFieldDelimiter</code>.
	That defines what delimiter is used to separate the value fields in the <code>targetType</code>
	values defined below.  I added this value because I didn't know if, no matter what delimiter I
	chose, there would probably be some target language that could use that delimiter in a way that
	it would be needed in the actual type string and would therefore break the parsing and screw up
	the value. C++, for example, uses colons (:) in scoped type names.  Letting the user define a
	different delimiter for the file should eliminate any conflicts.</p>

	<p>Inside each <code>typeMap</code> node are any number of <code>type</code> nodes, each of which
	contains one <code>sourceType</code> value that defines a data type that is used in the config
	values file to declare a data type for an object.  That's followed by any number of <code>targetType</code>
	mappings that are needed in the templates wherever the object type is important.  Again, the easiest way
	to get a better grip on this usage is in the <code>Examples/codegenerator/database_alt</code>
	directories.</p>

	<p>The <code>targetType</code> values are in two parts divided by a field delimiter.  The first field
	is the <code>groupID</code>.  The <code>groupID</code> is used in the <code>typeConvert</code> tag
	to define which of the <code>targetType</code> mappings to use at that location in the template.
	That means you aren't limited to data types for conversions.  You can include anything that you
	need for the templates that are type-dependent including function names like getters/setters that
	depend on type.  You can also do type constants names that are defined by some class.  All of these
	are found in the example above.</p>

	<p>Now we have all of the elements needed for the tag.  Once again, here's the tag example from above:</p>

	<pre><code>&lt;%typeConvert targetLanguage = "java" sourceType = &lt;%type%&gt; groupID = "builtin" %&gt;</code></pre>

	<p>So, the <code>targetLanguage</code> attribute tells the <code>typeConvert</code> tag which
	<code>typeMap</code> set to use.  The <code>sourceType</code> attribute tells the <code>typeConvert</code>
	tag which value from the config values to use to find the desired <code>sourceType</code> from the map.
	And the <code>groupID</code> attribute tells it which of the <code>targetType</code> mappings to
	use for that <code>sourceType</code>.</p>

	<p>So given the file segment above, let's say that the &lt;%type%&gt; config value for the current
	config node returns "tinyint".  Then this example would get the <code>targetType</code> with the
	groupID of "builtin" which is "int".</p>
 */
public class TypeConvert extends Tag_Base {

	static public final String		TAG_NAME						= "typeConvert";

	static private final String		ATTRIBUTE_TARGET_LANGUAGE		= "targetLanguage";
	static private final String		ATTRIBUTE_SOURCE_TYPE			= "sourceType";
	static private final String		ATTRIBUTE_GROUP_ID				= "groupID";


	// Data members
	private	String		m_targetLanguage	= null;
	private	Tag_Base	m_sourceType		= null;
	private	String		m_groupID			= null;


	//*********************************
	public TypeConvert() {
		super(TAG_NAME);
		m_isSafeForText			= true;
		m_isSafeForAttributes	= true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TypeConvert.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			// The target language should be a string constant.
			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TARGET_LANGUAGE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeConvert.Init() did not find the [" + ATTRIBUTE_TARGET_LANGUAGE + "] attribute that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_targetLanguage = t_nodeAttribute.GetAttributeValueAsString();
			if (m_targetLanguage == null) {
				Logger.LogError("TypeConvert.Init() did not get the value from attribute [" + ATTRIBUTE_TARGET_LANGUAGE + "] that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}


			// Only the source type should require evaluation.
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_SOURCE_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeConvert.Init() did not find the [" + ATTRIBUTE_SOURCE_TYPE + "] attribute that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_sourceType = t_nodeAttribute.GetAttributeValue();
			if (m_sourceType == null) {
				Logger.LogError("TypeConvert.Init() did not get the value from attribute [" + ATTRIBUTE_SOURCE_TYPE + "] that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}


			// The group ID should be a string constant.
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_GROUP_ID);
			if (t_nodeAttribute == null) {
				Logger.LogError("TypeConvert.Init() did not find the [" + ATTRIBUTE_GROUP_ID + "] attribute that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			m_groupID = t_nodeAttribute.GetAttributeValueAsString();
			if (m_groupID == null) {
				Logger.LogError("TypeConvert.Init() did not get the value from attribute [" + ATTRIBUTE_GROUP_ID + "] that is required for TypeConvert tags at line number [" + p_tagParser.GetLineNumber() + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeConvert.Init() failed with error at line number [" + p_tagParser.GetLineNumber() + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TypeConvert();
	}


	//*********************************
	@Override
	public boolean Parse(TemplateTokenizer p_tokenizer) {
		return true;
	}


	//*********************************
	@Override
	public boolean Evaluate(EvaluationContext p_evaluationContext)
	{
		try {
			if (m_targetLanguage == null) {
				Logger.LogError("TypeConvert.Evaluate() was not initialized.");
				return false;
			}

			// Only the source type should require evaluation.  The target language and group IDs should both be string constants.
			String t_sourceTypeValue = Tag_Base.EvaluateToString(m_sourceType, p_evaluationContext);
			if (t_sourceTypeValue == null) {
				Logger.LogError("TypeConvert.Evaluate() failed to evaluate the [sourceType] value.");
				return false;
			}

			String t_convertedType = DataTypeManager.GetTypeConversion(m_targetLanguage, t_sourceTypeValue, m_groupID);

			if ((t_convertedType != null) && !t_convertedType.isEmpty())
				p_evaluationContext.GetCursor().Write(t_convertedType);
			else
				p_evaluationContext.GetCursor().Write("");
				//p_evaluationContext.GetCursor().Write("No type conversion was found for language [" + t_targetLanguageValue + "] sourceType [" + t_sourceValue + "] groupID [" + t_groupIDValue + "].");
		}
		catch (Throwable t_error) {
			Logger.LogException("TypeConvert.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name 	+ "\n");

		if (m_sourceType != null) {
			t_dump.append("\n\n");
			t_dump.append(m_sourceType.Dump(p_tabs + "\t"));

		}

		return t_dump.toString();
	}
}

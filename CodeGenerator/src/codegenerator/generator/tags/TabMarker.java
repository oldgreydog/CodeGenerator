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
	<p>Sets the tab marker offset to the length of the line as it exists when this tag is evaluated.  In other
	words, this sets a marker column value equal to the offset of the first character in the opening tag delimiter.
	In the example below, that is the column number where the "<code><B>&lt;</B></code>" appears.</p>

	<p>!!NOTE!! The offset column value is set at the point in time that this tag is <i>evaluated</i>, not where it
	appears in the raw template file!  You can have any number of tags in the line preceding this tag in the template,
	but the tag gets the offset value from the line length of the cursor object it receives in its Evaluate() context
	parameter.  At that point, all preceding tags have been evaluated and their output text written to the cursor so
	the value the marker gets has nothing to do with its column position in the template file itself.</p>

	<p>Example use of this tag:</p>

	<pre>	<code>&lt;%text%&gt;
		public &lt;%className%&gt;(&lt;%tabMarker%&gt;&lt;%endtext%&gt;

	&lt;%foreach node=column%&gt;
		&lt;%first%&gt;

		&lt;%else%&gt;
			&lt;%text%&gt;,
	&lt;%endtext%&gt;
		&lt;%endfirst%&gt;

		&lt;%text%&gt;&lt;%tabStop stopType = marker %&gt;&lt;%variable name = parameterType evalmode = evaluate %&gt;	&lt;%tabStop stopType = marker offset = 12 %&gt;p_&lt;%firstLetterToLowerCase value = &lt;%name%&gt;%&gt;&lt;%endtext%&gt;
	&lt;%endfor%&gt;
</code></pre>

	<p>At any point after this tag is used, a <code><B>tabStop</B></code> tag with its <code><B>stopType</B></code>
	attribute set to <code><B>marker</B></code> can use that marker location as its offset by itself or
	it can add an <code><B>offset</B></code> to place a column relative to the marker.  Both of these use-cases
	are shown in the example above and would output a java constructor that looks like this (I'm not sure
	if the parameters will line up in every browser, but they would in the output file):</p>

	<pre><code>	public Company(int		   p_companyID,
				   String	   p_name,
				   String	   p_warehouseName,
				   Integer	   p_parentCompanyID,
				   boolean	   p_isActive,
				   String	   p_locale)
</code></pre>

	<p>No matter how long the class name is, the parameters would always line up on the first column after the "("
	where the marker was set.</p>
*/
public class TabMarker extends Tag_Base {

	static public final String		TAG_NAME		= "tabMarker";


	// Data members


	//*********************************
	public TabMarker() {
		super(TAG_NAME);
		m_isSafeForText = true;
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		if (!super.Init(p_tagParser)) {
			Logger.LogError("TabMarker.Init() failed in the parent Init() at line number [" + p_tagParser.GetLineNumber() + "].");
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TabMarker();
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
			TabSettingsManager t_tabsManager = p_evaluationContext.GetTabSettingsManager();
			if (t_tabsManager == null) {
				Logger.LogError("TabMarker.Evaluate() got a NULL TabSettingsManager reference from the evaluation context.");
				return false;
			}

			t_tabsManager.SetMarker(t_tabsManager.GetCurrentLineLength(p_evaluationContext.GetCursor().GetCurrentLineContents()));
		}
		catch (Throwable t_error) {
			Logger.LogException("TabMarker.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

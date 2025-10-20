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
import coreutil.logging.*;



/**
<p>Sets the tab marker offset to the length of the line as it exists when this tag is evaluated.  In other
words, this sets a marker column value equal to the offset of the first character in the opening tag delimiter.
In the example below, that is the column number where the "<code><b>&lt;</b></code>" appears.</p>

<p>!!NOTE!! The offset column value is set at the point in time that this tag is <i>evaluated</i>, not where it
appears in the raw template file!  You can have any number of tags in the line preceding this tag in the template,
but the tag gets the offset value from the line length of the cursor object it receives in its Evaluate() context
parameter.  At that point, all preceding tags have been evaluated and their output text written to the cursor so
the value the marker gets has nothing to do with its column position in the template file itself.</p>

<h3>Usage example</h3>

<pre>	<code><b>&lt;%text%&gt;
		public &lt;%className%&gt;(&lt;%tabMarker%&gt;&lt;%endtext%&gt;

	&lt;%foreach node=column%&gt;
		&lt;%first%&gt;

		&lt;%else%&gt;
			&lt;%text%&gt;,
	&lt;%endtext%&gt;
		&lt;%endfirst%&gt;

		&lt;%text%&gt;&lt;%tabStop stopType = marker %&gt;&lt;%variable name = parameterType evalmode = evaluate %&gt;	&lt;%tabStop stopType = marker offset = 12 %&gt;p_&lt;%firstLetterToLowerCase value = &lt;%name%&gt;%&gt;&lt;%endtext%&gt;
	&lt;%endfor%&gt;
</b></code></pre>

<p>At any point after this tag is used, a <code><b>tabStop</b></code> tag with its <code><b>stopType</b></code>
attribute set to <code><b>marker</b></code> can use that marker location as its offset by itself or
it can add an <code><b>offset</b></code> attribute value to place a column relative to the marker.  Both of these use-cases
are shown in the example above and would output a java constructor that looks like this (I'm not sure
if the parameters shown below will line up in every browser, but they would in the output file):</p>

<pre><code><b>    public Company(int       p_companyID,
				   String    p_name,
				   String    p_warehouseName,
				   Integer   p_parentCompanyID,
				   boolean   p_isActive,
				   String    p_locale)
</b></code></pre>

<p>No matter how long the class name is, the parameter's data types would always line up on the first column after the "("
where the marker was set.</p>

<p>I finally reached the point that I wanted even more control of alignment so I've added the <code><b>optionalMarkerName</b></code>
attribute so that I can have as many markers as I want.  This is what the new attribute can look like:</p>
<pre>	<code><b>&lt;%tabMarker optionalMarkerName = "&lt;%className%&gt;1stMark" %&gt; </b></code></pre>

<p>Note that the name has to be unique for the template, so except in trivial cases, you'll have to include some config value(s) in
it to guarantee that uniqueness.  Once you have a named marker, you can use it with <code><b>tabStop</b></code> like this:</p>
<pre>	<code><b>&lt;%tabStop stopType = "marker" optionalMarkerName = "&lt;%className%&gt;1stMark" %&gt;</b></code></pre>

<p>The <code><b>optionalMarkerName</b></code> attribute only works on <code><b>tabStop</b></code> with <code><b>stopType = "marker"</b></code>.</p>

<p>NOTE!!! You can still use <code><b>tabMarker</b></code> without a name just like you have before.  That just means that each new unnamed
<code><b>tabMarker</b></code> will reset the "default" mark offset for any following unnamed <code><b>tabStop</b></code>s.</p>
*/
public class TabMarker extends Tag_Base {

	static public final String		TAG_NAME						= "tabMarker";

	static public final String		ATTRIBUTE_OPTIONAL_MARKER_NAME	= "optionalMarkerName";


	// Data members

	private	String			m_optionalMarkerName	= null;


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

		TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OPTIONAL_MARKER_NAME);
		if (t_nodeAttribute != null) {
			m_optionalMarkerName = t_nodeAttribute.GetAttributeValueAsString();
			if ((m_optionalMarkerName == null) || m_optionalMarkerName.isBlank()) {
				Logger.LogError("TabMarker.Init() did not get the value from the optional attribute [" + ATTRIBUTE_OPTIONAL_MARKER_NAME + "] at line number [" + m_lineNumber + "].");
				return false;
			}
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
				Logger.LogError("TabMarker.Evaluate() got a NULL TabSettingsManager reference from the evaluation context.  This can be caused by not having the [tabSettings] tag at the top of the file.");
				return false;
			}

			if (m_optionalMarkerName != null) {
				if (!t_tabsManager.SetNamedMarker(m_optionalMarkerName, t_tabsManager.GetCurrentLineLength(p_evaluationContext.GetCursor().GetCurrentLineContents()))) {
					Logger.LogError("TabMarker.Evaluate() failed to set the optional marker named [" + m_optionalMarkerName + "].");
					return false;
				}
			}
			else if (!t_tabsManager.SetMarker(t_tabsManager.GetCurrentLineLength(p_evaluationContext.GetCursor().GetCurrentLineContents()))) {
				Logger.LogError("TabMarker.Evaluate() failed to set the optional marker named [" + m_optionalMarkerName + "].");
				return false;
			}
		}
		catch (Throwable t_error) {
			Logger.LogException("TabMarker.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}
}

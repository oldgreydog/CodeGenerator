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
<p>Sets the tab parameters for the template until the next <code><b>tabSettings</b></code> changes them.</p>

<h3>Usage example</h3>

<pre>	<code><b>&lt;%tabSettings tabLength = "4" outputType = "tabs" %&gt;</b></code></pre>

<pre>	<code><b>&lt;%tabSettings tabLength = "4" outputType = "spaces" %&gt;</b></code></pre>

<h3>Attribute descriptions</h3>

<p><code><b>tabLength</b></code>:  defines how many spaces will be represented by a tab.  This
is used by the code when it is asked to do an offset of N spaces and the output type is "tabs".  It divides
the offset by the tabLength to get how many tabs to write and then it adds any spaces needed to reach the final
offset if it doesn't fall on a tab boundary.</p>

<p><code><b>outputType</b></code>: [values: <code><b>tabs</b></code>|<code><b>spaces</b></code>, no default: a value must be set]  defines whether a tab character or spaces will be written
to the output to represent a tab's worth of spacing.</p>
 */
public class TabSettings extends Tag_Base {

	static public final String		TAG_NAME				= "tabSettings";

	static private final String		ATTRIBUTE_TAB_LENGTH	= "tabLength";
	static private final String		ATTRIBUTE_OUTPUT_TYPE	= "outputType";

	static private final String		OUTPUT_TYPE_TABS		= "tabs";
	static private final String		OUTPUT_TYPE_SPACES		= "spaces";


	// Data members
	private int		m_tabLength		= -1;
	private int		m_outputType	= -1;


	//*********************************
	public TabSettings() {
		super(TAG_NAME);
	}


	//*********************************
	@Override
	public boolean Init(TagParser p_tagParser) {
		try {
			if (!super.Init(p_tagParser)) {
				Logger.LogError("TabSettings.Init() failed in the parent Init().");
				return false;
			}

			TagAttributeParser t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_TAB_LENGTH);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabSettings.Init() did not find the [" + ATTRIBUTE_TAB_LENGTH + "] attribute that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_tabLengthString = t_nodeAttribute.GetAttributeValueAsString();
			if (t_tabLengthString == null) {
				Logger.LogError("TabSettings.Evaluate() failed to get the [" + ATTRIBUTE_TAB_LENGTH + "] value at line number [" + m_lineNumber + "].");
				return false;
			}

			m_tabLength = Integer.parseInt(t_tabLengthString);


			// The offset attribute is required for type "stop" but not for "marker".
			t_nodeAttribute = p_tagParser.GetNamedAttribute(ATTRIBUTE_OUTPUT_TYPE);
			if (t_nodeAttribute == null) {
				Logger.LogError("TabSettings.Init() did not find the [" + ATTRIBUTE_OUTPUT_TYPE + "] attribute that is required for [" + TAG_NAME + "] tags at line number [" + m_lineNumber + "].");
				return false;
			}

			String t_outputType = t_nodeAttribute.GetAttributeValueAsString();
			if ((t_outputType == null) || t_outputType.isBlank()) {
				Logger.LogError("TabSettings.Evaluate() failed to get the [" + ATTRIBUTE_OUTPUT_TYPE + "] value at line number [" + m_lineNumber + "].");
				return false;
			}

			if (t_outputType.equalsIgnoreCase(OUTPUT_TYPE_TABS))
				m_outputType = TabSettingsManager.OUTPUT_TYPE_TABS;
			else if (t_outputType.equalsIgnoreCase(OUTPUT_TYPE_SPACES))
				m_outputType = TabSettingsManager.OUTPUT_TYPE_SPACES;
			else  {
				Logger.LogError("TabSettings.Evaluate() received an unknown [" + ATTRIBUTE_OUTPUT_TYPE + "] value [" + t_outputType + "] at line number [" + m_lineNumber + "].");
				return false;
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("TabSettings.Init() failed with error at line number [" + m_lineNumber + "]: ", t_error);
			return false;
		}
	}


	//*********************************
	@Override
	public Tag_Base GetInstance() {
		return new TabSettings();
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
			if (m_tabLength == -1) {
				Logger.LogError("TabSettings.Evaluate() - the tab length was not set for this template.");
				return false;
			}

			if (m_outputType == -1) {
				Logger.LogError("TabSettings.Evaluate() - the output type was not set for this template.");
				return false;
			}

			p_evaluationContext.PushTabSettingsManager(new TabSettingsManager(m_tabLength, m_outputType));
		}
		catch (Throwable t_error) {
			Logger.LogException("TabSettings.Evaluate() failed with error: ", t_error);
			return false;
		}

		return true;
	}


	//*********************************
	@Override
	public String Dump(String p_tabs) {
		StringBuilder t_dump = new StringBuilder();

		t_dump.append(p_tabs + "Tag name         :  " + m_name			+ "\n");
		t_dump.append(p_tabs + "Tab length		 :  " + m_tabLength		+ "\n");
		t_dump.append(p_tabs + "Output type		 :  " + m_outputType 	+ "\n");

		return t_dump.toString();
	}
}

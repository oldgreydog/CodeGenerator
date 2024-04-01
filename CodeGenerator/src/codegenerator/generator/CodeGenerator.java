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


package codegenerator.generator;



import java.io.*;
import java.util.*;

import codegenerator.generator.tags.*;
import codegenerator.generator.utils.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	This class encapsulates the code necessary to initialize and execute code generation.
 */
public class CodeGenerator {


	//*********************************
	/**
	 * Takes in the pathnames of the two files necessary to run the generator and executes all of
	 * the file loads/parses and then executes the generation.
	 *
	 * @param p_templateFilename	File contains the "root" template to be used for the generation run.
	 * @param p_configFilename	File contains the values that will be substituted into the template(s).  It is in the same ConfigManager XML format as is used in p_configFileName.
	 * @return
	 */
	public synchronized boolean Execute(String p_templateFilename,
										String p_configFilename)
	{
		try
		{
			// Parse the template file, which should be the next parameter.
			File t_templateFile = new File(p_templateFilename);
			if (!t_templateFile.exists()) {
				Logger.LogFatal("CodeGenerator.Execute() failed to open the template file [" + p_templateFilename + "].");
				return false;
			}


			long t_startTemplateParse = Calendar.getInstance().getTimeInMillis();

			TemplateParser	t_parser	= new TemplateParser();
			Tag_Base		t_template	= t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogFatal("CodeGenerator.Execute() failed to parse the template file [" + p_templateFilename + "].");
				return false;
			}

			//Logger.LogDebug("Template tree dump:\n" + t_template.Dump(""));

			long t_endTemplateParse = Calendar.getInstance().getTimeInMillis();


			// Parse the config file that contains the information that will be merged into the template.
			FileConfigValueSet t_configValues = new FileConfigValueSet();
			if (!t_configValues.Load(p_configFilename)) {
				Logger.LogFatal("CodeGenerator.Execute() failed to parse the config variables file [" + p_configFilename + "].");
				return false;
			}

			ConfigManager.AddValueSetFirst(t_configValues);		// I used the config substitution in a template config file but it turned out that it can only work through the ConfigManager so I had to add the parsed config file set to the ConfigManager to get it to work.

			ConfigNode t_templateConfig = t_configValues.GetRootNode();

			long t_endConfigValuesParse = Calendar.getInstance().getTimeInMillis();


			long t_startGenerate = Calendar.getInstance().getTimeInMillis();

			// And finally, "evaluate" the template with the config to generate the all of the file outputs.
			EvaluationContext t_context = new EvaluationContext(t_templateConfig, t_templateConfig, null, new LoopCounter());
			if (!t_template.Evaluate(t_context)) {
				Logger.LogFatal("CodeGenerator.Execute() failed to evaluate the template file [" + t_templateFile.getAbsolutePath() + "].");
				return false;
			}

			long t_endGenerate = Calendar.getInstance().getTimeInMillis();


			Logger.LogInfo("Template parse (millisec):      "	+ (t_endTemplateParse		- t_startTemplateParse));
			Logger.LogInfo("Config values parse (millisec): "	+ (t_endConfigValuesParse	- t_endTemplateParse));
			Logger.LogInfo("Generation time (millisec):     "	+ (t_endGenerate			- t_startGenerate));
			Logger.LogInfo("Generated file count:           "	+ FileTag.GetFileCount());

			return true;
		}
		catch (Throwable t_error)
		{
			Logger.LogFatal("CodeGenerator.Execute() failed with error: ", t_error);
			return false;
		}
	}
}

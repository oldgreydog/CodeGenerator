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


package codegenerator.generator;



import java.io.*;
import java.util.*;

import codegenerator.generator.tags.*;
import codegenerator.generator.utils.*;
import codegenerator.generator.utils.multithreading.*;
import coreutil.config.*;
import coreutil.logging.*;



/**
	An embeddable control class that executes the steps necessary to initialize and execute code generation.
 */
public class CodeGenerator {


	//===========================================
	/**
	 * Takes in the pathnames of the three files necessary to run the generator and executes all of
	 * the file loads/parses and then executes the generation.
	 *
	 * @param p_configFileName		File contains the general config info for the generator in ConfigManager XML format.  Currently that is the data conversion filenames and the logger config.
	 * @param p_templateFilename	File contains the "root" template to be used for the generation run.
	 * @param p_variablesFilename	File contains the values that will be substituted into the template(s).  It is in the same ConfigManager XML format as is used in p_configFileName.
	 * @return
	 */
	public boolean Execute(String p_configFileName,
						   String p_templateFilename,
						   String p_variablesFilename)
	{
		try
		{
			// The ConfigManager can be given, theoretically, any number of configuration info sources.  In practice, it will probably only be a couple of sources: the config file as default first source and either a database source or network source depending on whether the app is a client/server or a multi-tier architecture (respectively).
			// Load the config file and add its "source" to the ConfigManager first.  This will make its values the "default" values for anything not in other config sources added later.
			FileConfigValueSet	t_configValues = new FileConfigValueSet();
			if (!t_configValues.Load(p_configFileName)) {
				System.out.println("CodeGenerator.Execute() failed to import the config file [" + p_configFileName + "].");
				Cleanup();
				System.exit(1);
			}

			ConfigManager.AddValueSetFirst(t_configValues);


			// Set up the logger(s) that we need for this app.  This is controlled by the logging config info in the config file.
			if (!Logger.Init()) {
				System.out.println("CodeGenerator.Execute() failed initializing the Logger.");
				return false;
			}


			// Parse the template file, which should be the next parameter.
			File t_templateFile = new File(p_templateFilename);
			if (!t_templateFile.exists()) {
				Logger.LogFatal("CodeGenerator.Execute() failed to open the template file [" + p_templateFilename + "].");
				return false;
			}


			long t_startTemplateParse = Calendar.getInstance().getTimeInMillis();

			TemplateParser		t_parser	= new TemplateParser();
			TemplateBlock_Base	t_template	= t_parser.ParseTemplate(t_templateFile);
			if (t_template == null) {
				Logger.LogFatal("CodeGenerator.Execute() failed to parse the template file [" + p_templateFilename + "].");
				Cleanup();
				return false;
			}

			//Logger.LogDebug("Template tree dump:\n" + t_template.Dump(""));

			long t_endTemplateParse = Calendar.getInstance().getTimeInMillis();


			// Parse the config file that contains the information that will be merged into the template.
			File t_templateConfigFile = new File(p_variablesFilename);
			ConfigNode t_templateConfig = null;
			XMLConfigParser t_configParser = new XMLConfigParser();
			if ((t_templateConfig = t_configParser.ParseConfigFile(t_templateConfigFile)) == null) {
				Logger.LogFatal("CodeGenerator.Execute() failed to parse the template variables file [" + p_variablesFilename + "].");
				Cleanup();
				return false;
			}

			long t_endConfigValuesParse = Calendar.getInstance().getTimeInMillis();


			long t_startGenerate = Calendar.getInstance().getTimeInMillis();

			// And finally, "evaluate" the template with the config to generate the all of the file outputs.
			EvaluationContext t_context = new EvaluationContext(t_templateConfig, t_templateConfig, null, new LoopCounter());
			t_template.Evaluate(t_context);

			long t_endGenerate = Calendar.getInstance().getTimeInMillis();


			Logger.LogInfo("Template parse (millisec):      "	+ (t_endTemplateParse		- t_startTemplateParse));
			Logger.LogInfo("Config values parse (millisec): "	+ (t_endConfigValuesParse	- t_endTemplateParse));
			Logger.LogInfo("Generation time (millisec):     "	+ (t_endGenerate			- t_startGenerate));
			Logger.LogInfo("Generated file count:           "	+ FileBlock.GetFileCount());

			Cleanup();

			return true;
		}
		catch (Throwable t_error)
		{
			Logger.LogFatal("CodeGenerator.Execute() failed with error: ", t_error);

			Cleanup();

			return false;
		}
	}


	//===========================================
	private void Cleanup() {
		try {
			Logger.Shutdown();
		}
		catch (Throwable t_error)
		{
			Logger.LogException("CodeGenerator.Cleanup() failed with error: ", t_error);
		}
	}
}
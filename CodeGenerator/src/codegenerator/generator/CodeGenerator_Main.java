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



import coreutil.config.*;
import coreutil.logging.*;


/**
	This is a simple wrapper class that lets you easily run the code generator from the command line.

	<p>Usage:</p> <pre><code>java -cp .,coreutil.jar CodeGenerator_Main &lt;configFilename&gt; &lt;templateFilename&gt; &lt;variablesFilename&gt;</code></pre>
*/
public class CodeGenerator_Main {


	//===========================================
	static protected void Usage() {
		Logger.LogError("Usage: java -cp .,coreutil.jar CodeGenerator_Main <configFilename> <templateFilename> <variablesFilename>");
	}


	//===========================================
	public static void main(String[] p_args) {
		try
		{
			if (p_args.length != 3) {
				Usage();
				System.exit(1);
			}

			// The ConfigManager can be given, theoretically, any number of configuration info sources.  In practice, it will probably only be a couple of sources: the config file as default first source and either a database source or network source depending on whether the app is a client/server or a multi-tier architecture (respectively).
			// Load the config file and add its "source" to the ConfigManager first.  This will make its values the "default" values for anything not in other config sources added later.
			FileConfigValueSet	t_configValues		= new FileConfigValueSet();
			String				p_configFileName	= p_args[0];
			if (!t_configValues.Load(p_configFileName)) {
				System.out.println("CodeGenerator_Main() failed to import the config file [" + p_configFileName + "].");
				System.exit(1);
			}

			ConfigManager.AddValueSetFirst(t_configValues);


			// Set up the logger(s) that we need for this app.  This is controlled by the logging config info in the config file.
			if (!Logger.Init()) {
				System.out.println("CodeGenerator_Main() failed initializing the Logger.");
				Cleanup();
				System.exit(1);
			}


			CodeGenerator t_codeGenerator = new CodeGenerator();
			if (!t_codeGenerator.Execute(p_args[1], p_args[2])) {
				Logger.LogFatal("CodeGenerator_Main.main() failed to execute the code generator.");
				Cleanup();
				System.exit(1);
			}


			Cleanup();
		}
		catch (Throwable t_error)
		{
			Logger.LogFatal("CodeGenerator_Main.main() failed with error: ", t_error);
			Cleanup();
			System.exit(1);
		}
	}


	//===========================================
	private static void Cleanup() {
		try {
			Logger.Shutdown();
		}
		catch (Throwable t_error)
		{
			Logger.LogException("CodeGenerator_Main.main() failed with error: ", t_error);
		}
	}
}

@echo off


# The command line parameters must be in the correct order.
# Usage: ./generate <root template file path> <config values xml file path>

# NOTE: The CodeGeneratorConfig.xml is defaulted in the command line below.  If you copy this script to some other location, you will have to add the path to that file or make it a parameter, too.


java -cp lib/coreutil.jar;lib/code_generator.jar codegenerator.generator.CodeGenerator_Main CodeGeneratorConfig.xml %1 %2

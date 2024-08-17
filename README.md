## Getting started

First, thank you for stopping to look at this project!  This is a code generator that I have slowly evolved over the last 20 years or so to make my job a little easier.  Before I started this, I had been exposed to a couple of code generation efforts, one for Corba and another for a one-off code conversion/code generator.  The Corba generator was part of the package we were using (I don't remember which) and while it did generate working code, the output was an unreadable mess.  The other example was a one-off effort that did output reasonably readable code, but it was hand coded specifically for that project and it's code.  There was nothing reusable about it.  I wanted to have a code generator that could output code (or any other text) exactly the way I wanted it to look and to be reusable for anything I wanted.  This is the result of that effort.

You should really only need this repository if you are trying to write your own new tag handlers so that you can debug things.  Otherwise, you should just grab the latest release.  The release also has a javadoc.zip file with all of the generated javadoc files.  That should be the most detailed reference for using each of the tags.  The wiki here on the project is not the same as the javadoc.  The wiki has more of a high-level discussion of the how and why of using the generator whereas the javadocs are a more detailed tag reference.  You'll need to read both to get started, starting with the wiki first.

The RELEASE file contains descriptions of the changes contained in each release.

## Try the examples first

The "Examples" folder has its own README and the batch files necessary to generate the example output code with the provided config and templates.  The templates use most, but maybe not all, of the tags so that's a great place to reference when you're trying to create your first templates.  And if you want, you can play with the templates and the config and keep re-running the generation so that you can see how the changes affect the output.

## Or look at the new architecture templates (ArchTemplates) project

This new project is an attempt to show what's possible when using the code generator to create and maintain large swaths of a project.  You can find this new project at:

[ArchTemplates](https://github.com/oldgreydog/ArchTemplates)

It not only shows the generation of code, but also of project directories for an IDE (Eclipse in the alpha release) and generation of test code.


## If you decide to write your own tags...

You'll need to pull this repository to do debugging.  Please refer to the "Building the code" section in the wiki for more info, particularly about changing the logging level to see the parsing output.


## Let me know what you think

If you have questions, comments or problems, please email me.

helots54612@mypacks.net

I would be particularly interested in discussing internationalization (foreign language support) if anyone needs changes for that.



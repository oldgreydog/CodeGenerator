## Build Change!!

I've re-exposed the CoreUtils code but in a way that, unfortunately, changes how you need to build the code generator.  That code was originally just inside this repository but I eventually got to a point where I didn't think anyone would care about it, so I removed it and started just including the jar file.  Then when I was starting the v1.5 commits, I realized that if I compiled that jar locally where I usually run the latest OpenJDK (v14 at the moment), then people that can't run the latest jdk can't use it.  I shot myself in the foot with that one.

I've been thinking about it and I decided that I needed to put that code back out here but that it should be in a separate repository since the config management and logger code it contains aren't restricted to the code generator.  To that end, you also need to clone the oldgreydog/CoreUtils repository into the same parent directory where you cloned CodeGenerator. If you don't want to pull both projects into your IDE of choice (Eclipse project files are included in the repositories), then use the build script

./dev_build

the first time to build the CoreUtils jar and have it added it to the CodeGenerator lib directory.  From that point on, you can can just use the CodeGenerator project and include the lib/coreutil.jar file.

Or you can run the script

./release_build 

if you want to create the release zip file that contains the "install" folder that you can then use to run the code generator.

!!NOTE!! I've decided that it probably isn't practical to provide a pre-built release zip file since it has to be built to a particular version n of java and no matter what version I choose, there will be some group of people that, for various reasons, can't use a version of java >= n.  Therefore, the best solution for everyone will be for you to build you own release zip file as described above so you are guaranteed it will work for your installation.

!!NOTE 2!!  I haven't had a windows machine in years and it just occurred to me that these scripts only cover Linux, which is a problem.  I'll try to borrow a windows laptop from somebody and use it to create/test the windows versions of these scripts.  I'll add them as soon as I can.  Sorry!


## Now we return to the original programming...

First, thank you for stopping to look at this project!  This landing page has been completely stripped down and most of the project information has been moved to the wiki tab above.

If you have questions, comments or problems, please email me.

helots54612@mypacks.net

I would be particularly interested in discussing internationalization (foreign language support) if anyone needs changes for that.


## Note for existing users!

If you have version 1.3 or earlier and you want to get new code, you must read the RELEASE file!  Later versions might break your templates in a various ways and they are explained there.



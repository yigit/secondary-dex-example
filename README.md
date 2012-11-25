secondary-dex-example
=====================

This is a sample project to demonstrate how to dynamically include an external jar file into an android project to avoid LinearAlloc limits.

Android comes with a limit on how many classes-methods you can include in your project and you may hit that limit easily on Gingerbread devices if you include big jar files in your project.

This sample project is a workaround for that problem.
You can read more about it here: http://birbit.com/how-to-solve-linearalloc-problem/
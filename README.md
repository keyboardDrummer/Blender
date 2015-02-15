Particle Compiler
===============

Particle compiler is a tool for quickly building toy compilers. It can be used for prototyping language features or as a way of teaching compiler construction.
The approach taken is to define compiler 'particles' that represent language features and which can be combined in many ways.
A similar approach is described in the paper '*A Nanopass Framework for Compiler Education*'.

###Particles
A particle can include one or several phases such as parsing, type checking, optimization and code generation.
Commonly compilers are modularised by defining several compiler phases and using a limited number of intermediate languages.
Particle compiler focuses purely on intermediate languages to achieve modularisation.
Using weak typing it becomes simple to define a new intermediate language by applying a delta to an existing one.

###GUI
Particle compiler includes a GUI. You can use this to play around with the defined particles and construct a compiler form them.
Once you're happy with your compiler you can play around with it in the compiler cockpit. Here you can of course run the compiler,
but also do thing like ask the compiler for its in- and output grammar.

###BiGrammar
To enable parsing and printing with little development effort, particle compiler uses a 'BiGrammar'.
A BiGrammar defines a bidirectional mapping between text and an AST.
The approach taken here is very similar to that described by the paper '*Invertible Syntax Descriptions: Unifying Parsing and Pretty Printing*'.
A BiGrammar may be defined in a left recursive fashion because our implementation uses packrat parsing as described in
'*Packrat Parsing: Simple, Powerful, Lazy, Linear Time*' to deal with problems associated with such grammars.

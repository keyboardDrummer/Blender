Blender [![Build Status](https://travis-ci.org/keyboardDrummer/ParticleCompiler.svg?branch=master)](https://travis-ci.org/keyboardDrummer/ParticleCompiler) [![Join the chat at https://gitter.im/ParticleCompiler/Lobby#](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ParticleCompiler/Lobby#?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
===============

Blender is a language workbench, which is a tool to construct programming languages, such as <a href="https://www.jetbrains.com/mps/">Jetbrain's MPS</a>. 

Blender's main goal is to allow creating modular and thus re-usable languages. Blender gets this right by allowing you to transform any language into any other language. Most other language workbenches support only partial language modularity by allowing you to extend but not constrain a language. This allows you to only grow a small language into a bigger one, but not to transform between arbitrary ones. 

Another differentiator of Blender is that it is embedded in Scala, allowing you to use Blender features while inside a powerful general purpose programming language.

The core concept of Blender is a *particle*. A particle is piece of code that applies a small change to a language, such as adding/removing a language feature, or adding an optimization. Particles are put into an ordered list to form a language. Language re-use comes from re-using these particles. Some particles depend on others but there's a lot of freedom in combining them. A similar approach is described in the paper '*A Nanopass Framework for Compiler Education*'.

###Particles
A particle can include one or several phases such as parsing, type checking, optimization and code generation.
Commonly compilers are modularised by defining several compiler phases and using a limited number of intermediate languages.
Particle compiler focuses purely on intermediate languages to achieve modularisation.
By keeping the abstract syntax tree untyped, it becomes relatively simple to define a language as a sequence of language delta's.

###GUI
Blender includes a GUI. You can use this to play around with the defined particles and construct a compiler from them.
Once you're happy with your compiler you can play around with it in the compiler cockpit. Here you can run your compiler,
and do things like ask the compiler for its in- and output grammar.

###BiGrammar
To enable parsing and printing with little development effort, Blender uses a 'BiGrammar'.
A BiGrammar defines a bidirectional mapping between text and an AST.
The approach taken here is very similar to that described by the paper '*Invertible Syntax Descriptions: Unifying Parsing and Pretty Printing*'.
A BiGrammar may be defined in a left recursive fashion because our implementation uses packrat parsing as described in
'*Packrat Parsing: Simple, Powerful, Lazy, Linear Time*' to deal with problems associated with such grammars.

###State
Currently I'm only working on particles that enable a Java to JVM bytecode translation. I'm using JVM as a target because it's a relatively high level language so it's easy to compile to. My current goal is to finish or almost finish a Java compiler. I'd say it's about halfway done. After that I'd like to use very different languages for front- and back-end, say Haskell as a front-end and Javascript as a back-end. I'm curious what the similarities between these languages from different paradigms are and how many compiler particles can be reused between different languages.

### Build instructions
1. Install <a href="http://www.scala-sbt.org/">sbt</a>
2. Call 'sbt run' in the project root

###Introduction Video
<a href="http://www.youtube.com/watch?feature=player_embedded&v=IHFHcf61g-k
" target="_blank"><img src="http://img.youtube.com/vi/IHFHcf61g-k/0.jpg" 
alt="Introduction video" width="240" height="180" border="10" /></a>

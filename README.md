# modular

Modular provides a meta-architecture for Clojure projects, based on Stuart Sierra's
[component](https://github.com/stuartsierra/component) library.

Component dependencies form directed acyclic graphs. Two components in a
dependency relationship are coupled using Clojure protocols.

Besides some simple utility functions, modular is defined by a number of
pre-built components which can be composed with custom components to
form application systems.

Modular is an approach, rather than a library or framework.

Benefits include :-

* Components can be reasoned about _in their own terms_ independently from the system
* Component re-use
* Architectural consistency
* Stronger cohesion within parts, weaker coupling between parts

## Usage

```clojure
lein new modular website
```

## Copyright & License

The MIT License (MIT)

Copyright © 2014 JUXT LTD.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

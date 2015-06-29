# yada demonstrations

Demonstrations of yada.

This project is intended to be used both as a 'playground' project and as the basis for your own yada-based web project. You are free to change anything in this project including the license.

## Configuration

Configuration is loaded from `config.edn` using [aero](https://github.com/juxt/aero) and passed to affected components as per the logic in `{{sanitized}}/system.clj`.

## Standalone examples

Standalone examples can be found in `{{sanitized}}/examples.clj`.

The examples are managed by a component called `Examples` which satisfies the [component](https://github.com/stuartsierra/component) `Lifecycle` so in the `start` phase a new Aleph server instance is started. The port number of this can be found in `config.edn`.

```clojure
{:standalone-examples {:port 3001}
 …
}
 ```

### Routes

A single [bidi](https://github.com/juxt/bidi) route structure is created using `make-routes`. This is then injected into the Ring request by the `wrap-inject-routes` Ring middleware.

Note, this method of injecting dependency data into the Ring request is not encouraged. [Modular projects](https://modularity.org) have a more general form of dependency injection using [component](https://github.com/stuartsierra/component) dependencies.



## License

The MIT License (MIT)

Copyright © 2015 (insert your name/organisation here)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

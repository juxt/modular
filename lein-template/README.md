# Modular templates

Modular provides a number of templates to generate Clojure project skeletons with the aim of getting you up-and-running quickly with your own projects.

The command line arguments are

```clojure
lein new modular <your-project-name> <template-to-generate-from>
```

For example

```clojure
lein new modular my-website bootstrap-cover
```

## Supported templates

A growing number of templates are supported. If you find any problems
with these templates, raise issues on this project to get them fixed, or send pull requests.

These templates make use of components that are provided by modular, so
in most cases there is not much code, demonstrating the re-use that can
be achieved by using components.

### ```hello-world-web```

A simple website that displays "Hello World!".

This introduces the ```modular.ring/WebRequestHandler``` protocol which provides an integration surface between a Ring-compatible web server (e.g. Jetty, http-kit) and a Ring handler.

In most applications, the choice of web server is made ahead of time and some code is written to provide the web server with the handler to call. In this example, the relationship between the web server and Ring handler is made via a dependency declaration between the web server (in this case, [http-kit](http://www.http-kit.org/)) and the handler.

In the ```system.clj``` we see two components: ```:http-listener-listener``` and ```:hello-world-website-handler```. The first component declares a dependency on a request handler, with the ```using``` clause. The ```new-dependency-map``` function returns a map that satisfies the web server comonent's ```:request-handler``` dependency with our application component ```hello-world-web.hello-world-website/HelloWorldHandler```. This component must satisfy the ```modular.ring/WebRequestHandler``` protocol. This pattern of using protocols to provide the integration surface between components is used throughout [modular](https://github.com/juxt/modular).

### ```bidi-hello-world```

Another simple website that displays "Hello World!".

This introduces a router component which dispatches incoming HTTP
requests to one of its dependant components providing routes.

This demonstrates one of the principles of modularity. We avoid a single data structure comprising all the HTTP routes in a system. Rather, we allow individual modules to make contributions to this route structure.

[bidi](https://github.com/juxt/bidi) is used in this example, but the
principle would be the same using Compojure routes, which supports
similar composeable mechanisms.

### ```bootstrap-cover```

Bootstrap cover is adapted from [Twitter Bootstrap's 'cover' example](http://getbootstrap.com/examples/cover/). It introduces the Mustache template renderer, provided by [Clostache](https://github.com/fhd/clostache).

It also shows how to provide static resources from [JQuery](https://jquery.com/) and [Bootstrap](https://getbootstrap.com/) by contributing routes to the router, rather than requiring code modifications.

The template also introduces the concept of _co-dependencies_. This is
can be seen in the arguments to the ```Website``` record in
```website.clj```, which include a reference to the router. The
component uses this router to construct the URLs to other
handlers. While these handlers are also defined within the ```Website```
component, routes can be constructed in this way to any known handler in
the system, using its keyword and any required arguments. See
[bidi](https://github.com/juxt/bidi) for more details.

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

### ```bidi-hello-world```

A simple website that displays "Hello World!".

This introduces a router component which dispatches incoming HTTP
requests to one of its dependant components providing routes.

This demonstrates one of the principles of modularity. We avoid a single data structure comprising all the HTTP routes in a system. Rather, we allow individual modules to make contributions to this route structure.

[bidi](https://github.com/juxt/bidi) is used in this example, but the
principle would be the same using Compojure routes, which supports
similar composeable mechanisms.

### ```bootstrap-cover```

Bootstrap cover is adapted from [Twitter Bootstrap's 'cover' example](http://getbootstrap.com/examples/cover/). It introduces the Mustache template renderer, provided by [Clostache](https://github.com/fhd/clostache).

# http-kit-events

Components providing support for HTML5 Server Sent Events

## Quick start

    $ lein new modular +sse

## Leiningen

Add the following to the `:dependencies` section of your project's `project.clj` file :-

    [juxt.modular/http-kit-events "0.5.2"]

## Component constructors

### new event-service

#### Example usage

    (modular.http-kit.events/new-event-service :)

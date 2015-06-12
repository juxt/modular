# Modular template

With templating, we often want a model to be merged from multiple
contributors.

For example, you may have a template model that incorporates the title
and metadata of a web page, with some table data you want displayed.

```clojure
{:title "Console"
 :description "Data console"
 :copyright "Copyright 2015 © Grocers Ltd."

 :data [{:fruit "apple"}
        {:fruit "banana"}]
}
```

In this case, where should you create the template model? You might have
to create it in the code that renders your table data, but then you'd
need to pass in the title and metadata of the webpage, simply to you
could pass those details on to the templating process when constructing
a page.

Instead, we can think of a template model as aggregating the
contributions of data from a number of sources. If we are using
_component_, we can consider each source to be a separate component or
module. We can then use explicit dependency declarations as the
mechanism for loosely coupling the template model with its sources.

For this case, we would create a `AggregateTemplateModel` component, and bind one or more (usually more) dependencies, each of which satisfy the `modular.template.TemplateModel` protocol.

# modular less

The Less component compiles [Less](http://lesscss.org/) files to CSS.

The project contains a vanilla compiler component and one pre-configured for [Bootstrap](http://getbootstrap.com/) customization

This is demonstrated in modular's dashboard template, which shows how
Bootstrap can be
[customized](http://www.smashingmagazine.com/2013/03/12/customizing-bootstrap/)
via the Less files, which is more flexible than overlaying a custom CSS
file.

```
lein new modular foo dashboard
```

# Design and develop in the same environment

With this component, it is possible to coordinate development with
designers who can make changes to Less files, run a (reset) and test
their changes against the actual working app.


# References

The default configuration of the Bootstrap component follows the conventions described in this article: http://www.smashingmagazine.com/2013/03/12/customizing-bootstrap/

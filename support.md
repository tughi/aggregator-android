---
layout: page
title: Support
home_link: 1
---

Please don't use email to request features or report bugs. 

{% assign release = site.tags.release[0] %}
> Before creating a new issue, please make sure that you already use the latest app version: [{{ release.title }}]({{ release.url | relative_url }}).

## Feature requests

If you have a feature request, please [check the existing issues]({{ site.github.repository_url }}/issues) first, and if no one else has already requested it, then [create a new issue]({{ site.github.repository_url }}/issues/new/choose).

## Bugs

If you think you found a bug, please follow the same steps as in case of a feature request.

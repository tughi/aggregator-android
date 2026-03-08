---
layout: default
title: Aggregator
tags: classic
no_breadcrumb: true
---

This is the news feed for the classic Aggregator app:

{%- assign classic_posts = site.classic | sort: "date" | reverse %}
{%- for post in classic_posts %}
- [{{ post.title }}]({{ post.url | relative_url }}) — {{ post.date | date: "%B %-d, %Y" }}
{%- endfor %}

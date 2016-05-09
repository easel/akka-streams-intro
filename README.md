# Akka Streams Intro

## View the Presentation

This presentation officially lives at http://erik.labianca.org/akka-streams-intro

## Abstract

Streams are probably the most overused word in the modern programming vocabulary.
We'll start by taking a look at what a stream is, why reactive streams are
important in the modern world of exploding data and connectivity and
put some context around Reactive Streams, Akka Streams, and the rest of the ecosystem.
Finally, we'll look at some specific use cases where Akka Streams can improve
performance, increase reasonability, and solve problems that are
difficult if not impossible to solve otherwise.

## About the Presenter

Erik LaBianca is software developer and entrepreneur who spends his time
solving problems at the intersection of software, data, and the real world. Erik
has been programming Scala since 2010 and enjoys exploring the ecosystem around it,
from data analytics to functional programming and reactive systems. When not
programming Scala, Erik enjoys spending time with his family
and playing ice hockey. Erik is also a co-founder at Seventh Sense and currently
serves as CTO.

## Building the presentation

This presentation is built with tut and reveal.js. The easiest way to work on it
is to run both of the following commands in seperate shell windows:

- `sbt ~tut`
- `npm start`

This will update the presentation pages in `slides/`, and serve the updated
pages at `http://localhost:8001/` with livereload enable for easy viewing.

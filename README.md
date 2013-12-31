# Collage

Collage is a simple-to-use image processing library for Clojure. It's intended
to be a drop-in solution for high-level image processing needs. It draws some
inspiration from Python's [PIL](http://effbot.org/imagingbook/pil-index.htm) and
is somewhat similar to mikera's [imagez](https://github.com/mikera/imagez).

### Motivation
Collage grew out of my own need when I was writing another Clojure application.
I wanted to have some high-level image processing functionality available-
functions into which I could just pass some `BufferedImages` and get back
transformed `BufferedImages`. At the time, I resorted to doing Java interop,
which is nice, but Clojure is nicer. I also wanted to get more experience with
Clojure in general.

### Project goals
* Ease of use
* Well tested-ness
* Clean, composable internal API
* Learn (more idiomatic) Clojure

## Usage

#### Using the `with-image` macro.
```clj
(:require [fivetonine.collage.core :refer :all])

(with-image "/path/to/image.jpg"
  (resize :width 1000)
  (rotate 90)
  (save :quality 0.85))
```

Loads the image at `/path/to/image.jpg`, resizes it to have width of 1000px
(height is computed automatically), rotates 90 degrees clockwise and saves it
with 85% quality of the original, overwriting the original.

```clj
(:require [fivetonine.collage.util :as util])
(:require [fivetonine.collage.core :refer :all])

(def image (util/load-image "/path/to/image.jpg"))
(with-image image
	        (crop 100 50 200 100)
	        (save "/path/to/new-image.jpg" :quality 1.0))
```

Loads an image at `/path/to/image.jpg`. With that image, crops a 200px by 100px
image out of the original, at the point (100,50) in the original image, saves
it with 100% quality at `/path/to/new-image.jpg`.

#### Vanilla functions.
```clj
(:require [fivetonine.collage.core :refer :all])

(def image (load-image "/path/to/image.png"))
(save (flip image :horizontal))
```

## Contributing

Contributions, suggestions and friendly criticism are all welcome.

## License

Copyright © 2013 Karl Sutt

Distributed under the Eclipse Public License either version 1.0.

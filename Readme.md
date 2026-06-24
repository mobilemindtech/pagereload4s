# Reload for ScalaJS

## Use g8 template

```shell
sbt new mobilemindtech/reload4scalajs.g8 
```

## Options

### Tasks

* `livereloadServe` Start http server
* `livereloadWatch` Start file watcher
* `livereloadStart` Start the reload plugin
* `livereloadStop` Stop the reload plugin

### Settings

* `livereloadWatchTarget` Target path to watch changes
* `livereloadCopyJSTo`  Destination folder to copy js after compilation"
* `livereloadPublic` Static dir to serve
* `livereloadPublicJS` JS folder to serve. Default value is assets/js
* `livereloadWatchPublic` If should watch dist folder. Default value is true if livereloadPublic is defined, otherwise false. If true, livereloadPublic must be defined.
* `livereloadDebug` Run plugin on debug mode
* `livereloadServerPort` Http server port. Default value is 10101.
* `livereloadExtensions` File extensions to watch
* `liveReloadUrl` External URL to reload when JS change

### Configs

##### Server configs

```sbt 
livereloadDebug := Some(true)
livereloadServerPort := Some(10101)
livereloadExtensions := Some(List("js", "map", "css", "jpg", "jpeg", "png", "ico", "html"))
copyJsToFile := baseDirectory.value / "public" / "assets" / "js" / "main.js"
livereloadPublic := Some(baseDirectory.value / "public"),
```

#### Use plugin to copy files to external location.

This plugin can be used to copy the generated JS to an external project, for example to the golang or node application. 
In this case we need to specify the location where the js will be copied and the folder we want to monitor changes for autoreload. 

Config example:

```shell
my-go-app/
  public/
    js/
my-scalajs-app/
  src/
```

```sbt
.settings(
    livereloadWatchTarget := Some(baseDirectory.value / ".." / "my-go-app" / "public")
    livereloadCopyJSTo := Some(baseDirectory.value / ".." / "my-go-app" / "public" / "js")
    copyJsToFile := livereloadCopyJSTo.value.get
)
.settings(
  Seq(fullOptJS, fastOptJS)
    .map(task => (Compile / crossTarget) := livereloadCopyJSTo.value.get)
)
```

#### Use plugin to serve files

We can use the plugin in a quick project, or single page application, 
where everything we need is inside the public folder.

Config:
```sbt 
copyJsToFile := baseDirectory.value / "public" / "assets" / "js" / "main.js"
livereloadPublic := Some(baseDirectory.value / "public")
livereloadPublicJS := Some(baseDirectory.value / "public" / "assets" / "js")
livereloadWatchPublic := Some(true)
```

## Example

### plugins.sbt

```sbt
addSbtPlugin("br.com.mobilemind" % "livereload" % "0.3.0")
```

### Public folder

```shell
app/
    src/
    public/
      index.html
      assets/
        js/
```

### build.sbt
```sbt

ThisBuild / name := "example"
ThisBuild / scalaVersion := "3.8.4"

lazy val app = (project in file("."))
  .enablePlugins(ScalaJSPlugin, Reload4ScalaJSPlugin, CopyJSOnCompilePlugin)
  .enablePlugins()
  .settings(
    name := "example",
    scalaJSUseMainModuleInitializer := true,
    livereloadPublic := Some(baseDirectory.value / "public"),
    copyJsToFile := baseDirectory.value / "public" / "assets" / "js" / "main.js",
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-dom" % "2.8.1"
    )
  )
	
```

## Usage

- Add on HTML page
```
<script type="text/javascript" src="http://localhost:10101/js/livereload.js"></script>
```

- In sbt console execute

```
sbt:appjs> livereloadStart
```

- Run ~fastLinkJS to compile scalajs files.

```
sbt:appjs> ~fastLinkJS
```

- Done, the HTML page will be reloaded.

### Test project

- On appjs folder, run sbt

```
sbt:appjs> livereloadStart
```

- Open test html on http://localhost:10101/sample/index.html.

```
sbt:appjs> ~fastLinkJS
```

- Change `Main.scala` and save to HTML reload.


## Publish

local

```
sbt:appjs> sbt publishM2
```

maven central

```
sbt sonatypeBundleRelease
```
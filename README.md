# Elasticsearch json-api Ingest Processor

Fetches data from an external JSON API and extracts a subset into a target field

## Usage


```
PUT _ingest/pipeline/json-api-pipeline
{
  "description": "A pipeline to do whatever",
  "processors": [
    {
      "json_api" : {
        "field": "ip",
        "target_field": "country",
        "json_path": "country_name",
        "ignore_missing": true,
        "multi_value": false,
        "url_prefix" : "http://freegeoip.net/json/{}"
      }
    }
  ]
}

PUT /my-index/my-type/1?pipeline=json-api-pipeline
{
  "ip" : "216.102.95.101"
}

GET /my-index/my-type/1
{
  "ip" : "216.102.95.101",
  "country": "United States"
}
```

## Configuration

| Parameter | Use |
| --- | --- |
| ingest.json-api.cache_size   | Configure the in-memory on-heap cache size. This cache is backed by `org.elasticsearch.common.cache.Cache` and is shared by all ingest pipelines using this plugin on a given elasticsearch node. (Default: 1000)  |

## Setup

In order to install this plugin, you need to create a zip distribution first by running

```bash
gradle clean check
```

This will run tests and produce a zip file in `build/distributions`.

After building the zip file, you can install it like this:

```bash
bin/plugin install file:///path/to/ingest-json-api/build/distribution/ingest-json-api-0.0.1-SNAPSHOT.zip
```

## Contributing

Make sure you have a modern Gradle installed, as the plugin uses it as its build system.

IntelliJ users can automatically configure their IDE: `gradle idea` then `File->New Project From Existing Sources`. Point to the root of the source directory, select `Import project from external model->Gradle`, enable `Use auto-import`. Additionally, in order to run tests directly from IDEA 2017.1 and above it is required to disable the IDEA run launcher, which can be achieved by adding `-Didea.no.launcher=true` [JVM option](https://intellij-support.jetbrains.com/hc/en-us/articles/206544869-Configuring-JVM-options-and-platform-properties) and restarting IntelliJ. You may also need to remove some JARs from the default classpath (e.g., `ant-javafx.jar`) in order to fix Jar hell errors. To do so, edit `Project Structure...->SDK->Classpath`.


## Credits

The plugin logic is based on Kosho Owa's [ingest-http](https://github.com/kosho/ingest-http) plugin. I tried to fork it and 
backport in Alexander Reelsen's [cookiecutter](https://github.com/spinscale/cookiecutter-elasticsearch-ingest-processor)
but couldn't figure it out, so I started from scratch. Thanks Kosho and Alexander for the jump starts.
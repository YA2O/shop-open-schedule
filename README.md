# Opening hours

## About
Web server with a single endpoint to present a restaurant's opening hours in a human-readable format.

An [OpenAPI schema](./opening-hours.yaml) is available, as well as an [API documentation](./opening-hours.html)
to open in a browser (generated with this [script](https://gist.github.com/oseiskar/dbd51a3727fc96dcf5ed189fca491fb3)).

## Technical stack
I chose libraries which enable to use a Functional Programming style in Scala:
* *Cats Effects*: pure asynchronous run-time
* *Http4s*: minimal, idiomatic Scala interface for HTTP service
* *Circe*: JSON (de)serialization
* *Ciris*: functional configurations

## Environment
The following versions were used during development:
* Scala 2.13.11
* sbt 1.9.4
* Java 17.0.8 (17.0.8-zulu installed via sdkman!)
* MacOS Ventura 13.6


## Instructions

First, be sure to be inside the project's folder.
`cd opening-hours`

### Run the server with sbt
* To start the server, run this command in the terminal:
```
sbt run
```
This will start a web server on port `8080`.

* The server port is configurable. If you want to specify another value than the default, start the server
as follows:
```
sbt run -Dserver.port=9000

```

* Stop the server by pressing `Ctrl+C`

### Run the server in a Docker container
* Note: Docker needs to be installed.

* Create a Docker image and publish it by running the command in 
the terminal:
`sbt docker:publishLocal`
(note that you might get "errors" which are false negatives and can be ignored)

* Run the image inside a Docker container:
`docker run -d --name opening-hours-service -p 8080:8080 ya2o/opening-hours:1.0.0`

* Stop the container:
Get the id of the container by running:
`docker ps`
and then:
`docker stop $CONTAINER_ID`
(if this is the last container that has been published to Docker, you can run: `docker stop $(docker ps -ql)`)
 
* Remove the container from Docker:
`docker rm $CONTAINER_ID`
(if this is the last container that has been published to Docker, you can run: `docker rm $(docker ps -ql)`)

* Remove the image from Docker:
`docker rmi ya2o/opening-hours:1.0.0`

### Send requests 
* Send a request from a file:
```
curl -v http://localhost:8080/opening-hours/v1 --json @src/test/resources/input1.json
```
 
* or directly:

```
curl -v http://localhost:8080/opening-hours/v1 \
--json \
'{
    "monday": [],
    "tuesday": [
        {
            "type": "open",
            "value": 36000
        },
        {
            "type": "close",
            "value": 64800
        }
    ],
    "wednesday": [],
    "thursday": [
        {
            "type": "open",
            "value": 37800
        },
        {
            "type": "close",
            "value": 64800
        }
    ],
    "friday": [
        {
            "type": "open",
            "value": 36000
        }
    ],
    "saturday": [
        {
            "type": "close",
            "value": 3600
        },
        {
            "type": "open",
            "value": 36000
        }
    ],
    "sunday": [
        {
            "type": "close",
            "value": 3600
        },
        {
            "type": "open",
            "value": 43200
        },
        {
            "type": "close",
            "value": 75600
        }
    ]
}'
```

## Assumptions and decisions
* See the [specification](./specification.pdf). 
* A closing must be less than 24 hours after its corresponding opening. This is to avoid ambiguity; e.g. the 
opening period `Monday: 1 AM - 1:30 AM`, could be closing on `Monday 1:30 AM`, or on `Tuesday 1:30 AM`.
* Entries for closed days (like e.g. `wednesday: []`) can be omitted in the request.
* The API is lenient for bad casing of day names and event types (i.e. `open` and `close`).
* I decided to have a JSON response. I didn't want to mix a request with `Content-Type: application\json` and 
a response with with `Content-Type: text\plain`. And JSON is convenient to work with for the client.
* Rather than a multiline string in JSON (using line breaks `\n`), I opted for responding with an array of 
strings. Line breaks can be problematic for the client, depending on the client's language and platform. 
* No authorization is implemented. It feels overkill in this case.
* No metrics nor health check endpoints are implemented. Often nowadays, the infrastructure takes care of 
this; metrics are built-in in Heroku, Amazon has Amazon CloudWatch, etc. If custom metrics are needed, extra
work is needed here.


## A word about the data format of the input

I would suggest to use this format instead:

```
'
{
    "schedule": [
        {     
            "opening": {
                "day": "monday"
                "time": "11:00"
            }
            "closing":  {
                "day": "monday"
                "time": "15:30"
            }
        },
        {     
            "opening": {
                "day": "monday"
                "time": "19:00"
            }
            "closing":  {
                "day": "tuesday"
                "time": "01:00"
            }
        },
        etc...
   ]     
}
'
```

### Benefits:
* this is closer to how I'd expect a "domain expert" would think/communicate about the opening hours of a 
restaurant.
* this is closer to the data structure used in the model (`List[OpeningPeriod]`), and thus would save us some
  conversion trouble.
* the time format is human-readable, and therefore developer-friendly; especially useful when debugging or 
testing.
* openings and closings are always coming in pairs: we would skip the complexity of pairing opening and closing 
events, and handling errors when the pairings are incorrect/incomplete.
* we wouldn't need any key decoder (`KeyDecoder[DayOfWeek]`).
  (You could argue that I could have hard-coded the 7 days instead. But this is ugly:
```case class Input(monday: List[EventAtTime], tuesday: EventAtTime, ...```
)
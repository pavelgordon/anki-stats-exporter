## Anki stats exporter
Fetches statistics of learned cards from Anki Desktop and renders them as html page
## Tech/framework used
- Kotlin
- Ktor
- Docker
- Gradle
- [jib gradle plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin)
- [Anki connect](https://github.com/FooSoft/anki-connect)  

## Installation
- Install Kotlin
- Install Gradle
- Install Anki Desktop with [AnkiConnect addon](https://ankiweb.net/shared/info/2055492159)
- Install Docker(optional)
- Clone project to local machine.
## How to run?
### Via IntelliJ
* Run `Application.kt`
* Or run IntelliJ profile from .run directory
#### Via Jar
* `./gradlew build`
* `java -jar build/libs/anki-stats-exporter-0.0.1.jar`
#### Via Docker
* `docker build -t anki-stats .`
* `docker run -m512M --cpus 2 -it -p 8080:8080 --rm anki-stats`
## How to use
* Call `GET localhost:8080` to view dashboard(work in progress)
* Call `GET localhost:8080/anki/stats` to get decks stats from running Anki Desktop
* Call `POST localhost:8080/stats` to manually put stats to storage(see requests.http)

Right now, deck names are hardcoded: Italiano, German
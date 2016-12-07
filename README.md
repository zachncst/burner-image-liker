# Burner Image Uploader and Liker!

## Description

This is a scala application running finch and using finagle to call dropbox to upload images. 
When a message is sent to a burner number and this app is integrated it will call the event
end point and perform a few different actions. If the message is an iamge type, it will
upload this photo to dropbox and store it in an in memory DB (Map) to track how many people
text burner with the name of the image. If the type of message is a text message and
the message has the image name in the text it will add one like to the in-memory database.

## Structure

| filename | desc |
| ------------------ | -----------------|
| DropboxClient.scala | Dropbox API calls |
| ImageDB.scala | Simple in-memory DB for images|
|ImageLikerService.scala | Endpoints and business logic|
|WebServer.scala | Webserver|

## To build

SBT is required to build

## How to run

The easiest means is by running this command:

```bash
sbt run main
```

## Config

An example config file is provided under src/main/resources named resources.cfg. The format
looks like this:

```hocon
port = 8080
dropbox {
    token = "your token"
    folder = "folder you want in dropbox"
}
```
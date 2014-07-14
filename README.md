# Lightstreamer - Leap Motion Demo - Java Adapter #
<!-- START DESCRIPTION lightstreamer-example-leapmotion-adapter-java -->

The *Leap Motion Demo* is a simple application showing the integration between a [Leap Motion Controller](https://www.leapmotion.com/) and the [Lightstreamer JavaScript Client library](http://www.lightstreamer.com/docs/client_javascript_uni_api/index.html).

This project shows the Data Adapter and Metadata Adapters for the *Leap Motion Demo* and how they can be plugged into Lightstreamer Server.

As example of a client using this adapter, you may refer to the [Lightstreamer - Leap Motion Demo - HTML (LeapJS, Three.js) Client](https://github.com/Weswit/Lightstreamer-example-LeapMotion-client-javascript) and view the corresponding [Live Demo](http://demos.lightstreamer.com/LeapDemo/).

## Details

This demo displays a game field containing some small blocks. Each block is controlled by a different user connected to the same application through a Leap Motion Controller device. 

This adapter maintains a representation of the world containing all of the users' blocks and updates it accordingly with the clients commands.
Each block in the world can be in either one of the following statuses:

* Grabbed: in this case all the positions are sent to the Lightstreamer server by the client. The server updates the world without any 
modification on the received position, and broadcasts the positions back to all the connected clients. 
* Released: the client only sends to the Lightstreamer server the forces to apply to its object in the world. The server broadcasts such forces to 
the clients, that are then able to update the world representation themselves, while also keeping its own world updated by 
applying such forces. Every few seconds the server will also broadcast the current positions to re-synchronize the clients with the 
server positions. 

<!-- END DESCRIPTION lightstreamer-example-leapmotion-adapter-java -->

## Install
If you want to install a version of this demo in your local Lightstreamer Server, follow these steps.
* Download *Lightstreamer Server Vivace* (make sure you use Vivace edition, otherwise you will see a limit on the event rate; Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Get the `deploy.zip` file of the [latest release](https://github.com/Weswit/Lightstreamer-example-LeapMotion-adapter-java/releases), unzip it and copy the just unzipped `LeapDemo` folder into the `adapters` folder of your Lightstreamer Server installation.
* Download [croftsoft](http://sourceforge.net/projects/croftsoft/files/) library and compile a `croftsoft-math.jar` version. Please make sure to include: applet, io, inlp, lang and math packages.
* Copy the just compiled `croftsoft-math.jar` file in the `adapters/LeapDemo/lib` folder.
* Launch Lightstreamer Server.
* Test the Adapter, launching one of the clients listed in [Clients Using This Adapter](https://github.com/Weswit/Lightstreamer-example-LeapMotion-adapter-java#clients-using-this-adapter).

## Build
To build your own version of `LS_leapdemo_adapters.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Weswit/Lightstreamer-example-LeapMotion-adapter-java#install) section above, follow these steps:
* Clone this project
* Get the `ls-adapter-interface.jar`, `ls-generic-adapters.jar`, and `log4j-1.2.15.jar` from the [Lightstreamer distribution](http://www.lightstreamer.com/download) and copy them into the `lib` folder..
* Download [croftsoft](http://sourceforge.net/projects/croftsoft/files/) library and compile a `croftsoft-math.jar` version. Please make sure to include: applet, io, inlp, lang and math packages, and Put the just compiled `croftsoft-math.jar` file in the `lib` folder.
* Build the java source files in the `src` folder into a `LS_leapdemo_adapters.jar` file. Here is an example for that:
```
 > javac -classpath ./lib/croftsoft-math.jar;./lib/ls-adapter-interface.jar;./lib/ls-generic-adapters.jar;./lib/log4j.jar -d ./classes ./src/com/lightstreamer/adapters/\LeapMotionDemo/*.java ./src/com/lightstreamer/adapters/\LeapMotionDemo/engine3D/*.java ./src/com/lightstreamer/adapters/\LeapMotionDemo/room/*.java
 > jar cvf LS_leapdemo_adapters.jar -C tmp_classes com
```
* Copy the just compiled `LS_leapdemo_adapters.jar` in the `adapters/LeapDemo/lib` folder of your Lightstreamer Server installation.


## See Also 

### Clients Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Leap Motion Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-LeapMotion-client-javascript)

<!-- END RELATED_ENTRIES -->

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer SDK for Java Adapters since 5.1


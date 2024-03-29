# Lightstreamer - Leap Motion Demo - Java Adapter #
<!-- START DESCRIPTION lightstreamer-example-leapmotion-adapter-java -->

The *Leap Motion Demo* is a simple application showing the integration between a [Leap Motion Controller](https://www.leapmotion.com/) and the [Lightstreamer Web Client library](https://lightstreamer.com/api/ls-web-client/latest/index.html).

This project shows the Data Adapter and Metadata Adapters for the *Leap Motion Demo* and how they can be plugged into Lightstreamer Server.

As an example of a client using this adapter, you may refer to the [Lightstreamer - Leap Motion Demo - HTML (LeapJS, Three.js) Client](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-client-javascript) and view the corresponding [Live Demo](http://demos.lightstreamer.com/LeapDemo/).

## Details

This demo displays a game field containing some small blocks. Each block is controlled by a different user connected to the same application through a Leap Motion Controller device. 

This adapter maintains a representation of the world containing all of the users' blocks and updates it accordingly with the clients� commands.
Each block in the world can be in either one of the following statuses:

* Grabbed: in this case, all the positions are sent to the Lightstreamer server by the client. The server updates the world without any 
modification on the received position, and broadcasts the positions back to all the connected clients. 
* Released: the client only sends to the Lightstreamer server the forces to apply to its object in the world. The server broadcasts such forces to 
the clients, that are then able to update the world representation themselves, while also keeping its own world updated by 
applying such forces. Every few seconds, the server will also broadcast the current positions to re-synchronize the clients with the 
server positions. 

<!-- END DESCRIPTION lightstreamer-example-leapmotion-adapter-java -->

### The Adapter Set Configuration

This Adapter Set is configured and will be referenced by the clients as `LEAPDEMO`. 

The `adapters.xml` file for the *Leap Motion Demo*, should look like this:

```xml      
<?xml version="1.0"?>

<adapters_conf id="LEAPDEMO">
    <metadata_provider>
        <adapter_class>com.lightstreamer.adapters.LeapMotionDemo.LeapMotionMetaDataAdapter</adapter_class>
    </metadata_provider>
    
    <data_provider>
        <adapter_class>com.lightstreamer.adapters.LeapMotionDemo.LeapMotionDataAdapter</adapter_class>
    </data_provider>
</adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, see the [Java In-Process Adapter Interface Project](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#configuration) for details.</i><br>
<br>
Please refer [here](https://lightstreamer.com/docs/ls-server/latest/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

## Install

If you want to install a version of this demo on your local Lightstreamer Server, follow these steps:
* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users; this should be preferred to using COMMUNITY edition, otherwise you would see a limit on the event rate) from [Lightstreamer Download page](https://lightstreamer.com/download/), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Get the `deploy.zip` file of the [latest release](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-adapter-java/releases), unzip it, and copy the just unzipped `LeapDemo` folder into the `adapters` folder of your Lightstreamer Server installation.
* [Optional] Customize logging settings in log4j configuration file `LeapDemo/classes/log4j2.xml`.
* Launch Lightstreamer Server.
* Test the Adapter, launching one of the clients listed in [Clients Using This Adapter](#clients-using-this-adapter).

## Build

To build your own version of `example-leapmotion-adapter-java-x.y.z.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity, only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```

## See Also 

### Clients Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Leap Motion Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-client-javascript)

<!-- END RELATED_ENTRIES -->

## Lightstreamer Compatibility Notes


- Compatible with Lightstreamer SDK for Java In-Process Adapters since version 8.0.
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 7.4.x, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-adapter-java/tree/last_for_interface_7.4.x).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters versions 7.0 to 7.3, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-adapter-java/tree/last_for_interface_7.3).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 6.0, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-adapter-java/tree/pre_mvn).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 5.1, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-LeapMotion-adapter-java/tree/for_Lightstreamer_5.1).
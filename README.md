IntentSniffer
=============

Tool for sniffing Recent activity as well as payload of SMS and future types of communications on Android devices.

## Getting Started

1. This is an Android Project. It is inspired by the Sniffing Tools that have come previously. You will need to download Ecplise, and setup the Android SDK. This requires Android 4+ SDK.

2. Compile and Run the project from within eclipse, the only dependency is the turbo HTTP Async client which allows us to run quick, async HTTP communication to the Web comonent for logging data.

3. For full use, the web interface is provided in the following <a href="https://github.com/isotlab/Sniffer-Web">project</a>, or your own can be written, it provides full REST usage for storing and saving data.

## How it works

1. The intent sniffer registers itself as a broadcast receiver for several components, currently it only registers for SMS broadcast send and receive-- we will extend this in the future.

2. Threads are created for monitoring recent activity based on the activity manager as well as real time data coming through broadcast intents.

3. Phones register themselves with the Sniffer-Web project, and send activity to the database for further processing.

4. The database can then be used to analyze trends and develop algorithms for use in intent modeling.

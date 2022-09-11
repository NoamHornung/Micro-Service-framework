# Micro-Service-framework
### A simple micro-service framework that uses various multithreading techniques, on top of which is implemented a system for managing the university compute resources. In the system students can train models, test them and publish their results.
  
the program takes as input a list of students and their models, GPUs, CPUs, conferences and the duration of the program in millisecs. an example input file is given in the root directory.
The output of the program is redirected to the file /src/main/java/file.json.
  
### Micro-Service framework
the Micro-Service framework consists of two main parts: A Message-Bus and Micro-Services. Each Micro-Service is a
thread that can exchange messages with other Micro-Services using a shared object
referred to as the Message-Bus. There are two different types of messages: 
- __Events__ - an action that needs to be processed. Each Micro-Service specializes in processing one or more types of events.
Upon receiving an event, the Message-Bus assigns it to the messages queue of an appropriate Micro-Service which is subscribed to this type of
event. Each event holds a result. This result should be resolved once the Micro-Service to
which the event was assigned completes processing it.
- __Broadcasts__ - global announcements in the system. Each Micro-Service can subscribe to the type of broadcast messages it is interested to receive.

### Compute Resources Management System  
Students can create the “TrainModel” event in order to start training a model. 
Once such an event is sent, a chain of events starts - the Message Bus inserts the event to a __GPU__ queue, the GPU processes the message by sending data batches to 
the __Cluster__ for the __CPUs__ to process, and trainig the processed data after receiving it from the Cluster (the GPUs have a limited amount of processed data they can store).
when the GPU finishes the training the event is resolved.  
In addition, when a student finishes training a model, he can create another event
“TestModel”, which will be handled by the GPU, and will return results.
if the results are good (defined randomly) he can publish its results.  
Results are submitted to a new type of Micro-Service, `Conference’, each conference will
aggregate results from students, and on a set predefined time, will Broadcast the
aggregated results to all the students.

#### command:
mvn compile  
mvn exec:java -Dexec.mainClass="bgu.spl.mics.application.CRMSRunner" -Dexec.args="example_input.json"

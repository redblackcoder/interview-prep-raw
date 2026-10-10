# Databricks Architecture Round — Distributed File System (transcript)

> Real interview, 2026-09-28, ~45 min. Databricks architecture round; interviewer from the multi-cloud efficiency org. Names redacted.
> Auto-transcribed (Databricks Notetaker + dictation); artifacts like "Wispr" are transcription noise.
> Whiteboard: [white-board.md](white-board.md).

## Prompt (as read aloud)
Customers ingest data into the lakehouse and run analysis and ML pipelines. Build a scalable, reliable file system to store it for all customers.
- Arbitrary number of files per directory. Files are immutable once written (no modify, no append). Directories are mutable.
- Cannot use existing cloud/blob storage.
- Scope: single region, single data center. Multi-region is a bonus.
- API: put file (dir, name, data), get file (path, offset, length), create directory, list directory, delete file.
- Clarified live: single file up to TBs, minimum KBs, total PBs; ~1000s read QPS, ~100s write QPS (read-optimized); directories one level deep.

## Transcript

Interviewer: Hey, hi.
Candidate (You):  Hey, [interviewer]. How are you doing today?
Interviewer:  Uh, I'm doing good. Uh, how are you doing?
Candidate (You):  Yeah, I'm doing good, Wispr. Thanks. Uh, sorry I got a couple of minutes late.
Interviewer:  No, no worries. Uh.
Candidate (You):  Thank you.
Interviewer:  Sorry, I'm just trying to, like, see what this Databricks Notetaker is. How do I hide it? Okay.
Interviewer:  Oh, it's okay. I guess this is, uh, sorry, I guess this is like an automated Notetaker.
Candidate (You):  Okay, okay. Sure.
Interviewer:  Uh, awesome. So yeah, I, uh, I guess just to introduce myself, I'm [interviewer]. I work in the multi-cloud efficiency org in Databricks.
Interviewer:  Uh, so our team deals with Databricks runs on multiple clouds. Uh, so we have costs across all these clouds.
Interviewer:  Uh, we get a bill from all the cloud providers, and we try to understand, uh, which infrastructure, which product is spending, uh, how much on each of these clouds, and then go one step further to figure out how do we optimize it, how do we improve the efficiency, how do we optimize it so that we can save cost, uh, on all these, uh, products. Uh, so yeah, I guess eventually it helps improve the product margin.
Interviewer:  Uh, so it's called multi-cloud efficiency.
Candidate (You):  Okay.
Interviewer:  Uh, I've been with Databricks for close to a year. Uh, before that, I was working at, uh, Google, uh, on a, I guess, similar infrastructure but a low-level infrastructure problem, uh, to capture, uh, sensing signals across, uh, all the Google infrastructure.
Candidate (You):  Okay.
Interviewer:  Uh, before that, I was at, uh, Facebook, uh, working on, I guess, configuration infrastructure. And before that, at a bunch of other companies. Uh.
Candidate (You):  I see.
Interviewer:  So yeah, that's, that's, that's about me. Do you want to introduce yourself?
Candidate (You):  Yeah. Um, uh, thanks for, like, giving some context. Uh, my name is [candidate].
Candidate (You):  I am currently at Slack, and, um, my team owns the, uh, the data catalog and the governance layer, uh, for the data at Slack. It is for, like, all the internal data that Slack has.
Candidate (You):  Um, my role is more of a tech lead and also hands-on IC. Um, so I've worked on features like where we integrate with more, uh, data platforms in Slack.
Candidate (You):  Uh, Slack uses, like, a lot of data platforms. We don't use Databricks, but we use, uh, Snowflake.
Candidate (You):  But we have, like, our internal Iceberg warehouse, uh, Pinot, Airflow. So, like, a lot of those things, and everything comes together, um, from the metadata perspective into the catalog.
Candidate (You):  And we provide service, uh, search, search service over it, so you can search across, like, different data sets to find the right one that you want to use. Uh, we have lineage engine, so it's like relationship between each data set and pipeline and whatnot.
Candidate (You):  And, um, third important piece is, like, the, uh, metadata enrichment. So we do, like, AI-based metadata enrichment and also, like, rule-based, so, like, data quality checks that we run, um, or, like, if the pipeline is delayed.
Candidate (You):  So all those kind of things. So this whole provides a trust layer, uh, to the customers who are trying to use our data, uh, to make decisions.
Candidate (You):  Uh, before that, I was at another team in Salesforce, which was also in the lakehouse, uh, area, but it was, um, the lakehouse for all the external customers, so not just, like, internal. Um, and there, I was, again, in the metadata team, um, which provided a service to kind of, um, represent the logical, um, mapping, like, logical object mapping to the physical objects that we stored, uh, in, in S3 for Wispr, um, and providing, like, uh, fast access to our various services which needed it.
Candidate (You):  Like, query engine was the primary one, uh, to kind of to figure out where the data is. It has to know, uh, that from the metadata lookup.
Candidate (You):  And also, we hosted a lot of, um, stats about the data so that the query engine can make, um, um, like, optimizations, like, okay, these files I don't want to read, these files I want to read, even within this file, which, um, uh, row groups I want to read, which is like a chunk in a file for a Wispr table. So that's been last three to four years, uh, across these two teams.
Candidate (You):  Uh, before that, I was working with, uh, Tableau team, uh, again, in, I would say, like, more in a middleware service teams. Uh, and before that, I was with, uh, Microsoft at Bing, um, working in search platform, um, team.
Candidate (You):  Uh, so building, like, yeah, like the answers platform, which is a lot of customers use, a lot of internal teams use to build, um, solutions on Bing.
Interviewer:  Awesome. Uh, awesome. Thank you.
Interviewer:  Thank you for the detailed, uh, introduction. Uh, I, I, I guess before I start the interview, just to make sure, uh, this aligns with you, this is supposed to be an architecture round.
Interviewer:  Uh, we'll go over, like, architecture question.
Candidate (You):  Yeah.
Interviewer:  Okay, perfect.
Candidate (You):  Okay.
Interviewer:  Uh, so yeah, if you're logging to Codepad, I have posted the question.
Candidate (You):  Okay.
Interviewer:  Uh, I just want to give you sufficient time to go through the question so we can quickly hop onto the Codepad.
Candidate (You):  Yeah.
Interviewer:  Uh.
Candidate (You):  Let me log into that.
Interviewer:  And just like, I guess, like, uh, any other architecture question, please feel free to drive the design, uh, ask me any clarification question. We are not implementing the code.
Candidate (You):  Okay.
Interviewer:  Uh, this is supposed to be, like, a high-level architecture. Uh, we are trying to build a system.
Candidate (You):  Okay.
Interviewer:  Uh, I might try to, I guess, steer the interview at few points if, uh.
Candidate (You):  Okay.
Interviewer:  Uh, I get the sufficient signal. But yeah, uh, feel free to take the lead on the design.
Candidate (You):  Got it. Uh, all right. Do you want me to share screen or, uh.
Interviewer:  Uh, no, I'm, I'm.
Candidate (You):  You can see it.
Interviewer:  Okay.
Candidate (You):  Okay.
Interviewer:  Yes, yes.
Candidate (You):  All right. Um, okay, let me read it up. Um, customer ingests their data into the lakehouse and run analysis and ML pipelines.
Candidate (You):  We want to build a scalable and reliable file system to store all of that data for all our customers. Okay.
Candidate (You):  So the constraint are customer can store an arbitrary number of files in directory. Okay.
Candidate (You):  Files are immutable once written, no modify, no append. Directories are mutable.
Candidate (You):  You can create files and some directories in them. You cannot use existing cloud and blob storage.
Candidate (You):  Okay. Um, scope is single region, single data center.
Candidate (You):  Multi-region is a bonus. Okay.
Candidate (You):  So we're talking about, um, okay, so files and okay, so it seems like you, when you create something, you put the whole data, like a binary blob in the API, uh, for a particular path, and you create it.
Candidate (You):  But you can read a portion of that file, um, based on the offset and length. We have, uh, create directory, list directory, and delete a particular file, uh, from the system.
Candidate (You):  Okay. Um, yeah.
Candidate (You):  So I think the interesting thing from this APIs, um, is, yeah, when I think of, like, interface of S3, for example, it's mostly about objects.
Candidate (You):  Um, the additional layer here, it feels like it's the directory, uh, bit, which, um, generally doesn't exist on, uh, blob storage. So it seems like there's, like, a mix of a couple of things happening here.
Candidate (You):  Um, yeah. I'm also thinking, like, for the, um, get file API, when we are trying to get the particular offset for a file, um, we are just talking about, like, okay, like, these are just, like, byte arrays.
Candidate (You):  So we don't have to worry about, okay, what's in the file. We are just storing a byte array.
Candidate (You):  And then from that byte array, um, read a particular offset and a length. Okay.
Candidate (You):  So, um, few things that I'm considering is these files are also immutable. Okay.
Candidate (You):  Um, and are there, like, any constraint on the size of the data that you are putting in this file, or we assume a range? Uh, what does look like?
Interviewer:  Yeah, yeah, that's a good question. So we should assume that the file could be terabytes of data, a single file.
Candidate (You):  Okay.
Interviewer:  And we are trying to build, like, a petabyte storage system. So yeah, you, the overall size of the entire storage could be petabytes, but a single file itself could be more than a terabyte.
Candidate (You):  Okay. So file size can range up to, say, few terabytes. And overall size is petabytes.
Candidate (You):  Um, okay. Is there also, like, a lower limit on the file size?
Candidate (You):  How or we can assume, like.
Interviewer:  Yeah, I guess it could be a.
Candidate (You):  Like, few kilobytes.
Interviewer:  Yeah, I would say KBs. Yeah, exactly.
Candidate (You):  Yeah, yeah. Okay.
Candidate (You):  Okay. So, um, all right.
Candidate (You):  And then we have a bunch of directories that seems like, uh, so path becomes a directory path. And then for each directory, for each path, list everything underneath it.
Candidate (You):  These directories, subdirectories in it. Okay.
Candidate (You):  So directories are also nested. I'm thinking, will my design care how much depth is in the directory?
Candidate (You):  Um, yeah, I, I, I, yeah, I haven't thought through the design, but yeah, for now, I will assume it can be arbitrary deep.
Candidate (You):  But if we find some issues, maybe we'll come back to and see if we can limit how much depth we want to support. Um, but yeah, for now, I'll assume they are, like, arbitrary deep, arbitrary depth.
Candidate (You):  Um, for directories.
Candidate (You):  And when we are doing a put file, so suppose user creates a directory at, um, A, B, C, um, then, like, are these direct will everything become a directory at this point?
Candidate (You):  So, like, A, B, and C all will be directories? Um.
Interviewer:  I, I, I think we can keep it simple. We can say, create directory, there's a path provided.
Candidate (You):  Yeah.
Interviewer:  Uh, so directory name would be, uh, yeah, I, I guess just for our design for now, let's assume a simple architecture. Let's say there is a test directory. You use create directory using test directory slash.
Interviewer:  And then you say, uh, put file.
Candidate (You):  Yeah.
Interviewer:  And then you pass in the test DR. And then, uh, the file name.
Candidate (You):  Okay.
Interviewer:  Or something. So yeah, that way you don't have to worry about, like, the nested. Exactly.
Interviewer:  We can keep it one level for now, uh, and then, uh, extend later.
Candidate (You):  Yeah, makes sense.
Candidate (You):  Okay. And so let's, uh, let me think about, like, um, no modify, no append.
Candidate (You):  So once the file get created, it's you can just read it, but you can delete it. Okay.
Candidate (You):  So, um, are there, like, uh, do you have, like, how many writes we are supporting, uh, on this system? Like, how frequent these files are being written and read, uh, from the system?
Candidate (You):  So, like, say, write QPS or, like, uh, read QPS on the system that you want to design for?
Interviewer:  Uh, yeah, I guess we can, uh, assume a reasonable number. So we can.
Candidate (You):  Okay.
Interviewer:  Uh, optimize more on the read part because writes tend to happen, uh, less frequently.
Candidate (You):  Okay.
Interviewer:  So we can.
Candidate (You):  Okay.
Interviewer:  Yeah, I, I don't know, 1,000.
Candidate (You):  Okay, reasonable.
Interviewer:  QPS per read.
Candidate (You):  Yeah.
Interviewer:  Hundreds of QPS per write.
Candidate (You):  Okay, okay. So read to write ratio could be.
Interviewer:  Large.
Candidate (You):  Uh, you said 10.
Interviewer:  Exactly.
Candidate (You):  But we can assume even higher.
Interviewer:  Yes.
Candidate (You):  Yeah, something like that. Okay. And then reasonable, like, 100 for.
Interviewer:  Yeah, hundreds of QPS with thousands.
Candidate (You):  Yes.
Interviewer:  Of QPS.
Candidate (You):  Okay.
Interviewer:  Because of that.
Candidate (You):  So, um, let's see. Um, can we switch to a whiteboard? Oh, what's the drawing mode?
Candidate (You):  I haven't used.
Interviewer:  Yeah, yeah, there is a drawing mode here. So if you click on that, uh.
Candidate (You):  Yeah.
Interviewer:  It opens up the Excalidraw.
Candidate (You):  Okay. Oh, okay, okay, okay, okay. Cool, cool, cool.
Candidate (You):  All right. So because we are not using the program, I'll just, yeah, put it.
Candidate (You):  All right. So when I'm thinking about, like, creating it, let's start with something very simple.
Candidate (You):  Yeah, we, we need, we need a, a server where which has, like, either network attached storage or, like, local storage. Um, we'll kind of abstract that out that, okay, something is providing us a certain capacity with certain IOPS that we can attach to our server.
Candidate (You):  Um, so we have, say, a bunch of disks, uh, available on a node, um, that we can write to. Okay.
Candidate (You):  So let's see. These are my disks on a server.
Candidate (You):  So this is my storage service. Maybe let's call it that.
Candidate (You):  Right. And the writes coming to it, it's just writing.
Candidate (You):  So it, um, okay.
Candidate (You):  So on top of it, like, something needs so we have, like, multiple of these nodes.
Candidate (You):  And on top of this, we have something which is creating, um, kind of mapping of a file to where that file lives.
Candidate (You):  Um, so let's say if that is my, I'll call it metadata service. That is my metadata service.
Candidate (You):  Okay. So metadata service because it needs to keep, uh, mapping.
Candidate (You):  So we need some kind of a DB to keep that mapping for metadata service to use. Um, all right.
Candidate (You):  So let's say this is my DB.
Candidate (You):  Um, it will have, let's say, file path.
Candidate (You):  Um, let me think. For the file path, do I let me consider, like, a file ID.
Candidate (You):  Okay. If I have a file ID and then, um, so when the file is coming in, we talked about, okay, the file is pretty big.
Candidate (You):  So we want to somehow, um, break the file into multiple pieces.
Candidate (You):  And we don't want to store a huge file in one place. For instance, like, our disk cannot support it or, you know, space is not in there.
Candidate (You):  So we don't want to have, like, all our disks to be so big that it can, uh, sufficiently store them. Uh, we can consider, like, some, some number which is, um, reasonable that we think we will stripe the file into.
Candidate (You):  So let's say we choose, um, like, 4 megabyte. I don't know.
Candidate (You):  Actually, if I choose 4 megabyte, we said, like, we also have files which are few kilobytes. So we want to choose some kilobytes as a number.
Candidate (You):  Let's say 4 KB is our number. And we assume, okay, all the files are that size.
Candidate (You):  If a file is smaller than that, we will still pack into 4 KB. So we'll, yeah, won't be very efficient, but lose some space.
Candidate (You):  But at least that's our, uh, unit. So we're saying, like, okay, 4 KB is the unit of storage for a file segment.
Candidate (You):  All right. So, um, so then I don't have file ID.
Candidate (You):  I need two mappings. So let's see what I need.
Candidate (You):  So I have a file segment. Um, so I have a file, say, which is identified by a path.
Candidate (You):  And then it generates a bunch of segments. Okay.
Candidate (You):  And then those segments, I need to write to my disk. All right.
Candidate (You):  So, um, so what interface will I use to store them on the disk?
Candidate (You):  Let's say I'm creating files on my disk, right? So I have so those are my storage.
Candidate (You):  Um, storage unit is, like, a file on the disk. So if I say, um, my file, let's say it is 1 gigabyte, um, as an example, and I create, like, 4 KB segments, then I'm talking about, um, let's see, 1,024, 1,024.
Candidate (You):  That's the kilobytes by 4. So if I approximate, that's, like, 250,000, 250K segments.
Candidate (You):  Okay. So I definitely don't want to create one file for each segment.
Candidate (You):  Okay. That, that will make things very difficult.
Candidate (You):  Um, yeah, our OS, like, so storage service, like, if it is managing many, many small files, then, um, the OS has to manage metadata about all those files, which can be very big. And then there are obviously some high limits.
Candidate (You):  I don't remember what those limits are for, let's say, Linux. But they're, like, as the number of files increase, those metadata lookups itself will get slow on the OS layer.
Candidate (You):  So we don't want to create one file for each segment. So somehow we want to, um, combine these segments into a single file on storage.
Candidate (You):  So, um, combine multiple segments into one file. Okay.
Candidate (You):  So if I have that, uh, let's say, okay, let me okay, I'll come back to DB later.
Candidate (You):  So if I zoom into one of the, uh, storage service nodes. So in the storage service node, I said I will I'm representing the storage as a file.
Candidate (You):  So if this is my file, um, right, so this is my storage file.
Candidate (You):  Then I'm storing the content. And here I'm writing these segments.
Candidate (You):  So a bunch of segments will come in, uh, and get written here.
Candidate (You):  Right. So to represent a file, I will need so which node?
Candidate (You):  Or maybe I'll say, hmm, okay, so let's say storage file identifier.
Candidate (You):  And then within that storage file identifier, um, I have an offset. So where it starts and then the length that it goes to.
Candidate (You):  So this is, say, um, my file is composed of so my logical file is composed of an array of these, uh, objects. So I have a storage file ID, what the offset, and how much it is written in that.
Candidate (You):  And yeah, maybe it's, like, a it's in the order. So to read anything, I just can go through this order.
Candidate (You):  If I have to do a byte offset, then basically, yeah, I have to look through this. I have to figure out, like, how to store this in some data structure so I can do mapping from particular offset to the storage file ID and the offset within that to read.
Candidate (You):  Uh, but that this is my mapping. So now if I'm talking about this, um, the second layer is how do I, um, storage file IDs?
Candidate (You):  So how do I map these storage files to my storage service? So that's, that's the second level of mapping which I was considering in my metadata service.
Candidate (You):  Um, like, where this storage file lives on which storage service.
Candidate (You):  So that's what the metadata service is handling. So I have my DB here, bringing it back.
Candidate (You):  And then we are talking about.
Interviewer:  I, I guess a quick question. Is it possible to add, like, arrows to understand, like, how the request would flow?
Candidate (You):  Yeah.
Interviewer:  Probably can, uh, uh, take a simple, I don't know, I, I guess put file probably would be the first one.
Candidate (You):  Yeah.
Interviewer:  Uh, and show, like, how the request would go through so that.
Candidate (You):  Yeah.
Interviewer:  I, I can understand how these components are connected.
Candidate (You):  Yeah. Let me put, uh, what I want to store in DB and then we'll walk through.
Interviewer:  Yeah.
Candidate (You):  Some case.
Interviewer:  Perfect.
Candidate (You):  Uh, okay. So here in DB, I'm storing, uh, logical file.
Candidate (You):  So I have my logical file, um, path. And it is mapped to, um, the it's mapped to storage file ID, uh, offset and length, uh, array of that.
Candidate (You):  And, okay. And then I also need, um, mapping of storage file identifier to the storage service node.
Candidate (You):  So let's say storage node ID. Okay.
Candidate (You):  So these are the two things that I'm currently storing in DB. Um, okay.
Candidate (You):  One and two. All right.
Candidate (You):  So when the request, say, comes in for, uh, storing, let's say the request will come in with, say, put. So we'll layer on the directory.
Candidate (You):  I'm considering directory purely as, like, a metadata. Um.
Interviewer:  Okay.
Candidate (You):  Layer, layer. So we can.
Interviewer:  Yep.
Candidate (You):  Yeah. We can stitch them in some other DB or maybe in the same DB. So the my put request is, let's say, um, some directory.
Candidate (You):  So we'll figure out the directory mapping later. And then the file name and then the data.
Candidate (You):  Okay. So my file name plus dir, maybe that becomes my okay, that will become my, uh, file path, logical file path.
Candidate (You):  So logical file path will become dir plus file name. Um, yeah, the system should ensure it is unique.
Candidate (You):  Um, yeah. Like, I think the delete might be interesting.
Candidate (You):  We'll come back to that. But assuming this is my path, so metadata service will get a request.
Candidate (You):  Okay. This is my logical file path.
Candidate (You):  Um, data probably will not come all at once. So we'll layer that on top.
Candidate (You):  But assuming we have some data, um, which one request can help. So, um, let's see.
Candidate (You):  So when the request comes in, it so the meta service based on the data. So I have the data.
Candidate (You):  And I have so it will create it will it need to find where this file will go.
Candidate (You):  So that is the first decision that the meta service is doing. Okay.
Candidate (You):  Then I need to find where to store this file.
Candidate (You):  So when it is making that decision, where to store this file, it is when it makes that decision and then it stores it in the DB. So it identifies, um, so metadata service, it has, um, information about how much of these storage files are full, um, where I can write.
Candidate (You):  So it keeps, uh, another data. Let's see.
Candidate (You):  Um, yeah.
Candidate (You):  So it has okay.
Candidate (You):  So let's talk about, like, can we write to any file or do we need to so in my system, like, I will have many of these files.
Interviewer:  Okay.
Candidate (You):  Spread everywhere. So I'm considering, like, should be can we write to them in any order or do we want to, um, do we want to, like, go one storage file to another?
Candidate (You):  So, um, so when the metadata service is writing, there can be, like, multiple storage files, obviously. Like, we don't want one then because then it restricts how many concurrent writes we can do.
Candidate (You):  So there are many storage files which are available to write across many storage services. So it has, uh, this list, like, a big list of, like, uh, available storage files, right?
Candidate (You):  And it also knows where they are, so which node they are on. And, um, node and how much capacity they have left.
Candidate (You):  So, uh, capacity left. So maybe it has so we talked about, like, this being let's say the storage file itself are some terabytes.
Candidate (You):  So it might fill up. And based on that capacity left, it will make the decision, okay, how do I want to store this data that is coming for this file?
Candidate (You):  So if everything fits in one, it can do that. Or maybe it creates two or three, whatever number of entries.
Candidate (You):  So the entries that will be created, um, so it will refer this data. And then it will write to the DB that, okay, where I am going to store, um, store the file in which storage file.
Candidate (You):  So this is the mapping it will update for logical path. There will be many entries created.
Candidate (You):  Okay. So then.
Interviewer:  Sorry. How would how would metadata service oh, quick question. How would metadata service know which storage service to go to for this file?
Candidate (You):  Um, so metadata service, so all the storage services, um, I mean, this information is also available in the database that, um, like, which storage file has how much capacity left and on which storage node it resides on.
Candidate (You):  So.
Interviewer:  Okay.
Candidate (You):  The storage service, like, whenever it is writing, it is updating this information once it writes the information, uh, for the request.
Interviewer:  Okay.
Candidate (You):  So yeah. So the storage services all are writing into.
Interviewer:  Okay. So there is some place to okay.
Candidate (You):  Yeah. Yeah. So, um, so let's actually create another layer.
Candidate (You):  So I have my so let's say so metadata service is not responsible for the writes. Um, so I have my uploader.
Candidate (You):  Or maybe let's say this is, hmm, okay.
Candidate (You):  If let's say if this is my client. Let's start with the client.
Candidate (You):  And we'll see if I need to introduce more layers. But the client comes in.
Candidate (You):  It made this request. Uh, in our SDK, maybe, like, when you make a put request, the request comes into meta service.
Candidate (You):  Meta service returns you, um, some identifier. It will not return you, like, internal detail, but you will get, um.
Candidate (You):  So this is, like, you are sending in, um, this information. And what you are getting back is, um, okay.
Candidate (You):  How do I scroll to the left? Okay.
Candidate (You):  Okay. So what you are getting back from the metadata service is, um, maybe some kind of identifier which is opaque.
Candidate (You):  So, like, write handle.
Candidate (You):  So.
Interviewer:  Is, is this the rit-riturn code? Uh.
Candidate (You):  Um, so first, like, the client resolved the put request into a response. It has not it just sends first metadata that I want to write this file. And this is the size of the file that I'm going to write to.
Candidate (You):  Uh, it has not started sending the data. So first is just, like, pure metadata operation that happened.
Candidate (You):  The system is getting ready to write it. So it returns a bunch of write handles, um, to the client which are, like, yeah, like, some opaque handles.
Candidate (You):  They don't know, but they will resolve into something that our, uh, uploader can use.
Candidate (You):  Okay. So on this request, it got these write handles which encode basically this information that we talked about.
Candidate (You):  Okay. This logical file, I need to split into these many writes which are, like, storage file ID, offset, and length, um, where it need to be written.
Candidate (You):  And I think it also needs, um, which portion of the logical file to read to, like, it maps to. Um, so maybe we can extend this.
Candidate (You):  So this is my source offset and target offset.
Candidate (You):  Right. So source offset is purely metadata.
Candidate (You):  So when the client gets that information, it's basically going to then use this. It can do it concurrently, um, for all the write handles.
Candidate (You):  And then uploader, um, knows from that write handle where these requests need to go to. So then it will send those requests to our various storage service, maybe in parallel, to write to it.
Candidate (You):  And, um, these files will start appending into.
Candidate (You):  Now, okay.
Candidate (You):  So let's consider, like, what will happen if multiple writes are happening in the system, like, how these storage files are going to.
Interviewer:  I.
Candidate (You):  Yeah.
Interviewer:  Yeah, yeah. Sorry. Before that, can we do read path?
Candidate (You):  Yeah.
Interviewer:  Let's say DB try to read the same path. How would it work?
Candidate (You):  Yeah. Okay.
Candidate (You):  So my, um, client okay.
Candidate (You):  So now my client is again going to do a get.
Candidate (You):  So let's see. Um, okay.
Candidate (You):  It is saying, okay, let me read a particular file. So I'll just say file path.
Candidate (You):  We'll layer on directory later. And then the offset and how much I want to read.
Candidate (You):  So again, the metadata service is handling it. So hi, the metadata service.
Candidate (You):  Handle it.
Candidate (You):  Okay. So metadata service, again, it has, um, information to the DB where we wrote the mapping from logical file path to which storage nodes the data resides in.
Candidate (You):  It has to do this calculation. So that's why I said, like, database is storing it in a merried normalized form.
Candidate (You):  But maybe, like, we have, um, on the metadata service, maybe cached in the memory a better data structure which can do this translation for a file offset into file offset plus length into storage file, storage offset, and length. Uh, so that's the translation that need to happen for, uh, constructing it.
Interviewer:  Sorry. So if you mm-hmm. Okay.
Interviewer:  Sorry. Few basic questions here.
Candidate (You):  Mm-hmm.
Interviewer:  So this is the read path.
Candidate (You):  Mm-hmm.
Interviewer:  Uh, so metadata service, uh, I, I guess when you have storage, uh, identifiers, source offset, and length. So I yeah. I guess the first question is how would metadata service know which machine to go to?
Interviewer:  Because we said, like, this is a large-scale system. You can have multiple machines.
Candidate (You):  Mm-hmm.
Interviewer:  And, uh, the file could be spread across those files.
Candidate (You):  So that is the second this, this mapping that we also maintain. Like, where each storage file identifier lives. So that is the mapping.
Interviewer:  Okay.
Candidate (You):  From storage file identifier to the storage node ID.
Candidate (You):  So it is using.
Interviewer:  Okay.
Candidate (You):  Both these information to construct these write handles that is sent. So that's how the uploader knows, like, which node and which file it has to write to.
Interviewer:  Okay.
Candidate (You):  And similarly, the same, uh, translation will happen here where, like, the metadata service will return me, some, like, read handles, uh, which does that translation for the client to resolve the paths.
Interviewer:  Okay. Uh, so in this read path, I guess what you're saying is, uh, metadata service looks up the storage node ID.
Candidate (You):  Yeah.
Interviewer:  From the DB. And then uses the logical file path and the storage node ID to get the data.
Candidate (You):  Yeah. So first it looks up, like, okay, I have this logical file path to these storage file IDs. Then I have this offset to length.
Candidate (You):  So it pruned down this to, okay, I need to read from this offset to this length in a particular storage file. And then it used this mapping to attach, okay, which node it actually lives on.
Candidate (You):  And that's construct our read handles.
Candidate (You):  Right. So once you get the read handles back, uh, then our client or whatever SDK we have, uh, maybe HTTP request, multiple S3 request.
Candidate (You):  Then the second directory request will go to our reader. Um, and this is happening, um, through a load balancer.
Candidate (You):  So the client is just making, like, HTTP request. And behind the scene, we are routing to reader or metadata service or uploader, what-whatever.
Candidate (You):  Um, so now so now reader gets the read handles. This can also happen, um, in parallel.
Candidate (You):  Uh, so our client SDK can stitch the responses back to return the full file to the user. Uh, so this can happen in parallel.
Candidate (You):  So reader is getting, say, one read handle to work with. And, um, using that information, it is going to the, uh, storage services to read that particular offset.
Candidate (You):  So now okay. So I think reads and writes.
Candidate (You):  Okay. So we talked about the writes happening.
Candidate (You):  O-one thing okay. So this does this help understand, okay, the full cycle of writes and how the reads work?
Candidate (You):  Um.
Interviewer:  I yeah. I guess I have I have, like, uh, some reliability questions here.
Candidate (You):  Okay. Uh-huh.
Interviewer:  So, uh, are we are we storing when you say node ID, what are we storing here? And what happens if that node is, uh, uh, unavailable?
Candidate (You):  Yeah. Yeah. Yeah.
Interviewer:  Uh, after.
Candidate (You):  Yeah. So yeah. I think, uh, we cannot just store our data in a single storage file, right?
Candidate (You):  We want to, um, store multiple copies of this, uh, slice of data that we figured out. So when we have, um yeah.
Candidate (You):  So I think this mapping cannot be just one-to-one. So a logical file, like, whatever segments it has, it need to go into multiple of these places.
Candidate (You):  So let's say we store this across, um again, I mean, that, uh, will be dependent upon, like, how much hmm, I mean, there are two factors.
Candidate (You):  There is a durability factor. And then there is the availability factor, right?
Candidate (You):  So to ensure high availability, we need to let's say we are talking about one region. So if we say we have three copies of each segment of the file that we are storing, then we can handle, let's say, two, like, one data center going down.
Candidate (You):  So we're talking about, like, availability zones for AWS. Uh, I create three copies.
Candidate (You):  I spread it across three availability zones. So one data center going down, I can still read my file.
Candidate (You):  If, let's say, one of the storage services node goes down, um, yeah.
Candidate (You):  Then so within a data center, I think hmm, yeah.
Candidate (You):  Within the data center, I don't think I need to create two copies. So data centers have storage service instances.
Candidate (You):  And so what I'm creating, I'm just spreading across three availability zones. And one of the storage service will have the data.
Candidate (You):  Um, so even, like, if one data center goes down and, let's say, in another data center, one of the storage service goes down, I have one final one remaining, uh, which I can go and read to. Now, this mapping, um okay.
Candidate (You):  So we have for each file path okay. So let's say we actually do.
Interviewer:  Yeah.
Candidate (You):  Three of three of these mapping. Yeah.
Interviewer:  Yeah. Quick question, [candidate]. I, I guess the replication part, I can understand.
Candidate (You):  Yeah.
Interviewer:  Uh, one more thing. I guess as you are designing the solution is, uh, I if I understand correctly, your file maps to one node, right?
Candidate (You):  Yeah.
Interviewer:  Or, I guess, a bunch of replicated nodes.
Candidate (You):  Yeah.
Interviewer:  Uh, but we said that a file could be very large, more than terabytes.
Candidate (You):  Mm-hmm. Mm-hmm.
Interviewer:  So how would, uh, uh assuming, like, we can't fit in a single node, how would you handle that?
Candidate (You):  So, um, we already splitted our large file into multiple of these, um so we okay. So let's talk about, like, uh, my file. So I'll pick a long thing.
Candidate (You):  So this is my logical file.
Candidate (You):  Um, right. So we talked about, okay, we are going to split our logical file into segments, 4KB segments.
Interviewer:  Okay.
Candidate (You):  Right. So this is my 4KB segment. Now, um, this is now getting mapped to my storage file.
Candidate (You):  So I have this storage file.
Candidate (You):  And storage file also has these segments.
Candidate (You):  Right. Let's say they are same size.
Candidate (You):  So now.
Interviewer:  Okay.
Candidate (You):  What is happening is.
Interviewer:  So yeah. Not I think not, uh.
Candidate (You):  Yeah. So it is getting replicated across.
Interviewer:  Yeah. I think it makes.
Candidate (You):  Three storage files.
Interviewer:  Yeah.
Candidate (You):  Yeah. Okay. Cool.
Candidate (You):  So yeah.
Interviewer:  And just to, like, just to sorry. I'm just, uh, moving fast just to make sure that, uh.
Candidate (You):  Yeah. Yeah.
Interviewer:  Okay. Cover the other parts.
Candidate (You):  Yeah.
Interviewer:  So I, I guess just to understand, uh, what you're saying is, uh, we are splitting the file into logical files with 4KB parts.
Candidate (You):  Mm-hmm.
Interviewer:  And the storage file identifier here which you're referring to is the 4KB identifier for the 4KB part. So that is stored in this node ID.
Candidate (You):  Um.
Interviewer:  Is that right?
Candidate (You):  No. So the storage file ID is the ID of this file. Maybe each file has its own ID.
Candidate (You):  But I'll, I'll, I'll explain, like, how the split is happening. So let's say this is my storage file ID two.
Candidate (You):  Um, storage file ID one. Okay.
Candidate (You):  So I think I, I mean, what you hmm, yeah. I think that it should also work.
Candidate (You):  Like, we can do either. We can map from file to node, or we can map from segment to node.
Candidate (You):  I will keep file to node because that will give me less metadata to worry about. Um.
Interviewer:  Okay.
Candidate (You):  And the way the segments are created is based on the offset. So let's say I store, uh, two segments from the logical file into one storage file together. So that creates one metadata entry for me.
Candidate (You):  So I don't have to, like, create so I'm not restricting my design to have one metadata entry for each 4KB slice. I can use offset and length.
Candidate (You):  So maybe I have enough space in a storage file to store 100 segments. So I will create just one metadata entry that all these 100 segments are stored in storage file two, storage file one, and storage file three.
Interviewer:  Okay. Okay. This, this makes sense.
Interviewer:  So I guess if there is a terabyte of file, then you would have multiple entries in this logical.
Candidate (You):  Yes.
Interviewer:  File path.
Candidate (You):  Yes.
Interviewer:  And, uh, each, uh, storage file would point to its own node ID. So I guess now going back to the put file.
Candidate (You):  Yeah.
Interviewer:  Uh, how, uh yeah. I'm still not, uh, clear on this part. So how would let's say let's assume your file system is pretty close to full.
Candidate (You):  Mm-hmm.
Interviewer:  How would you know, uh, which node ID to pick up? Like, who is updating this availability part? Like, who is deciding which node ID is.
Candidate (You):  Yeah.
Interviewer:  Available?
Candidate (You):  Yeah. Yeah. So that is the storage service.
Candidate (You):  So when the write is happening, right so let's say storage service has, um, eight storage, uh, storage files available on which it is writing.
Candidate (You):  So it can receive, like, eight write requests. And it can write to all these storage files.
Candidate (You):  So once the storage files get full or whenever it is writing it, it first of all will update the capacity left. So suppose it gets a request, okay, I want to write this many number of bytes in my storage file one.
Candidate (You):  Uh, once I write it, I will update my DB that, okay, the capacity left. Suppose the capacity left is zero.
Candidate (You):  Um, but once the storage file is full, let's say it is only working on based on, like, we figured out, okay, eight file size what we want to support on storage service that we can write to. If one of the files gets full, it will open or create a new file on one of the disks which still has space.
Candidate (You):  So that mapping is completely on storage service, like, where, uh, this storage file ID. So storage file ID is kind of a logical identifier.
Candidate (You):  Then the storage service has its own DB where it maintains of its storage file ID, which physical disk and, um, offset it lives on. So then it will create a new storage file ID and create a new mapping here.
Candidate (You):  Okay. The previous one is full.
Candidate (You):  The new one is this one which has full capacity. So at any point, metadata service knows, okay, like, which file has what capacity.
Candidate (You):  And there is some kind of maybe reservation that is happening, okay, that, hey, I'm going to upload it. So I'll update this DB to say, okay, I'm reserving it.
Candidate (You):  So storage service, no, like, only with this particular identifier if the request comes in, I will write. Otherwise, I will ignore it, uh, so that we can, uh, handle the race conditions.
Candidate (You):  Like, maybe metadata service reserved it, but it was not able to follow up. So then that reservation will die.
Candidate (You):  Uh, so that way, like, this mapping, uh, each storage service updates. If it fills, then it creates new entries in there so that yeah.
Candidate (You):  Like, the new, um so if this gets full.
Interviewer:  So.
Candidate (You):  Like, I think yeah.
Interviewer:  I yeah. I guess how would that work if it's not the same storage file? I mean, I can understand if it's the same file creating multiple storage files.
Interviewer:  But let's say you just have multiple writers.
Candidate (You):  Yeah.
Interviewer:  Uh, assume for all practical cases, one-to-one mapping between the logical and the physical file.
Candidate (You):  Mm-hmm.
Interviewer:  Uh, how would this work then? You, you yeah. I guess some of these nodes are full.
Interviewer:  There is no capacity.
Candidate (You):  Mm-hmm.
Interviewer:  So how would this work?
Candidate (You):  Um, so let's say, like, my storage service, right, this one, it has.
Interviewer:  Yeah.
Candidate (You):  It can open many storage files. So at a time, I'm saying, like, it has eight storage files open.
Interviewer:  Okay.
Candidate (You):  Right. So storage node one, it is currently working on storage file one, storage file two, dot, dot, dot, storage file eight. And each has some capacity left.
Candidate (You):  Let's say it has 100 blocks left that can be written. And it is all, like, getting appended one by one.
Candidate (You):  So it is not doing random seeks over it. And it has this.
Candidate (You):  So so this is the DB mapping that I mentioned. So let's say you are saying, like, let's say all of them got filled up.
Candidate (You):  So multiple writes are happening. These all gets filled up.
Candidate (You):  Um, so now storage node one has no file that it can write to. So there are two cases it can happen.
Candidate (You):  Let's assume, like, it is it doesn't even have any additional disk space left that can create new storage files. But we are saying, okay, suppose we have more space left.
Candidate (You):  So then it will create new files with full capacity and publish that to the DB. So other writers will see that, okay, I can still use it, reserve it, and write to storage node one.
Candidate (You):  Um, if our storage node completely is getting fold up, then we are spinning up new storage nodes, or maybe we are attaching new disks to the storage service so that the capacity becomes available. So that is, like, our scaling factor.
Candidate (You):  Like, how do we either attach more disks to the storage service, or we spin up new storage service with new disks getting attached to it, uh, and those get published, um, like, whenever our storage service comes in, sees it has it need to maintain, let's say, certain capacity, uh, on it. So it, it won't wait for all the storage files to go down to zero.
Candidate (You):  Maybe, like, when they are, like, 50 full, it will create new files and push it, uh, on the DB.
Interviewer:  Okay. And are the storage service running on the same node, or, uh, are they running on a different node as compared to, like, the file system?
Candidate (You):  Uh, storage service are nodes by itself. They are.
Interviewer:  Okay.
Candidate (You):  Yeah. They, they are the file system. They are they're running the file system on it.
Interviewer:  Okay. So it's just a process inside the node.
Candidate (You):  Yes.
Interviewer:  One of the process is called storage service.
Candidate (You):  Yes. Yeah. So this node.
Interviewer:  Uh.
Candidate (You):  Is a Linux container. So it has all the file system information. And it's just a process which is using that file system.
Candidate (You):  Metadata service is a different service. Um, and then they are talking through the DB across each other.
Interviewer:  Okay. And, and yeah. Sorry.
Interviewer:  One last question, I guess, before we run out of time. So what happens in case of crashes?
Interviewer:  Let's say your metadata service write happened. This whole thing is written.
Candidate (You):  Mm-hmm.
Interviewer:  And then uploader is trying to upload the file. For some reason, the application crashes here. And it never pauses to upload.
Interviewer:  So what happens to the metadata now?
Candidate (You):  Hmm. Yeah. Yeah.
Candidate (You):  Um, yeah. There need to be, um, some kind of acknowledgment that need to happen.
Candidate (You):  So, um, when the metadata service created this record, it created with, with the hope, okay, that the thing will succeed. So there is some marker, okay, that it will it will wait for some acknowledgment.
Candidate (You):  So either yeah. We don't want yeah.
Candidate (You):  So I think the uploader, like, uploader can acknowledge when it has written successfully that, okay, this particular chunk got written to, and it gets updated in the DB. If that doesn't happen, then we need, like, another pruner or something which can clean these orphaned, uh, nodes from the from the storage files and return them back to the available capacity that we have.
Candidate (You):  But yeah. That could be interesting.
Candidate (You):  Like, we might leave, like, holes in the storage file. So our storage file is filling up.
Candidate (You):  Let's say we filled up some chunk. That failed, but the later one succeeded.
Candidate (You):  Then we have a hole in our storage file. So then we have to figure out, okay, so our, like, pruner is actually maybe, like, a storage health system.
Candidate (You):  So it can, uh, more like a disk fragmentation. Like, I'm getting, like, old ideas of, like, disk fragment but something like this, like, okay, it copies the stuff into a new storage file, remove the holes.
Candidate (You):  So then that capacity becomes available.
Interviewer:  Okay. Uh, awesome. Yeah.
Interviewer:  I think we are out of, uh, almost.
Candidate (You):  Okay.
Interviewer:  Our time now. Um, uh, yeah. Do you have any questions for me?
Candidate (You):  Um, yeah. I think I want to learn a little bit more, like, what the work mean for you, like, when you talked about, like, optimizing it across different clouds. So my understanding is that Databricks runs on Azure and, uh, AWS.
Candidate (You):  That's the two I know of. Um, so is, is it that, like, how to run it there?
Candidate (You):  Or, like, even within AWS, how to optimize, uh, the needs of, uh, the service?
Interviewer:  Yeah. Yeah. So Databricks is, uh, is a complex product by itself.
Interviewer:  There is, like, Spark. There are a bunch of, like, workflow management and, uh, bin packing, scheduling.
Interviewer:  So it's not a simple SaaS that you can just run it on cloud.
Candidate (You):  Mm-hmm.
Interviewer:  So this entire Databricks complex package needs to run in different clouds.
Candidate (You):  Mm-hmm.
Interviewer:  And, uh, because we don't have our own cloud, we get billed from these cloud providers.
Candidate (You):  Yeah.
Interviewer:  So a-assuming a simple example of, like, $100 bill for running a Spark cluster.
Candidate (You):  Mm-hmm.
Interviewer:  So I guess our team tries to see if we can make it $90 by keeping the same.
Candidate (You):  Okay.
Interviewer:  Performance and everything.
Candidate (You):  Okay.
Interviewer:  Uh, probably improve the bin packing or, uh, figure out, like, uh, clever ways of, uh, setting up a warm pool or cold pool so we can.
Candidate (You):  Mm-hmm.
Interviewer:  Sort of, like, reduce the cost.
Candidate (You):  Mm-hmm.
Interviewer:  Uh, so yeah. This is what we focus on. We try to see, like, attribution, like, what is the exact cost for each of these products, and then efficiency, how do we improve the cost, uh, because we don't run on our, our own cluster.
Interviewer:  We run on, like, all these cloud providers. So we get a bill.
Candidate (You):  Yeah. Yeah.
Interviewer:  Uh, so if we can.
Candidate (You):  Yeah.
Interviewer:  Make a Spark cluster more efficient, then we can translate the savings to the users as well as the product.
Candidate (You):  Got it. Got it. So it is not more about, like, where should I run this.
Candidate (You):  Like, even when it is running, like, how we can improve how it is running.
Interviewer:  Yeah. Improve, uh, running not in terms of latency but in terms of cost, uh, but.
Candidate (You):  Okay.
Interviewer:  Without having a product to the, uh yeah.
Candidate (You):  So how does that interaction looks like? Because when you are trying to improve, say, cost for the Spark jobs, um, it also depends on how the Spark job, uh, is configured or, like, how maybe say the scheduler is using, uh, resources, spinning up resources. So I'm assuming, like, that is a different team.
Candidate (You):  So what does that.
Interviewer:  Yeah.
Candidate (You):  Interface looks like between you and that team?
Interviewer:  Yeah. So we work with we are more like a platform team. We work with every team.
Interviewer:  So some of these.
Candidate (You):  Mm-hmm.
Interviewer:  Low-level, Spark-level efficiency, uh, we can probably track it to say, hey, your cost was $100. Now it's $110. Something regressed.
Candidate (You):  Mm-hmm.
Interviewer:  Can you look into it? And can you find, uh, optimizations there?
Candidate (You):  Uh-huh.
Interviewer:  Uh, but we can track at, like, the entire Databricks level on how the cost is trending.
Candidate (You):  Mm-hmm.
Interviewer:  And, uh, yeah. I guess, like, there are things that we can do at a platform level. It doesn't have to be at the Spark level.
Candidate (You):  Oh, okay. Okay.
Interviewer:  Uh, choosing the right region, choosing the right size. So that we try to focus on. Uh, but yeah.
Candidate (You):  Can you give me an example of, like, what, what that look like, like, how at a platform generic level you can save the cost?
Interviewer:  Yeah. There are there is, like, tiered tiered storage in, uh, AWS, for example.
Candidate (You):  Mm-hmm.
Interviewer:  You have, like, different costs for different storage. So a simple, uh, tiering enablement for different TTL could save good amount of cost.
Candidate (You):  Mm-hmm.
Interviewer:  Uh, similarly, every cloud provider has, like, different policies. So we could, uh, optimize at the platform level. Uh.
Candidate (You):  Mm-hmm.
Interviewer:  Yeah. So I, I, I guess it's like a delicate balance. Like, we try to go deep.
Candidate (You):  Mm-hmm.
Interviewer:  As much as possible to understand the product. GenAI products, we try to go deep. But if it's.
Candidate (You):  Mm-hmm.
Interviewer:  Like, a very well-known product with a bunch of folks already working on it, then we try to be at the surface level, platform level.
Candidate (You):  Okay. Cool. All right.
Candidate (You):  Thanks. Thanks, [interviewer].
Interviewer:  Yeah. Thank you. Have a good one.
Interviewer:  Bye.
Candidate (You):  All right. Bye.

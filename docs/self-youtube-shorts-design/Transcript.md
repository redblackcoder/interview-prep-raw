You: Okay. In.
You:  This self-interview, I'm going to do assistant design for YouTube Shorts, so from the requirement point of view, our YouTube Short is basically a content creator, a user using the app to record a short video, say a 30-second video, and then they upload that video to the YouTube once.
You:  The video gets uploaded, it is available across the network for other viewers to see it. Generally, there is a algorithm which shows you— which shows the viewers a feed of, like, maybe like an infinite feed of these short videos, that you scroll through.
You:  So there is a— there is a very big and complex algorithm, I'm assuming, which shows these feeds to the user, and once your video is uploaded, it will start showing up in, like, various people's feed. There's also, like, concept of following, so then you have, like, a more curated feed.
You:  So if you are following certain creators, then you get a— maybe, like, there are, like, 2 feeds: one is, like, the algor-commended feed, and another is, like, feed which is only comprised of people that you are following. So— so from a requirement perspective, like, functional requirement perspective, I think we nailed down on uploading a short video having that short video available in user's feed, there could.
You:  Be, like, some— some delay, I think, I don't think it need to be instantaneous when the— once the short videos uploaded, it gets— let's say— processed in up to, like, 5.
You:  Minutes? In 5 minutes, on.
You:  The system and being available to the algorithm to give it to user's feeds. Viewing the feed, so when you come and view a feed, you get a list of videos that you scroll through, and as you keep scrolling, that list basically keeps— keeps giving more and more to you.
You:  And then there's, like, auxiliary feature of, like, following someone and following someone, which, yeah, which is, like, a simple—
You:  Simple mapping, like, who are the people that I'm following, who are the people who are following me. So with these, like, functional requirements, the— let me— let me write it down.
You:  So functional requirement: upload a short video— say less than 30 seconds—
You:  Uploaded video of available in 5 minute.
You:  For others—
You:  Feed.
You:  And maybe this is, like, our P905 number, or 90 number, something.
You:  Along those lines.
You:  And then a user views their feed; it is a infinite scrolling video feed.
You:  Then we have a user follows or unfollows—
You:  Other users.
You:  Okay.
You:  I messed up.
You:  Okay.
You:  Then non-functional—
You:  We can say there are—
You:  Let's say 100.
You:  Million active users daily.
You:  With maybe 1 billion total users.
You:  On average, say 1 million new short videos.
You:  Uploaded.
You:  Daily.
You:  And let's say.
You:  The number of videos watched is—
You:  Let's say 1 billion.
You:  Daily.
You:  1 billion.
You:  Daily. Video watched.
You:  Yeah.
You:  If you have, like, 100 million active users and we talk about, like, on average 10, that each watches, then— yeah, it's very easy to hit up 1 billion daily. Video watch.
You:  Obviously, this pattern will be spiky, maybe in the evening, like right before the night, like how—
You:  My understanding of the product is, like, I use these products, so there.
You:  Will be spiky patterns. So if we look at, like, 1 billion daily videos, watched, which is, like, 10 to the power 9 per day, it will equate to.
You:  10 to the power 4 per second, because we have 100.
You:  000 seconds in a day, approximately, and 100,000 is 10 to the power 5.
You:  Okay, and then we are saying peak time— so a lot of these views will be concentrated around peak times, so let's say, like,
You:  Yeah, like, I mean, we can do very complicated maths, but just as for just estimating, so I will say peak is—
You:  Peak is maybe, like, 100 times of that.
You:  Per second.
You:  All right. Then the other standard stuff, like availability, high availability,
You:  Durable videos, so once you uploaded, it is durable.
You:  Low latency for viewing.
You:  Yeah. So scrolling through the feed, like, the video starts playing pretty much instantaneously, so let's say, like, starts streaming—
You:  In less than 100 millisecond, which pretty much means that we have to push it to their device so we— yeah, I'm getting a little bit into design, but yeah, we have to keep the videos refreshed into— into the client so that they can scroll through it and start playing.
You:  So on the high-level design, if we jump into that, skipping the API, I think the API part is pretty simple. There's, like, a upload with a payload, which is the video.
You:  Like, encoded video, encoded videos.
You:  Stream.
You:  Encoded video, and.
You:  It would have some additional metadata about, like, user location, some.
You:  Text description, some tags,.
You:  And on the.
You:  Viewing side, it is feed—
You:  And.
You:  The feed API could be, like, how many you want, so the count.
You:  Parameter, and then some kind of a cursor, so we can—
You:  Scroll through it. So we have, like, no cursor at the beginning, and we ask for 10, we get a cursor back, and then we pass in that cursor to keep getting more and more and more.
You:  And this feed obviously can also have what kind of feed it is, as you were talking about too, when it's, like, algo-based, or it is follow.
You:  Based.
You:  Algo or followers.
You:  Following.
You:  The people that I'm following.
You:  So those are the two main APIs. Now, when we talk about, like, high-level diagram, we will start with something simple, we have—
You:  A user using, like, some device.
You:  Right? So this is the client.
You:  User.
You:  They record a video, and then they send that video over.
You:  To be stored in our system.
You:  So this is the upload.
You:  Right?
You:  So the uploader service—
You:  Where the request comes in.
You:  To upload the video. Obviously, there will be, like, network load balancer, a gateway to route the request to a fleet of uploaders, so that one of them will handle it.
You:  Now, the interesting bit is—
You:  We have this client, and client will have a video, so there are two options, that I'm thinking of, like, okay, so let's talk about, like, maybe the storage because that decides, like, what options we have.
You:  So assuming we have an abstraction for the storage, so then we have our storage.
You:  Service, which is abstracting how we are storing this.
You:  These videos.
You:  So the storage service is something which.
You:  Will— we can have the option that I'm thinking, like, we can have, like, our own. Fleet of storage, so it depends on, like,.
You:  How big the engineering effort is, like, should we just use S3, should we use something internal, maybe if the company is big enough, they have a blob storage of their own, or if the team is just getting started, maybe they just use disks to store it.
You:  But given, like, yeah, I won't recommend disks, like, currently blob storage are so cheap, so either— either go with, like, a cloud S3 for storing— storage needs, or if your company has a blob storage that you can use, use that.
You:  So let's assume, like, we just use AWS S3.
You:  Or equivalent.
You:  Company equivalent.
You:  Blob store.
You:  So this is where we are storing our videos.
You:  Raw videos.
You:  And storage service provides the abstraction for us to not worry about it.
You:  Now, the thing that.
You:  We have to consider— like, this client, when they're uploading this video, should we stream the video through our services before it lands into the blob store, or do we use, like, a direct upload to blob store, so we don't use up our bandwidth, like, our network capacity to do that?
You:  So those are, like, I'll write down the two options. So we have one option, upload.
You:  Directly to blob store from.
You:  The client.
You:  Or two,.
You:  Upload to.
You:  Internal service first, and.
You:  Then to blob store.
You:  So in the first option, we have the pros of, yeah, we save on network bandwidth, if we are using, like, AWS, then obviously a lot of.
You:  Network.
You:  Usage, causes bills, so we save on that, and we utilize, like, S3's ingress only versus, like, our service's ingress.
You:  When we are doing—
You:  When we are doing upload through the services, then we also have to think about our own internal storage.
You:  Like, temporary one, where we are going to do it.
You:  So we have that thing. The other thing, however, like, on the pro for uploading through the internal service, is that we don't have to make our clients complicated.
You:  Like, they don't need to worry about working with the blob storage, we just said, like, our storage provides the unified interface that we can use and not worry about, like, behind-the-scene, what is in there. But if we have to do a direct upload to the blob storage, then our client is tied to that implementation.
You:  For example, like, if we have, like, signed URLs, for AWS S3 bucket that we are providing to the client, for uploading, then client is tied to that. If in future we decide to move to a different cloud provider or roll our own, then the client has to change.
You:  And the migration will be much harder than just doing it internally through us.
You:  The other is, like, I'm thinking specifically for, like, video upload, generally a video gets—
You:  Encoded in multiple.
You:  Resolutions, so when you— when the user is recording, maybe they record it in, like, 4K, and then it's get. Uploaded, but we want to store in, like, various resolutions, maybe we want to store it in 1080p, we also want to store it in, like, 720p, or even maybe 4— is it 4?
You:  420? Or something around that.
You:  So we have— and we do that because when the users are consuming it, we want to provide them a buffering-free and fast video playback.
You:  And in some cases, also based in— based on your device, maybe your device screen resolution is not so high, so why stream a 4K to you? Or why stream a 1080p to you?
You:  Maybe 720 is good enough, and we save on, like, serving cost, which is much higher as we notice, like, it is 1 billion daily we use watched versus 1 million daily videos getting uploaded, so 1,000x difference.
You:  So those are the pros and cons, and based on, like, how— how the product looked like, we have to make a choice.
You:  Thinking long-term, and thinking about these, like, encodings, which are needed— I would go with routing through an internal service to do that. So the first hop will happen in the uploader, there.
You:  Is—
You:  So again, like, the storage service abstraction, I would like to use, so the uploader can use.
You:  The storage service to store it locally.
You:  And this thing—
You:  Yeah, so let's say we have a local fleet of.
You:  Storage—
You:  That is a storage service handles, maybe, like, disks,.
You:  So—
You:  It's going to temporarily write to it.
You:  And we have a encoder service so the encoder service is responsible for.
You:  Is responsible for, like, producing.
You:  The various encodings.
You:  The service, so let's see, we have storage here, we have our.
You:  Storage nodes, okay.
You:  The storage also has abstraction for going to the cloud storage, but it hasn't happened yet, so let's see if I number them.
You:  So we have first upload,.
You:  Then we have the storage service,.
You:  Then we have the upload here, and— and the storage service need to send a message to the encoder that, hey, this particular video has been uploaded, please do your work, so it will put a message into a message queue, which the encoders are. Listening to, and they will pull it back.
You:  So here we have—
You:  Our event queue.
You:  So let's.
You:  Go here.
You:  An encoder is basically then— again, calling back.
You:  Into storage service to write those encodings back.
You:  And while this is all happening, there is a database— obviously— which is used to capture the metadata about these videos. So we have our DB metadata, DB—
You:  Which is getting used by these services to.
You:  Update the state.
You:  So once upload, it writes maybe, like, a storage service provider ID, like, where it is, other metadata about the.
You:  Video, then the storage service—
You:  Event goes in here, the encoder, again, the encoder is using this information to fetch the video from the storage service or maybe— yeah, fetch it from the storage service, and.
You:  Once the encoding is done, it writes more metadata, like, here are the additional encodings, and their storage ID, and gets saved here.
You:  Once all this is done,.
You:  When all this is done, then we can— and queue— or, I mean, we don't have to wait for everything to be done, like, as soon as the stuff lands in storage. Service, we can—
You:  We can start the process of, like, uploading them to S3. So maybe the storage service asynchronously, once something lands in here,.
You:  Does a upload, maybe, which is, like, an internal— internal asynchronous processing that's happening inside the storage service, so I'll do, like, okay, the uploader— or I'll say.
You:  Blob uploader.
You:  Which triggers.
You:  Asynchronously as soon as something is returned to the storage service, and it gets uploaded to Amazon S3.
You:  And then there is a cleanup job, which cleans up old stuff from here.
You:  Cleanup.
You:  It will ensure that, yeah, like, the data is first on S3, obviously, that the metadata is there, validates it's on S3, maybe the size and stuff, so yeah, we accidentally don't delete something which is in process. Also, like, TTL-based or anything, after maybe a day or two, that we'll do.
You:  Okay, so this is the upload part.
You:  Before going into. Like, some of the scaling discussions, let me first think of what serving this looks like.
You:  So again, we have our another—
You:  User, who's trying to consume.
You:  These feeds.
You:  So we have the feed service.
You:  Which this client is going to call when they want to.
You:  Yeah, when they— when they want to see the— see their feed, and the feed service—
You:  Along with.
You:  So this is our—
You:  Social service.
You:  Social graph.
You:  Maybe this is our social graph.
You:  Let's see.
You:  So this is our social graph DB.
You:  Which holds the connection, and.
You:  We have—
You:  The.
You:  Follower service.
You:  Okay, so we have the social graph, so this is basically when users are following each other and they communicate with this service.
You:  To—
You:  Register that, okay, who I'm following, who I'm not unfollowing, and this internally keeps the social graph up to date.
You:  Which is our, like, data store.
You:  For the following service, for the followers service.
You:  Now, to the feed service, we talked about, like, two— two modes. One is algorithm-based, another is follower service-based.
You:  And.
You:  Yeah, and I'm thinking, like, this is— this is potentially something which— which.
You:  Will be good to abstract, so that the feed service has a single phone interface, like, how it need to fetch the feed,.
You:  And it should be easy to, like, just switch between algorithm-based or follower-based, so as we add, like, more features to the feed, both the paths get it instead of having two interfaces and implementing it in, like, doubling the cost of the implementation.
You:  So if I use that design principle, to build the feed, then one approach that we can take is have this feed getting materialized, through some background process, so what will the feed will look like. So feed for a user.
You:  So feed for a user will look like user ID, which is the key, and then we have—
You:  Post IDs, maybe we can say, like, these videos are postings, so it's an array of post IDs.
You:  So this is what the feed will look like, there could be different kind of feeds, obviously, so like, algo feed will look similar, like, the basic structure is the same.
You:  Okay,.
You:  So we have algo, feed, or follower feed.
You:  The structure is like this.
You:  Now,.
You:  We.
You:  Will go into the details of, like, how to generate this, but let's assume if we have a feed like this.
You:  Then what we are saying, like, our storage.
You:  For the feed service will basically provide this to us.
You:  And.
You:  Post ID.
You:  Are just, like, identifiers, so the algorithms and everything are just putting the identifiers into this, so this is small to store, but additional information, maybe from the metadata with the help of the metadata service,.
You:  Gets additional detail that need to be fetched to provide the feed to the user.
You:  And this is where, like, things like CDN can come into picture, because what the feed service can do is just provide the URL for the CDN endpoint for the videos, which are the most heavyweight.
You:  Which we haven't talked about here, but yeah, like, the CDN is used for, like, distributing the videos on the edge of the network, so all the traffic is not coming to our service and consuming all our bandwidth, so CDN lives globally distributed in multi-places and we handle the upload in one place, but it gets distributed with the help of the CDN, and the feed service is just putting the URL of the CDN, if that— if that content is not cached.
You:  In the CDN.
You:  Already, then the CDN will come to our service to get it, or if it is, like, S3-based, then the Amazon CDN can directly get it from the S3 and provide it.
You:  Bypassing any of our network.
You:  Okay, so that was a quick detour around CDN, but coming back to our feed, and this is our feed DB, which has— as I mentioned, like, let me.
You:  Put this here,.
You:  Yeah, so—
You:  Okay,.
You:  Cool.
You:  So the feed service is fetching feed from here,.
You:  To make the feed faster, we can also introduce cache, like, based on the latency, requirements, so if we put a cache in here, then this feed is basically getting cached—
You:  In memory instead of, like, getting read from the DB all the time.
You:  So let's talk about, like, how we will—
You:  One interesting thing is, like, how we will support the cursor.
You:  Like, first is how do we support things.
You:  Changing in the system, for example, I might be changing my followers, when.
You:  Is happening, how the cursor is still stable so that the videos doesn't, like, jump around for the users. So when the user makes a request.
You:  What we can do is—
You:  Have, like, some kind of.
You:  Snapshot ID for our feed, so these feeds can.
You:  Support, like, snapshotting, so if a user— so, like, a snapshot isolation kind of a thing— if the user is looking at a feed, you get a snapshot ID, which.
You:  Is—
You:  Which is respected both in the cache and in the DB, the feed service based on that snapshot ID always provides a stable cursor which will go over the results and return the results to the user.
You:  The snapshot ID expires, so the DB doesn't have to keep, like, many, many versions of these feeds, so for example, when you have a snapshot ID, the feed cannot update— or cannot be overwritten, so say a follower changed, and that information is pushed by the follower service to regenerate the feed, so that is the part which is the feed generator.
You:  Feed generator that we are going to go into next, so the feed generator is the one which is writing to the feed DB when things change, when something gets uploaded, a follower changes,.
You:  And maybe when we ship, like, different algorithms, we generate new versions of the feed, and the old versions of the feed get over time depleted, so—
You:  The feed generator is doing it, so any follower change, feed generator sees, a snapshot is in use, and it will not overwrite it, but instead will create a new version or, like, yeah, a new version of the feed that can be looked upon by the client.
You:  All right, let's.
You:  Talk about the feed generator itself.
You:  So for the feed generator, when the upload happens, then we are talking about a fanout, so I have a user A—
You:  Suppose user A is very popular,.
You:  So what the fanout is—
You:  So all— all the people who are following user A are basically—
You:  Getting their feed updated because user A just published something.
You:  And given how popular user A is, this can be a lot of users.
You:  Like, for a celebrity who have millions of users, then we are talking about updating feed for millions of users—
You:  Into our feed table.
You:  One way to— so we're talking about, like, this is scaling problem, and that's the problem that we will hit when we're talking about 1 million new shorts video uploaded daily, so 1 million read— if we have our feed— a client is connected, they are going through this feed, we scale it through our cache service, this cache service has many read replicas, so it can support.
You:  Like, say, one cluster can support hundreds of thousands of requests per second, and then we were saying our peak was 10 to the power 7, and we're talking about, like, 100 of thousands is 10 to the power 6, so we need say 10 read replicas, of the cache service, to handle the peak.
You:  So on the read side, I think that's— that's the solution that we can go with.
You:  On the write side, when we have an upload happening and the.
You:  Upload finishes, when the upload finishes and things get into S3, the feed generator will get an event to basically handle— handle that.
You:  Upload.
You:  That just happened.
You:  So—
You:  Yeah, maybe there's, like, yeah, an orchestrator, so uploader is not directly responsible for doing all this, there is an orchestrator involved.
You:  Orchestrator.
You:  Which is our guest.
You:  Which is, like, a durable execution engine, like temporal, which.
You:  Basically takes care of.
You:  Yeah, all the numbers will change, so let me do it this way.
You:  Okay, so 1.5, so the uploader is basically writing.
You:  Stuff here, and then submitting or starting a workflow on the orchestrator, which is doing this.
You:  Entire thing.
You:  Putting it on the storage, maybe it will— instead of now, another choice is, like, we don't even need this queue, the orchestrator can— once this upload finish, trigger the encoder to do this, so that's— that's the other part, once we have the orchestrator in place, which I— which I like better than— yeah, like, going directly.
You:  And once all the upload is finished, this orchestrator basically will call the feed generator to generate or insert this feed into our user's feed, so generator will use the follower service and.
You:  Maybe.
You:  Algorithm to— to generate the feed for the user, so we have the follower service, we also might have—
You:  Feed algo.
You:  Scorer.
You:  Service.
You:  Which can be used to also generate the other feed, so orchestrator, maybe first generates the follower, in parallel another activity, to generate the feed based on the algo, so using an scorer as a proxy for the feed, maybe the algo provides, like, a score for each video, for a user, so the input is, like, the video, and the user ID, and it will return a score, like, where it should go into the user's feed.
You:  And then it gets inserted into this list, and this list could be, like, a sorted set, and based on that score it goes there for the follower, the score is just the recency or it could be—
You:  It could be more complicated.
You:  Like, popularity of, like, how much engagement, but yeah, I think that's— that's the part of the algorithm, so I'll keep it simple, it's just, like, the recency, so for the follower feed, the sorted sets, score is based on the recency, for the algo based it is the score provided by the algo.
You:  So coming back to the.
You:  Fan out problem, so when this is happening and a user has millions of followers, so the orchestrator need to call this feed generator, maybe the feed generator.
You:  Yeah.
You:  The orchestrator, I think the better would be that.
You:  Orchestrator uses the follower service, I like that better, like, the orchestrator uses the follower service directly, sorry, it's getting a little messy, so.
You:  Okay.
You:  Let me go this way.
You:  So the orchestrator calls the follower service,.
You:  I'm trying to patch it, that is the whole idea, so instead of feed generator, or, let's see, let's see, I think, like, put it back there.
You:  So this diagram is getting a little complicated, maybe let me do it on the side.
You:  So the question is, okay, let's— let's first walk through what will happen in the feed generator.
You:  If we don't handle the huge fan out, so the orchestrator sends it to the feed generator, feed generator looks at.
You:  Follower service to figure out which users should get it in the follower feed, and then similarly for the.
You:  Algo feed,.
You:  Maybe the feed algo scorer, yeah, we can make the feed algo scorer also return which users should get this video, so that's also part of the algorithm, it could be based on user's interest, like, what users like to see, what they have interacted with previously, so certain kind of videos or creators, they will not map to everyone's feed, but will map to a subset of feed based on user's behavior and interest.
You:  So the algo service will also return a list of users, so that is the abstraction that we can create for the feed generator to work across both algo and follower, get this list of users whose feed should get this video, and the score of the video.
You:  So when the feed generator had that information, it, again, will use another workflow on the orchestrator to start putting into the feed, I'm doing it that way because then it provides a durability, as we are working through, like, a lot of these users whose feed need to be updated, we— we can resume if something fails, the service goes down, blah blah blah, and that way, like, we will know that it will durably update the feed.
You:  So if.
You:  We ignore the fan out for a second, then the feed generator basically will have, say, a thousands, up to thousands or ten thousands of users whose feed need to be updated, and it will use an workflow maybe batch those ten thousands into a thousand users, and then update the feed for thousand in a single batch, so we can checkpoint and resume if something fails, now when we have a millions of users, then updating those millions of users' feed is going to take a lot of time, and if we want the user uploaded video to be available within five minutes, based on, like, how fast the system is, maybe it's not possible, with a million fan out.
You:  So in that scenario, like, we can have our—
You:  Feed service basically on the fly stitch the feed for popular users—
You:  Not from this table, but maybe have another table which is celebrity feed, table, okay.
You:  So if I have—
You:  Celebrity user.
You:  ID to their post IDs, okay.
You:  And actually we need this for all the users, um.
You:  Because on their— on their profile page we want to show all the posts that you have created, so it is not part of the feed DB, but maybe—
You:  Yeah, it's part of maybe this metadata DB, user ID to post ID, where the users and their feed lips.
You:  Okay, so now the feed service, need to stitch the celebrities, so based on.
You:  If I.
You:  User is following a celebrity or if this user should have the celebrity in their feed based on the algo score, then it will also reach out to the metadata DB, and then compute this list on the fly, and I think this is where, like, our cache service will become quite important, so what the feed service is doing is first building this unified feed using the DB metadata and the feed DB, and then put that into the cache so it runs this calculation once, and then when the client comes scrolling, it is just going off this cache service, so cache becomes a unified feed across celebrities and regular users, that we can use, instead of, like, yeah, stitching it on the fly on each request.
You:  So this will provide us, like, better ability to page through it, and not holding the stuff into the feed service memory each time, when the page request is happening.
You:  So with that, I would say I will like to wrap up, there are, like, error scenarios which we potentially go into, but given we are at almost one h
You: our, I'm going to pause here and, yeah,.
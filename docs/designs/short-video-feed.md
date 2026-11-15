# Short video feed

Upload short videos and serve a personalized, continuously paginated feed.

## Worked design

Upload to object storage, queue transcoding, and publish a manifest only when playable variants exist. A CDN serves segments near viewers.

Fan ordinary authors' new video IDs into follower timelines. Pull popular authors' recent IDs at read time, merge, rank and deduplicate. Carry a stable ranking boundary and ID in the cursor so new uploads do not shift the next page.

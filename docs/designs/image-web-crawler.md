# Image web crawler

Submit seed URLs and retrieve a paginated set of images reachable from each seed.

## Worked design

Create a job with seeds and a depth limit. Normalize URLs, queue work by host, and throttle fetches per host. Cache parsed links and images by URL. Track traversal by seed, URL and remaining depth so a shared page contributes images to every relevant seed.

Example: if two seeds reach one gallery, fetch the gallery once and propagate its image IDs to both results. Track pending work durably before marking a job complete. Paginate seed and image lists independently.

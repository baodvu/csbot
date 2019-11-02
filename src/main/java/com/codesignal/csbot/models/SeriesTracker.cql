CREATE TABLE series_tracker (
   user_id bigint,
   query text,
   episode text,
   source text,
   min_age bigint,
   max_age bigint,
   min_size bigint,
   max_size bigint,
   min_seeds bigint,
   max_seeds bigint,
   min_peers bigint,
   max_peers bigint,
   created_at bigint,
   PRIMARY KEY (user_id, query, episode)
) WITH CLUSTERING ORDER BY (query ASC, episode ASC);

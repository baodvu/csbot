CREATE TABLE discord_message_versioned (
   channel_id bigint,
   message_id bigint,
   created_at timeuuid,

   author_id bigint,
   content text,
   PRIMARY KEY (channel_id, created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at ASC, message_id ASC);

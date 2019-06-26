CREATE TABLE discord_message (
   channel_id bigint,
   message_id bigint,
   created_at timeuuid,

   author_id bigint,
   content text,
   PRIMARY KEY (channel_id, message_id)
) WITH CLUSTERING ORDER BY (message_id ASC);

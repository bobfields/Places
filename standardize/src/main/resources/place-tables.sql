create table place_words (
  word varchar(255) not null default '',
  ids varchar(8192),
  primary key (word)
);
create table places (
  id int,
  name varchar(255) not null default '',
  alt_names varchar(4096) not null default '',
  types varchar(64) not null default '',
  located_in_id int,
  also_located_in_ids varchar(64) not null default '',
  level int,
  country_id int,
  latitude decimal(9,6),
  longitude decimal(9,6),
  sources varchar(1024) not null default '',
  primary key (id)
);

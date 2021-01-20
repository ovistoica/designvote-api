-- Drop tables
drop table if exists vote;
drop table if exists picture;

drop table if exists design_option;
drop table if exists design;

drop table if exists account;


-- Create Schema
CREATE TABLE account
(
 uid     text NOT NULL PRIMARY KEY,
 name    text NOT NULL,
 picture text
);

CREATE TABLE design
(
 design_id   text NOT NULL PRIMARY KEY,
 name        text NOT NULL,
 "public" boolean NOT NULL,
 img         text,
 description text,
 total_votes int CHECK (total_votes >= 0) DEFAULT 0,
 uid text NOT NULL REFERENCES account(uid) ON DELETE CASCADE
);


CREATE TABLE design_option
(
 option_id   text NOT NULL PRIMARY KEY,
 name        text NOT NULL,
 description text,
 design_id   text NOT NULL REFERENCES design(design_id) ON DELETE CASCADE,
 votes int CHECK (votes >= 0) DEFAULT 0
);


CREATE TABLE picture
(
 picture_id text NOT NULL PRIMARY KEY,
 "uri"      text NOT NULL,
 option_id  text NOT NULL REFERENCES design_option (option_id)
);

CREATE TABLE vote
(
 vote_id   serial NOT NULL PRIMARY KEY,
 opinion   text,
 option_id text NOT NULL REFERENCES design_option(option_id)
);
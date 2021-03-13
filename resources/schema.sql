-- Drop tables
drop table if exists vote;
drop table if exists picture;
drop table if exists opinion;

drop table if exists design_version;
drop table if exists design;

drop table if exists account;


-- Create Schema
CREATE TABLE account
(
 uid        text    NOT NULL PRIMARY KEY,
 name       text    NOT NULL,
 email      text    NOT NULL,
 nickname   text,
 social     boolean DEFAULT FALSE,
 picture    text,
 provider   text
);

CREATE TABLE design
(
 design_id   text NOT NULL PRIMARY KEY,
 name        text NOT NULL,
 "public" boolean NOT NULL,
 img         text,
 description text,
 question    text,
 short_url   text,
 design_type text,
 total_votes int CHECK (total_votes >= 0) DEFAULT 0,
 uid text NOT NULL REFERENCES account(uid) ON DELETE CASCADE
);


CREATE TABLE design_version
(
 version_id   text NOT NULL PRIMARY KEY,
 name        text NOT NULL,
 description text,
 design_id   text NOT NULL REFERENCES design(design_id) ON DELETE CASCADE,
 votes int CHECK (votes >= 0) DEFAULT 0
);


CREATE TABLE picture
(
 picture_id text NOT NULL PRIMARY KEY,
 "uri"      text NOT NULL,
 version_id  text NOT NULL REFERENCES design_version (version_id) ON DELETE CASCADE
);

CREATE TABLE vote
(
 vote_id   text NOT NULL PRIMARY KEY,
 version_id text NOT NULL REFERENCES design_version (version_id) ON DELETE CASCADE,
 uid        text REFERENCES account (uid) ON DELETE SET NULL
);

CREATE TABLE opinion
(
 opinion_id  serial NOT NULL PRIMARY KEY,
 opinion   text,
 version_id text NOT NULL REFERENCES design_version (version_id) ON DELETE CASCADE,
 design_id  text NOT NULL REFERENCES design (design_id) ON DELETE CASCADE,
 uid        text REFERENCES account (uid) ON DELETE SET NULL,
 thumbs_up  int CHECK (thumbs_up >= 0) DEFAULT 0
);

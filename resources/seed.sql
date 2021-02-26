-- account
truncate table account cascade;
insert into account ("uid", "name", "email")
values ('auth0|5ef440986e8fbb001355fd9c', 'Auth0', 'auth@designvote.io'),
       ('auth0|600ef45e7038e20071c89994', 'testing@designvote.io', 'testing@designvote.io'),
       ('mike@mailinator.com', 'Mike', 'mike@designvote.io'),
       ('jade@mailinator.com', 'Jade', 'jade@mailinator.com'),
       ('mark@mailinator.com', 'Mark', 'mark@mailinator.com');

-- design
truncate table design cascade;
insert into design (design_id, "public", "name", img, total_votes, "uid")
values ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680', false,  'New sign up', 'https://res.cloudinary.com/stoica94/image/upload/v1611124829/samples/ecommerce/accessories-bag.jpg', 5, 'auth0|5ef440986e8fbb001355fd9c'),
       ('a1995316-80ea-4a98-939d-7c6295e4bb46', true, 'Feed', 'https://res.cloudinary.com/stoica94/image/upload/v1611124828/samples/ecommerce/car-interior-design.jpg', 4, 'jade@mailinator.com');



-- design_version
truncate table design_version cascade;
insert into design_version (version_id, design_id, name, description, votes )
values ('867ed4bf-4628-48f4-944d-e6b7786bfa92', 'a3dde84c-4a33-45aa-b0f3-4bf9ac997680', 'Sign up 1', 'With facebook button', 2  ),
       ('803307da-8dec-4c1b-a0f2-36742ac0e7f2',  'a3dde84c-4a33-45aa-b0f3-4bf9ac997680','Sign up 2',  'Without facebook button', 3),
       ('22a82a84-91cc-40e2-8775-d5bee9d188ff',  'a1995316-80ea-4a98-939d-7c6295e4bb46','Feed 1', 'With cool title', 2),
       ('64f0aed2-157e-481a-a318-8752709e5a5a',  'a1995316-80ea-4a98-939d-7c6295e4bb46','Feed 2', 'Without cool title', 2);

-- picture
truncate table picture cascade;
insert into picture ( picture_id,version_id, "uri")
values ('27b1f44c-2852-416d-960e-3ee7d23ee713', '867ed4bf-4628-48f4-944d-e6b7786bfa92', 'https://res.cloudinary.com/stoica94/image/upload/v1611124824/samples/ecommerce/shoes.png'),
       ('c89e6054-5e4f-48f2-b6d4-f037460ef72e', '803307da-8dec-4c1b-a0f2-36742ac0e7f2','https://res.cloudinary.com/stoica94/image/upload/v1611124820/samples/ecommerce/analog-classic.jpg' ),
       ('aaa7ab14-efd7-45a1-ac86-aa6bfe13a2ab', '22a82a84-91cc-40e2-8775-d5bee9d188ff', 'https://res.cloudinary.com/stoica94/image/upload/v1611124829/samples/ecommerce/leather-bag-gray.jpg'),
       ('05cbe0ef-fd8a-47a0-8602-2c154a06edba', '64f0aed2-157e-481a-a318-8752709e5a5a', 'https://res.cloudinary.com/stoica94/image/upload/v1611124826/samples/people/jazz.jpg');


-- vote
truncate table vote cascade;
insert into vote (version_id, vote_id)
values ('867ed4bf-4628-48f4-944d-e6b7786bfa92', '867ed4bf-4628-48f4-944d-e6b7786bfa93'),
       ('867ed4bf-4628-48f4-944d-e6b7786bfa92', '867ed4bf-4628-48f4-944d-e6b7786bfa94'),
       ('803307da-8dec-4c1b-a0f2-36742ac0e7f2', '867ed4bf-4628-48f4-944d-e6b7786bfa95'),
       ('803307da-8dec-4c1b-a0f2-36742ac0e7f2', '867ed4bf-4628-48f4-944d-e6b7786bfa97'),
       ('803307da-8dec-4c1b-a0f2-36742ac0e7f2', '867ed4bf-4628-48f4-944d-e6b7786bfa23'),
       ('22a82a84-91cc-40e2-8775-d5bee9d188ff', '867ed4bf-4628-48f4-944d-e6b7786bfa63'),
       ('22a82a84-91cc-40e2-8775-d5bee9d188ff', '867ed4bf-4628-48f4-944d-e6b7786bfa21'),
       ('64f0aed2-157e-481a-a318-8752709e5a5a', '867ed4bf-4628-48f4-944d-e6b7786bfa19'),
       ('64f0aed2-157e-481a-a318-8752709e5a5a', '867ed4bf-4628-48f4-944d-e6b7786bfa70');

-- opinion
truncate table opinion  cascade;
insert into opinion (design_id, version_id, opinion)
values ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680','867ed4bf-4628-48f4-944d-e6b7786bfa92','I like this one better'),
       ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680','867ed4bf-4628-48f4-944d-e6b7786bfa92','I like this one'),
       ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680','803307da-8dec-4c1b-a0f2-36742ac0e7f2','The other one is ugly'),
       ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680','803307da-8dec-4c1b-a0f2-36742ac0e7f2','Meh, good enough'),
       ('a3dde84c-4a33-45aa-b0f3-4bf9ac997680','803307da-8dec-4c1b-a0f2-36742ac0e7f2','Ok'),
       ('a1995316-80ea-4a98-939d-7c6295e4bb46','22a82a84-91cc-40e2-8775-d5bee9d188ff','I like it'),
       ('a1995316-80ea-4a98-939d-7c6295e4bb46','22a82a84-91cc-40e2-8775-d5bee9d188ff','Great work on this one'),
       ('a1995316-80ea-4a98-939d-7c6295e4bb46','64f0aed2-157e-481a-a318-8752709e5a5a','This is perfect'),
       ('a1995316-80ea-4a98-939d-7c6295e4bb46','64f0aed2-157e-481a-a318-8752709e5a5a','Good for you');

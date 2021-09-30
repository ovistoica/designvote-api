# Designvote API

The API for the Designvote Website

## Tech Stack

**Client:** React, Typescript, Chakra-UI, Zustand, Auth0

**Server (this repo):** Clojure, Reitit, PostgreSQL, Auth0, DigitalOcean Spaces, Stripe

## Environment Variables

To run this project, you will need to add the following environment variables to your `resources/secrets.edn` file

```edn
# resources/secrets.edn 
  
{:stripe-secret      "example"
 :signing-secret     "example"
 :yearly-plan        "example"
 :monthly-plan       "example"
 :aws-access-key     "example"
 :aws-secret-key     "example"
 :aws-s3-bucket-name "example"
 :aws-s3-endpoint    "example"}
 ```

## Run Locally

Clone the project

```bash
  git clone git@github.com:ovistoica/designvote-api.git
```

Go to the project directory

```bash
  cd designvote-api
```

Start a repl

```bash
  lein repl
```

Start the server

```bash
  user=> (start)
```

## Deployment

The project is deployed on heroku so when you push to the master branch, it will deploy automatically

## API Reference

#### Get all items

```http
  GET /api/items
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `api_key` | `string` | **Required**. Your API key |

#### Upload multiple versions for a design

```http
  POST /v1/design/${design-id}/versions/multiple
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `design-id`      | `string` | **Required**. Id of design |

#### Vote on a design survey

```http
  POST /v1/design/vote/${design-id}/
```
| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `design-id`      | `string` | **Required**. Id of design |

Body: 
```edn
{
```

## Screens

#### Home Screen

```http request
/
```

#### Latest Screen

```http request
/latest
```
The screen with the latest public designs

#### Popular Screen

```http request
/popular
```
The screen with the most popular designs

#### Public Design Screen

```http request
/design/{designId}
```
| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `designId`      | `string` | **Required**. Id of design |
The screen where users can vote on designs. If a user doesn't vote, he will not see results

#### Designer screen
The designer screen

```http request
/${nickname}
```
| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `nickname`      | `string` | **Required**. Nickname of the designers |

#### Designer surveys
The designer screen

```http request
/${nickname}/surveys
```
| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `nickname`      | `string` | **Required**. Nickname of the designers |

#### Profile

```http request
/account/profile
```
The edit account settings

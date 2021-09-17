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

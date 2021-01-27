terraform {
  backend "local" {
    path                  = "terraform/terraform.tfstate"
  }
}

# Variables
variable "myRegion" { type             = string }
variable "accountId" { type            = string }
variable "s3Bucket" { type             = string }
variable "s3Key" { type                = string }
variable "dynamoDBTable" { type        = string }
variable "lambda_env_variables" {
  type                                 = map(string)
  default                              = {}
}

locals {
  aggregated_request_template          = <<REQUEST_TEMPLATE
      #set($inputRoot = $input.path('$'))
      #set($allParams = $input.params())
      $util.qr($inputRoot.put("httpMethod", $context.httpMethod))
      $util.qr($inputRoot.put("headers", $allParams.get('header')))
      $util.qr($inputRoot.put("parameters", $allParams.get('path')))
      $util.qr($inputRoot.put("queryparameters", $allParams.get('querystring')))
      $input.json('$')
    REQUEST_TEMPLATE
}

provider "aws" {
  shared_credentials_file = "~/.aws/credentials"
  profile                 = "default"
  region                  = var.myRegion
}

# API Gateway
resource "aws_api_gateway_rest_api" "gateway_rest_api" {
  name                    = "HelloTerraform"
}

resource "aws_api_gateway_deployment" "gateway_stage" {
  depends_on              = [module.gateway_integration_GET, module.gateway_integration_POST]
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  stage_name              = "dev"
}

resource "aws_api_gateway_resource" "gateway_resource" {
  path_part               = "resource"
  parent_id               = aws_api_gateway_rest_api.gateway_rest_api.root_resource_id
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
}

# API Gateway - GET
module "gateway_integration_GET" {
  source                  = "./terraformModules"
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = "GET"
  lambda_function_invoke_arn       = aws_lambda_function.lambda_function.invoke_arn
  api_gateway_integration_template = local.aggregated_request_template
}

# API Gateway - POST
module "gateway_integration_POST" {
  source                   = "./terraformModules"
  rest_api_id              = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id              = aws_api_gateway_resource.gateway_resource.id
  http_method              = "POST"
  lambda_function_invoke_arn       = aws_lambda_function.lambda_function.invoke_arn
  api_gateway_integration_template = local.aggregated_request_template
}

# Lambda
resource "aws_lambda_permission" "lambda_permission" {
  statement_id            = "AllowExecutionFromAPIGateway"
  action                  = "lambda:InvokeFunction"
  function_name           = aws_lambda_function.lambda_function.function_name
  principal               = "apigateway.amazonaws.com"
  source_arn              = "arn:aws:execute-api:${var.myRegion}:${var.accountId}:${aws_api_gateway_rest_api.gateway_rest_api.id}/*/*"
}

resource "aws_lambda_function" "lambda_function" {
  filename                = "target/examples-0.1.0-SNAPSHOT.jar"
  function_name           = "HelloTerraform"
  role                    = aws_iam_role.iam_role_for_lambda.arn
  handler                 = "org.ost.investigate.aws.lambda.examples.hello.LambdaMethodHandler::handleRequest"
  source_code_hash        = filebase64sha256("target/examples-0.1.0-SNAPSHOT.jar")
  runtime                 = "java11"
  memory_size             = 512
  timeout                 = 121
  environment {
    variables             = "${merge(var.lambda_env_variables,
        map("S3BUCKET", var.s3Bucket),
        map("S3KEY", var.s3Key),
        map("DYNAMO_DB_TABLE", var.dynamoDBTable),
        map("REGION", var.myRegion))
        }"
  }
}

# IAM Lambda
resource "aws_iam_role" "iam_role_for_lambda" {
  name                    = "hello-terra-role"
  assume_role_policy      = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
        {
          "Action": "sts:AssumeRole",
          "Principal": {
            "Service": "lambda.amazonaws.com"
          },
          "Effect": "Allow",
          "Sid": ""
        }
  ]
  }
  EOF
}

resource "aws_iam_role_policy_attachment" "s3_read_only_policy_attachment" {
  role                    = aws_iam_role.iam_role_for_lambda.name
  policy_arn              = "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"
}

resource "aws_iam_role_policy_attachment" "dynamo_db_policy_attachment" {
  role                    = aws_iam_role.iam_role_for_lambda.name
  policy_arn              = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}

resource "aws_iam_role_policy_attachment" "cloudwatch_policy_attachment" {
  role                    = aws_iam_role.iam_role_for_lambda.name
  policy_arn              = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
}

# S3
resource "aws_s3_bucket" "s3_bucket" {
  bucket                  = var.s3Bucket
  acl                     = "private"
}

resource "aws_s3_bucket_object" "s3_bucket_object" {
  bucket                  = var.s3Bucket
  key                     = var.s3Key
  source                  = "s3/HelloTemplateS3.json"
  etag                    = filemd5("s3/HelloTemplateS3.json")
}

# Dynamodb
resource "aws_dynamodb_table" "dynamodb_table" {
  name                    = var.dynamoDBTable
  hash_key                = "time"
  billing_mode            = "PROVISIONED"
  read_capacity           = 5
  write_capacity          = 5
  attribute {
    name                  = "time"
    type                  = "S"
  }
}
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
variable "lambda_env_variables" {
  type                                 = map(string)
  default                              = {}
}

locals {
  aggregated_request_template          = <<REQUEST_TEMPLATE
      #set($inputRoot = $input.path('$'))
      #set($allParams = $input.params())
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
  depends_on              = [aws_api_gateway_integration.gateway_integration]
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  stage_name              = "dev"
}

resource "aws_api_gateway_resource" "gateway_resource" {
  path_part               = "resource"
  parent_id               = aws_api_gateway_rest_api.gateway_rest_api.root_resource_id
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
}

# API Gateway - Get
resource "aws_api_gateway_method" "gateway_method" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = "GET"
  authorization           = "NONE"
}

resource "aws_api_gateway_integration" "gateway_integration" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  integration_http_method = "POST"
  type                    = "AWS"
  uri                     = aws_lambda_function.lambda_function.invoke_arn
  request_templates       = {
    "application/json"    = local.aggregated_request_template
  }
  passthrough_behavior    = "WHEN_NO_TEMPLATES"
}

resource "aws_api_gateway_method_response" "gateway_response_200" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  status_code = "200"
}

resource "aws_api_gateway_integration_response" "MyDemoIntegrationResponse" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  status_code             = aws_api_gateway_method_response.gateway_response_200.status_code
}

# API Gateway - POST
resource "aws_api_gateway_method" "gateway_method_POST" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = "POST"
  authorization           = "NONE"
}

resource "aws_api_gateway_integration" "gateway_integration_POST" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method_POST.http_method
  integration_http_method = "POST"
  type                    = "AWS"
  uri                     = aws_lambda_function.lambda_function.invoke_arn
  request_templates       = {
    "application/json"    = local.aggregated_request_template
  }
  passthrough_behavior    = "WHEN_NO_TEMPLATES"
}

resource "aws_api_gateway_method_response" "gateway_response_200_POST" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method_POST.http_method
  status_code = "200"
}

resource "aws_api_gateway_integration_response" "MyDemoIntegrationResponse_POST" {
  rest_api_id             = aws_api_gateway_rest_api.gateway_rest_api.id
  resource_id             = aws_api_gateway_resource.gateway_resource.id
  http_method             = aws_api_gateway_method.gateway_method_POST.http_method
  status_code             = aws_api_gateway_method_response.gateway_response_200_POST.status_code
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
  runtime                 = "java8"
  memory_size             = 256
  timeout                 = 60
  environment {
    variables             = "${merge(var.lambda_env_variables, map("S3BUCKET", var.s3Bucket), map("S3KEY", var.s3Key))}"
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
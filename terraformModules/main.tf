resource "aws_api_gateway_method" "gateway_method" {
  rest_api_id             = var.rest_api_id
  resource_id             = var.resource_id
  http_method             = var.http_method
  authorization           = "NONE"
}

resource "aws_api_gateway_integration" "gateway_integration" {
  rest_api_id             = var.rest_api_id
  resource_id             = var.resource_id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  integration_http_method = "POST"
  type                    = "AWS"
  uri                     = var.lambda_function_invoke_arn
  request_templates       = {
    "application/json"    = var.api_gateway_integration_template
  }
  passthrough_behavior    = "WHEN_NO_TEMPLATES"
}

resource "aws_api_gateway_method_response" "gateway_response_200" {
  rest_api_id             = var.rest_api_id
  resource_id             = var.resource_id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  status_code = "200"
}

resource "aws_api_gateway_integration_response" "MyDemoIntegrationResponse" {
  rest_api_id             = var.rest_api_id
  resource_id             = var.resource_id
  http_method             = aws_api_gateway_method.gateway_method.http_method
  status_code             = aws_api_gateway_method_response.gateway_response_200.status_code
}
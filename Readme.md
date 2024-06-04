This Lambda function POC takes an object stored in the S3 and creates a kinesis stream record with partitionKey equal to the file path and name in S3 bucket
Example of a kinesis record produced by the lambda function:
"Records": [
{
"SequenceNumber": "49652684163915532962702158220311267563231430989366427650",
"ApproximateArrivalTimestamp": "2024-06-04T16:12:46.056000+03:00",
"Data": "InRlc3QxL3Rlc3QyL3Rlc3RmaWxlLnR4dCI=",
"PartitionKey": "test1/test2/testfile.txt",
"EncryptionType": "NONE"
}
],

Steps to setup the localstack and run the entire flow:

1) Install LocalStack:
    First, ensure you have LocalStack installed. You can install LocalStack using pip (Python's package installer):
    pip install localstack
2) Start LocalStack:
   If you installed LocalStack using pip, start it with the following command:
   localstack start
3) Configure AWS CLI to use LocalStack:
   Configure your AWS CLI to point to LocalStack's endpoints:
   aws configure set aws_access_key_id test
   aws configure set aws_secret_access_key test
   aws configure set default.region us-east-1
4) Set the endpoint URL for S3:
   export AWS_ENDPOINT_URL=http://localhost:4566
5) Set IAM user permissions
   aws --endpoint-url=http://localhost:4566 iam create-role --role-name execution_role --assume-role-policy-document file://src/main/resources/trust-policy.json
   aws --endpoint-url=http://localhost:4566 iam create-policy --policy-name KinesisPutPolicy --policy-document file://src/main/resources/kinesis_put_policy.json
   aws --endpoint-url=http://localhost:4566 iam attach-role-policy --role-name execution_role --policy-arn arn:aws:iam::000000000000:policy/KinesisPutPolicy
6) Create an S3 Bucket:
   aws --endpoint-url=$AWS_ENDPOINT_URL s3 mb s3://my-test-bucket/test1/test2
7) Create Kinesis Stream
   aws --endpoint-url=$AWS_ENDPOINT_URL kinesis create-stream --stream-name my-kinesis-stream --shard-count 1
8) Create a jar for the lambda function
   mvn clean package
9) Deploy lambda function
   aws --endpoint-url=http://localhost:4566 lambda create-function --function-name s3-to-kinesis-lambda --runtime java11 --role arn:aws:iam::000000000000:role/execution_role --handler com.example.S3ToKinesisHandler::handleRequest --zip-file fileb://target/lambda-kinesis-poc-1.0-SNAPSHOT.jar --memory-size 1024 --environment Variables="{JAVA_TOOL_OPTIONS='-XX:MaxMetaspaceSize=128m'}"
10) Configure Lambda function to run on S3 object creation(wait a few minutes for the lambda function to properly be deployed)
    aws --endpoint-url=$AWS_ENDPOINT_URL s3api put-bucket-notification-configuration --bucket my-test-bucket --notification-configuration file://src/main/resources/notification.json
11) Add a file to s3 bucket
    aws --endpoint-url=$AWS_ENDPOINT_URL s3 cp src/main/resources/testfile.txt s3://my-test-bucket/test1/test2/testfile.txt --debug
12) Check the kinesis stream. Get kinesis shard id
    aws --endpoint-url=$AWS_ENDPOINT_URL kinesis get-shard-iterator --stream-name my-kinesis-stream --shard-id shardId-000000000000 --shard-iterator-type TRIM_HORIZON
13) Get kinesis records
    aws --endpoint-url=$AWS_ENDPOINT_URL kinesis get-records --shard-iterator {kinesis-shard-id}

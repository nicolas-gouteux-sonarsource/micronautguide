package s3example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

public class BucketExample15 {

  public static void main(String[] args) throws IOException {
    String bucketName = "ngx-bucket-dev";
    String key = "myKey";
    String anotehrUnusedString;
    String anotehrUnusedString14;
    String anotehrUnusedString16;

    try {

//      System.out.println("AWS_PROFILE="+System.getenv("AWS_PROFILE"));
//      System.out.println("AWS_REGION="+System.getenv("AWS_REGION"));

      //var client = new Amazon.SecurityToken.AmazonSecurityTokenServiceClient(REGION);
      // Get and display the information about the identity of the default user.
      //var callerIdRequest = new GetCallerIdentityRequest();

//      AWSSecurityTokenService tokenService = AWSSecurityTokenServiceClientBuilder.standard().build();
//
//      GetSessionTokenRequest session_token_request = new GetSessionTokenRequest();
//
//      GetSessionTokenResult session_token_result =
//        tokenService.getSessionToken(session_token_request);
//
//      Credentials session_creds = session_token_result.getCredentials();
//
//      BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
//        session_creds.getAccessKeyId(),
//        session_creds.getSecretAccessKey(),
//        session_creds.getSessionToken());
//
//      AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
//        .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
//        .build();


      S3Client s3Client =  S3Client.builder()
        //.httpClientBuilder(ApacheHttpClient.builder())
        //.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
//        .credentialsProvider(DefaultCredentialsProvider.create())
        .credentialsProvider(ProfileCredentialsProvider.create("sonarsource-dev-admin"))
        .region(Region.EU_WEST_1)
        .build();

      HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
        .bucket(bucketName)
        .build();
      try {
        s3Client.headBucket(headBucketRequest);
      } catch (NoSuchBucketException e) {
        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
          .bucket(bucketName)
          .build();

        s3Client.createBucket(bucketRequest);
        HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
          .bucket(bucketName)
          .build();
        System.out.println(bucketName +" is created");


        // Wait until the bucket is created and print out the response.
        S3Waiter s3Waiter = s3Client.waiter();
        WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
        waiterResponse.matched().response().ifPresent(System.out::println);
        System.out.println(bucketName +" is ready");
      }

      // put
      PutObjectRequest objectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .build();

      s3Client.putObject(objectRequest, RequestBody.fromByteBuffer(
        getRandomString(10_000)));
      System.out.println("Wrote " + key);


//      List<Bucket> blist = s3Client.listBuckets();
//      objectListing = s3Client.listObjects(bucketName);
//      System.out.println(objectListing);


      GetObjectRequest gor = GetObjectRequest.builder().bucket(bucketName).key(key)
        .build();
      ResponseInputStream<GetObjectResponse> r =  s3Client.getObject(gor);

      BufferedReader reader = new BufferedReader(new InputStreamReader(r));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }

//
//
//      if (s3Client.doesBucketExistV2(bucketName)) {
//        // Verify that the bucket was created by retrieving it and checking its location.
//        String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
//        System.out.println("Bucket location: " + bucketLocation);
//      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
    byte[] b = new byte[size];
    new Random().nextBytes(b);
    return ByteBuffer.wrap(b);
  }

  private static ByteBuffer getRandomString(int size) throws IOException {
    StringBuilder b = new StringBuilder(size);
    Random r = new Random();
    for (int i = 0; i < size; i++) {
      char c = (char) ('A' + r.nextInt(26));
    b.append(c);
    }
    return ByteBuffer.wrap(b.toString().getBytes(StandardCharsets.UTF_8));
  }


}
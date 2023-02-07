/*
 * Copyright (C) 2009-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package s3example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

// scratch version, TODO make it better
public class S3Wrapper {

  public static void printObject(ResponseInputStream<GetObjectResponse> r) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(r));
    String line;
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
    }
  }

  public static ResponseInputStream<GetObjectResponse> getObject(S3Client s3Client, String bucketName, String key) {
    GetObjectRequest gor = GetObjectRequest.builder().bucket(bucketName).key(key).build();
    return s3Client.getObject(gor);
  }

  public static byte[] getObjectAsBytes(S3Client s3Client, String bucketName, String key) {
    GetObjectRequest gor = GetObjectRequest.builder().bucket(bucketName).key(key).build();
    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(gor);
    return objectBytes.asByteArray();
  }

  public static String getObjectAsString(S3Client s3Client, String bucketName, String key) {
    byte[] data = getObjectAsBytes(s3Client, bucketName, key);
    return new String(data, StandardCharsets.UTF_8);
  }

  public static void putRandomString(S3Client s3Client, String bucketName, String key) throws IOException {
    PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build();
    s3Client.putObject(objectRequest, RequestBody.fromByteBuffer(getRandomString(10_000)));
    System.out.println("Wrote " + key);
  }

  public static void putString(S3Client s3Client, String bucketName, String key, String value) throws IOException {
    PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build();
    s3Client.putObject(objectRequest, RequestBody.fromBytes(value.getBytes(StandardCharsets.UTF_8)));
    System.out.println("Wrote " + key);
  }

  public static void putBytes(S3Client s3Client, String bucketName, String key, byte[] byteArray) throws IOException {
    PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build();
    s3Client.putObject(objectRequest, RequestBody.fromBytes(byteArray));
  }

  public static void createBucket(S3Client s3Client, String bucketName) {
    CreateBucketRequest bucketRequest = CreateBucketRequest.builder().bucket(bucketName).build();
    s3Client.createBucket(bucketRequest);
    System.out.println(bucketName + " is created");
    HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();
    S3Waiter s3Waiter = s3Client.waiter();
    WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
    waiterResponse.matched().response().ifPresent(System.out::println);
    System.out.println(bucketName + " is ready");
  }

  public static S3Client getS3Client() {
    return S3Client.builder()
      .credentialsProvider(ProfileCredentialsProvider.create("sonarsource-sandbox-admin"))
      .region(Region.EU_CENTRAL_1)
      .build();
  }

  public static boolean bucketExists(S3Client s3Client, String bucketName) {
    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
      .bucket(bucketName)
      .build();
    try {
      s3Client.headBucket(headBucketRequest);
      return true;
    } catch (NoSuchBucketException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private static ByteBuffer getRandomByteBuffer(int size) {
    byte[] b = new byte[size];
    new Random().nextBytes(b);
    return ByteBuffer.wrap(b);
  }

  private static ByteBuffer getRandomString(int size) {
    StringBuilder b = new StringBuilder(size);
    Random r = new Random();
    for (int i = 0; i < size; i++) {
      char c = (char) ('A' + r.nextInt(26));
      b.append(c);
    }
    return ByteBuffer.wrap(b.toString().getBytes(StandardCharsets.UTF_8));
  }


  /*** list ***/
  public static List<String> listBucketObjects(S3Client s3, String bucketName) {
    try {
      List<String> keys = new ArrayList<>();
      ListObjectsRequest listObjects = ListObjectsRequest
        .builder()
        .bucket(bucketName)
        .build();

      ListObjectsResponse res = s3.listObjects(listObjects);
      List<S3Object> objects = res.contents();
      for (S3Object obj : objects) {
        keys.add(obj.key());
      }
      return keys;
    } catch (S3Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      return null;
    }
  }

  /*** destruction ***/
  public static void deleteBucket(S3Client s3, String bucket) {
    try {
      // To delete a bucket, all the objects in the bucket must be deleted first.
      ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
        .bucket(bucket)
        .build();
      ListObjectsV2Response listObjectsV2Response;
      do {
        listObjectsV2Response = s3.listObjectsV2(listObjectsV2Request);
        for (S3Object s3Object : listObjectsV2Response.contents()) {
          DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(s3Object.key())
            .build();
          s3.deleteObject(request);
          System.out.println("Deleted " + bucket + "." + s3Object.key());
        }
      } while (listObjectsV2Response.isTruncated());

      DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
      s3.deleteBucket(deleteBucketRequest);
      System.out.println("Deleted " + bucket);

    } catch (S3Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
    }
  }
}
